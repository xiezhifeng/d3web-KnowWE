/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package de.d3web.we.flow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang.StringEscapeUtils;

import de.d3web.KnOfficeParser.D3webConditionBuilder;
import de.d3web.KnOfficeParser.DefaultLexer;
import de.d3web.KnOfficeParser.RestrictedIDObjectManager;
import de.d3web.KnOfficeParser.complexcondition.ComplexConditionSOLO;
import de.d3web.core.inference.condition.Condition;
import de.d3web.core.manage.KnowledgeBaseManagement;
import de.d3web.diaFlux.ConditionTrue;
import de.d3web.diaFlux.flow.Flow;
import de.d3web.diaFlux.flow.FlowFactory;
import de.d3web.diaFlux.flow.IEdge;
import de.d3web.diaFlux.flow.INode;
import de.d3web.diaFlux.inference.DiaFluxUtils;
import de.d3web.report.Message;
import de.d3web.we.flow.persistence.CommentNodeHandler;
import de.d3web.we.flow.persistence.ComposedNodeHandler;
import de.d3web.we.flow.persistence.DiagnosisNodeHandler;
import de.d3web.we.flow.persistence.ExitNodeHandler;
import de.d3web.we.flow.persistence.NodeHandler;
import de.d3web.we.flow.persistence.QuestionNodeHandler;
import de.d3web.we.flow.persistence.SnapshotNodeHandler;
import de.d3web.we.flow.persistence.StartNodeHandler;
import de.d3web.we.flow.type.EdgeType;
import de.d3web.we.flow.type.GuardType;
import de.d3web.we.flow.type.NodeType;
import de.d3web.we.flow.type.OriginType;
import de.d3web.we.flow.type.TargetType;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.report.KDOMReportMessage;
import de.d3web.we.kdom.report.KDOMWarning;
import de.d3web.we.kdom.xml.AbstractXMLObjectType;
import de.d3web.we.reviseHandler.D3webSubtreeHandler;

/**
 *
 *
 * @author Reinhard Hatko
 * @created on: 12.10.2009
 */
public class FlowchartSubTreeHandler extends D3webSubtreeHandler {

	private final static List<NodeHandler> HANDLERS;

	static {
		HANDLERS = new ArrayList<NodeHandler>();

		HANDLERS.add(new StartNodeHandler());
		HANDLERS.add(new ExitNodeHandler());
		HANDLERS.add(new DiagnosisNodeHandler());
		HANDLERS.add(new CommentNodeHandler());
		HANDLERS.add(new ComposedNodeHandler());
		HANDLERS.add(new QuestionNodeHandler());
		HANDLERS.add(new SnapshotNodeHandler());
	}

	@Override
	public Collection<KDOMReportMessage> create(KnowWEArticle article, Section s) {

		KnowledgeBaseManagement kbm = getKBM(article);
		Section flowcontent = ((AbstractXMLObjectType) s.getObjectType()).getContentChild(s);

		if (kbm == null || flowcontent == null) {
			return null;
		}

		Map<String, String> attributeMap = AbstractXMLObjectType.getAttributeMapFor(s);
		String name = attributeMap.get("name");
		String id = attributeMap.get("fcid");

		if (name == null || name.equals("")) name = "unnamed";

		List<Message> errors = new ArrayList<Message>();

		List<INode> nodes = createNodes(article, name, s, errors);

		List<IEdge> edges = createEdges(article, s, nodes, errors);

		Flow flow = FlowFactory.getInstance().createFlow(id, name, nodes, edges);

		DiaFluxUtils.addFlow(flow, kbm.getKnowledgeBase());

		List<KDOMReportMessage> msgs = new ArrayList<KDOMReportMessage>();

		for (final Message message : errors) {
			msgs.add(new KDOMWarning() {

				@Override
				public String getVerbalization() {
					return message.getMessageText();
				}
			});
		}

		return msgs;
	}

