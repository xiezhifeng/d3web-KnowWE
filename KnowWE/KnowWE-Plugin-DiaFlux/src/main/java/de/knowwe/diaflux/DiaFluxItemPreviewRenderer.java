package de.knowwe.diaflux;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.DelegateRenderer;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.preview.PreviewRenderer;
import de.knowwe.core.user.UserContext;
import de.knowwe.diaflux.type.ActionType;
import de.knowwe.diaflux.type.CommentType;
import de.knowwe.diaflux.type.DecisionType;
import de.knowwe.diaflux.type.EdgeType;
import de.knowwe.diaflux.type.ExitType;
import de.knowwe.diaflux.type.FlowchartType;
import de.knowwe.diaflux.type.FlowchartXMLHeadType;
import de.knowwe.diaflux.type.FlowchartXMLHeadType.FlowchartTermDef;
import de.knowwe.diaflux.type.GuardType;
import de.knowwe.diaflux.type.NodeContentType;
import de.knowwe.diaflux.type.NodeType;
import de.knowwe.diaflux.type.SnapshotType;
import de.knowwe.diaflux.type.StartType;

public class DiaFluxItemPreviewRenderer implements PreviewRenderer {

	@Override
	public void render(Section<?> section, Collection<Section<?>> relevantSubSections, UserContext user, RenderResult result) {
		Section<FlowchartType> self = Sections.cast(section, FlowchartType.class);
		renderFlowchartName(self, user, result);

		// build sets first to avoid duplicates
		Set<Section<NodeType>> nodes = new LinkedHashSet<Section<NodeType>>();
		Set<Section<EdgeType>> edges = new LinkedHashSet<Section<EdgeType>>();
		for (Section<?> item : relevantSubSections) {
			Section<NodeType> node = Sections.findAncestorOfType(item, NodeType.class);
			if (node != null) {
				nodes.add(node);
			}
			Section<EdgeType> edge = Sections.findAncestorOfType(item, EdgeType.class);
			if (edge != null) {
				edges.add(edge);
			}
		}

		// then render the created sets
		for (Section<NodeType> node : nodes) {
			renderNode(node, user, result);
		}
		for (Section<EdgeType> edge : edges) {
			renderEdge(edge, user, result);
		}
	}

	private void renderFlowchartName(Section<FlowchartType> self, UserContext user, RenderResult result) {
		Section<FlowchartXMLHeadType> head =
				Sections.findSuccessor(self, FlowchartXMLHeadType.class);
		if (head != null) {
			Section<FlowchartTermDef> term = Sections.findSuccessor(head, FlowchartTermDef.class);
			if (term != null) {
				DelegateRenderer.getRenderer(term, user).render(term, user, result);
				return;
			}
		}
		String name = FlowchartType.getFlowchartName(self);
		result.append(name);
	}

	private void renderEdge(Section<EdgeType> edge, UserContext user, RenderResult result) {
		result.appendHtml("<div class='preview edge'>&nbsp;&nbsp;").append("- Edge ");
		Section<GuardType> guard = Sections.findSuccessor(edge, GuardType.class);
		if (guard != null) {
			DelegateRenderer.getRenderer(guard, user).render(guard, user, result);
		}
		result.appendHtml("</div>");
	}

	private void renderNode(Section<NodeType> node, UserContext user, RenderResult result) {
		Section<NodeContentType> action = Sections.findSuccessor(node, NodeContentType.class);
		result.appendHtml("<div class='preview node ")
				.append(getNodeCSS(action))
				.appendHtml("'>&nbsp;&nbsp;").append("- Node ");
		renderNodePreviewHtml(action, user, result);
		result.appendHtml("</div>");
	}

	private String getNodeCSS(Section<NodeContentType> action) {
		if (Sections.findChildOfType(action, StartType.class) != null) return "type_Start";
		if (Sections.findChildOfType(action, ExitType.class) != null) return "type_Exit";
		if (Sections.findChildOfType(action, ActionType.class) != null) return "type_Action";
		if (Sections.findChildOfType(action, CommentType.class) != null) return "type_Comment";
		if (Sections.findChildOfType(action, SnapshotType.class) != null) return "type_Snapshot";
		if (Sections.findChildOfType(action, DecisionType.class) != null) return "type_Decision";
		return "type_Unexpected";
	}

	private void renderNodePreviewHtml(Section<NodeContentType> nodeSection, UserContext user, RenderResult result) {
		Section<?> section = Sections.findChildOfType(nodeSection, StartType.class);
		if (section != null) {
			DelegateRenderer.getRenderer(section, user).render(section, user, result);
			return;
		}

		section = Sections.findChildOfType(nodeSection, ExitType.class);
		if (section != null) {
			DelegateRenderer.getRenderer(section, user).render(section, user, result);
			return;
		}

		section = Sections.findChildOfType(nodeSection, ActionType.class);
		if (section != null) {
			DelegateRenderer.getRenderer(section, user).render(section, user, result);
			return;
		}

		section = Sections.findChildOfType(nodeSection, CommentType.class);
		if (section != null) {
			renderPrefixedXMLContent("Comment: ", section, user, result);
			return;
		}

		section = Sections.findChildOfType(nodeSection, SnapshotType.class);
		if (section != null) {
			renderPrefixedXMLContent("", section, user, result);
			return;
		}

		section = Sections.findChildOfType(nodeSection, DecisionType.class);
		if (section != null) {
			DelegateRenderer.getRenderer(section, user).render(section, user, result);
			return;
		}

		result.appendEntityEncoded(nodeSection.getText());
	}

	private void renderPrefixedXMLContent(String prefix, Section<?> section, UserContext user, RenderResult result) {
		result.append(prefix);
		result.appendEntityEncoded(section.getText());
	}

}