	private List<IEdge> createEdges(KnowWEArticle article, Section flowSection, List<INode> nodes, List<Message> errors) {
		List<IEdge> result = new ArrayList<IEdge>();

		List<Section> edgeSections = new ArrayList<Section>();
		Section flowcontent = ((AbstractXMLObjectType) flowSection.getObjectType()).getContentChild(flowSection);
		flowcontent.findSuccessorsOfType(EdgeType.class, edgeSections);

		for (Section section : edgeSections) {

			String id = AbstractXMLObjectType.getAttributeMapFor(section).get("fcid");
			Section content = (Section) section.getChildren().get(1);

			String sourceID = getXMLContentText(content.findChildOfType(OriginType.class));

			INode source = getNodeByID(sourceID, nodes);

			if (source == null) {
				String messageText = "No source node found with id " + sourceID + " in edge " + id
						+ ".";

				errors.add(new Message(messageText));
				continue;
			}

			String targetID = getXMLContentText(content.findChildOfType(TargetType.class));

			INode target = getNodeByID(targetID, nodes);

			if (target == null) {
				String messageText = "No target node found with id " + targetID + " in edge " + id
						+ ".";
				errors.add(new Message(messageText));
				continue;
			}

			Condition condition;

			Section guardSection = content.findChildOfType(GuardType.class);

			if (guardSection != null) {
				condition = buildCondition(article, guardSection, errors);

				if (condition == null) {
					condition = ConditionTrue.INSTANCE;

					errors.add(new Message("Could not parse condition: "
							+ getXMLContentText(guardSection)));

				}

			}
			else {
				condition = ConditionTrue.INSTANCE;
			}

			result.add(FlowFactory.getInstance().createEdge(id, source, target, condition));

		}

		return result;
	}

	private Condition buildCondition(KnowWEArticle article, Section s, List<Message> errors) {

		String originalText = getXMLContentText(s);

		InputStream stream = new ByteArrayInputStream(originalText.getBytes());
		ANTLRInputStream input = null;
		try {
			input = new ANTLRInputStream(stream);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		DefaultLexer lexer = new DefaultLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ComplexConditionSOLO parser = new ComplexConditionSOLO(tokens);

		RestrictedIDObjectManager idom = new RestrictedIDObjectManager(getKBM(article));

		// For creating conditions in edges coming out from call nodes
		// TODO explicitly create oc-question for all FCs
		// idom.setLazyAnswers(true);
		// idom.setLazyQuestions(true);

		D3webConditionBuilder builder = new D3webConditionBuilder("Parsed from article", errors,
				idom);

		parser.setBuilder(builder);
		try {
			parser.complexcondition();
		}
		catch (RecognitionException e) {
			e.printStackTrace();
		}
		Condition condition = builder.pop();

		return condition;
	}

	public static String getXMLContentText(Section s) {
		String originalText = ((Section) s.getChildren().get(1)).getOriginalText();
		return StringEscapeUtils.unescapeXml(originalText);
	}

	private INode getNodeByID(String nodeID, List<INode> nodes) {
		for (INode node : nodes) {
			if (node.getID().equals(nodeID)) return node;
		}

		return null;
	}

	private List<INode> createNodes(KnowWEArticle article, String flowName, Section flowSection, List<Message> errors) {

		List<INode> result = new ArrayList<INode>();
		List<Section> nodeSections = new ArrayList<Section>();
		Section flowcontent = ((AbstractXMLObjectType) flowSection.getObjectType()).getContentChild(flowSection);
		flowcontent.findSuccessorsOfType(NodeType.class, nodeSections);

		KnowledgeBaseManagement kbm = getKBM(article);

		for (Section nodeSection : nodeSections) {

			String id = AbstractXMLObjectType.getAttributeMapFor(nodeSection).get("fcid");

			NodeHandler handler = findNodeHandler(article, kbm, nodeSection);

			if (handler == null) {
				errors.add(new Message("No NodeHandler found for: " + nodeSection.getOriginalText()));
				continue;
			}
			else {// handler can in general handle NodeType
				INode node = handler.createNode(article, kbm, nodeSection, flowSection, id, errors);

				if (node != null) {
					result.add(node);
				}
				else { // handler could not generate a node for the supplied
						// section
					Section<KnowWEObjectType> nodeInfo = nodeSection.findSuccessor(handler.getObjectType().getClass());
					String text = getXMLContentText(nodeInfo);
					errors.add(new Message("NodeHandler " + handler.getClass().getSimpleName()
							+ " could not create node for: " + text));
					result.add(FlowFactory.getInstance().createCommentNode(id,
							"Surrogate for node of type "));
				}

			}

		}

		return result;
	}

	private NodeHandler findNodeHandler(KnowWEArticle article,
			KnowledgeBaseManagement kbm, Section nodeSection) {

		for (NodeHandler handler : HANDLERS) {
			if (handler.canCreateNode(article, kbm, nodeSection)) return handler;
		}

		return null;
	}

	//
	// private List<INode> createNodes(KnowWEArticle article, String flowName,
	// List<Section> nodeSections, List<Message> errors) {
	//
	// List<INode> result = new ArrayList<INode>();
	//
	// for (Section section : nodeSections) {
	//
	// Section nodeContent = (Section) section.getChildren().get(1);
	// // Section
	// // of
	// // NodeContentType
	//
	// // get the important info
	// List<Section> children = nodeContent.getChildren(new SectionFilter() {
	//
	// public boolean accept(Section section) {
	// return section.getObjectType() != PositionType.getInstance()
	// && section.getObjectType() != PlainText.getInstance();
	// }
	// });
	//
	// // Assert.assertEquals(section +
	// // " has the wrong number of children.",1, children.size());
	//
	// if (children.size() != 1) continue;
	//
	// Section nodeinfo = children.get(0);
	//
	// String id =
	// AbstractXMLObjectType.getAttributeMapFor(section).get("fcid");
	//
	// if (nodeinfo.getObjectType() == StartType.getInstance()) {
	// result.add(createStartNode(id, nodeinfo, errors));
	// } else if (nodeinfo.getObjectType() == ExitType.getInstance()) {
	// result.add(createEndNode(
	// article, id, flowName, nodeinfo, errors));
	// } else if (nodeinfo.getObjectType() == ActionType.getInstance()) {
	// result.add(createActionNode(
	// article, id, nodeinfo, errors));
	// } else if (nodeinfo.getObjectType() == CommentType.getInstance()) {
	// result.add(createCommentNode(
	// article, id, nodeinfo, errors));
	// } else if (nodeinfo.getObjectType() == SnapshotType.getInstance()) {
	// result.add(createSnapshotNode(
	// article, id, nodeinfo, errors));
	// }
	// else {
	// errors.add(new Message("Unknown node type: " +
	// nodeinfo.getObjectType()));
	// }
	//
	// }
	//
	// return result;
	// }

	// private INode createSnapshotNode(KnowWEArticle article, String id,
	// Section nodeinfo, List<Message> errors) {
	//
	// // SnapshotAction action = new SnapshotAction();
	//
	// // return FlowFactory.getInstance().createNode(id, action);
	// return new SnapshotNode(id);
	// }

	// private INode createCommentNode(KnowWEArticle article, String id,
	// Section nodeinfo, List<Message> errors) {
	//
	// PSAction action = NoopAction.INSTANCE;
	//
	// INode node = FlowFactory.getInstance().createActionNode(id, action);
	// String string = getXMLContentText(nodeinfo);
	//
	// node.setName("Comment: " + string);
	//
	//
	// return node;
	// }

	// private INode createActionNode(KnowWEArticle article, String id, Section
	// section, List<Message> errors) {
	//
	// PSAction action = NoopAction.INSTANCE;
	// String string = getXMLContentText(section);
	// if (string.startsWith("ERFRAGE") || string.startsWith("INSTANT")) {
	// action = createQAindicationAction(article, section, string, errors);
	// }
	// else if (string.contains("=")) {
	// action = createHeuristicScoreAction(article, section, string, errors);
	// }
	// else if (string.startsWith("CALL[")) {
	// action = createCallFlowAction(article, section, string, errors);
	// }
	// else action = NoopAction.INSTANCE;
	//
	// INode node = FlowFactory.getInstance().createActionNode(id, action);
	// node.setName(string);
	// return node;
	//
	// }

	// private PSAction createCallFlowAction(KnowWEArticle article, Section
	// section, String string, List<Message> errors) {
	//
	// int nodenameStart = string.indexOf('(');
	// int nodenameEnd = string.indexOf(')');
	//
	// String flowName = string.substring(5, nodenameStart);
	// String nodeName = string.substring(nodenameStart + 1, nodenameEnd);
	//
	// return new ComposedNodeAction(flowName, nodeName);
	//
	// //old version using terminology
	// KnowledgeBaseManagement kbm = getKBM(article);
	//
	// QContainer container = kbm.findQContainer(flowName + "_Questionnaire");
	//
	// if (container == null) {
	// errors.add(new Message("Terminology not found for flow'" + flowName +
	// "'"));
	// return NoopAction.INSTANCE;
	// }
	//
	// QuestionMC question = null;
	//
	// for (TerminologyObject child : container.getChildren()) {
	//
	// if (child.getName().equals(
	// flowName + "_" +
	// FlowchartTerminologySubTreeHandler.STARTNODES_QUESTION_NAME)) question =
	// (QuestionMC) child;
	// }
	//
	// if (question == null) {
	// errors.add(new Message("No startnode question found for flow '" +
	// flowName + "'."));
	// return NoopAction.INSTANCE;
	// }
	//
	// ActionSetValue action = FlowFactory.getInstance().createSetValueAction();
	//
	// action.setQuestion(question);
	//
	// Choice answer = null;
	// for (Choice child : question.getAllAlternatives()) {
	// if (child.getName().equals(nodeName)) answer = child;
	//
	// }
	//
	// if (answer == null) {
	// errors.add(new Message("Startnode  '" + nodeName
	// + "' not found in terminology of flow '" + flowName + "'."));
	// return NoopAction.INSTANCE;
	// }
	//
	// //TODO: HOTFIX
	// List<ChoiceValue> values = new LinkedList<ChoiceValue>();
	// values.add(new ChoiceValue(answer));
	//
	// action.setValue(new MultipleChoiceValue(values));
	// //
	// return action;
	// }

	// private PSAction createHeuristicScoreAction(KnowWEArticle article,
	// Section section,
	// String string, List<Message> errors) {
	//
	// String[] split = string.split("=");
	// String solution = split[0].trim();
	// String score = split[1].trim();
	//
	//
	// ActionHeuristicPS action = new ActionHeuristicPS();
	//
	// if (solution.startsWith("\"")) // remove "
	// solution = solution.substring(1, solution.length() - 1);
	//
	// KnowledgeBaseManagement kbm = getKBM(article);
	// Solution diagnosis = kbm.findSolution(solution);
	//
	// if (diagnosis == null) {
	// errors.add(new Message("Diagnosis not found: " + solution));
	// }
	//
	// action.setSolution(diagnosis);
	//
	// if (score.contains("P7")) action.setScore(Score.P7);
	// else action.setScore(Score.N7);
	//
	// return action;
	// }

	// private PSAction createQAindicationAction(KnowWEArticle article, Section
	// section, String string, List<Message> errors) {
	// String name = string.substring(8, string.length() - 1);
	// KnowledgeBaseManagement kbm = getKBM(article);
	// QASet findQuestion = kbm.findQuestion(name);
	//
	// if (findQuestion == null) findQuestion = kbm.findQContainer(name);
	//
	// if (findQuestion == null) {
	// errors.add(new Message("Question not found: " + name));
	// return NoopAction.INSTANCE;
	// }
	//
	// List<QASet> qasets = new ArrayList<QASet>();
	//
	// qasets.add(findQuestion);
	//
	//
	// ActionIndication action = new ActionIndication();
	//
	// action.setQASets(qasets);
	//
	// return action;
	// }

	// private INode createEndNode(KnowWEArticle article, String id, String
	// flowName, Section section, List<Message> errors) {
	// String name = ((Section) section.getChildren().get(1)).getOriginalText();
	//
	// ActionSetValue action = FlowFactory.getInstance().createSetValueAction();
	//
	// QuestionMC question = (QuestionMC) getKBM(article).findQuestion(
	// flowName + "_" +
	// FlowchartTerminologySubTreeHandler.EXITNODES_QUESTION_NAME);
	//
	// action.setQuestion(question);
	//
	// Choice answer = null;
	// for (Choice child : question.getAllAlternatives()) {
	// if (child.getName().equals(name)) {
	// answer = child;
	// break;
	// }
	//
	// }
	//
	// if (answer == null) {
	// errors.add(new Message("No startnode  '" + flowName +
	// "' not found in terminology of flow '" + flowName +"'."));
	// return null;
	// }
	//
	// //HOTFIX
	// List<ChoiceValue> values = new LinkedList<ChoiceValue>();
	// values.add(new ChoiceValue(answer));
	//
	// action.setValue(new MultipleChoiceValue(values));
	// //
	//
	// return FlowFactory.getInstance().createEndNode(id, name, action);
	// }

	// private INode createStartNode(String id, Section section, List<Message>
	// errors) {
	// String name = ((Section) section.getChildren().get(1)).getOriginalText();
	//
	// return FlowFactory.getInstance().createStartNode(id, name);
	//
	// }

}
