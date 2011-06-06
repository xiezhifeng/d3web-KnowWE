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
package de.d3web.we.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.knowledge.terminology.QuestionYN;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.we.core.KnowWEEnvironment;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Priority;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.objects.KnowWETerm;
import de.d3web.we.kdom.objects.NotUniqueKnowWETerm;
import de.d3web.we.kdom.rendering.StyleRenderer;
import de.d3web.we.kdom.report.KDOMReportMessage;
import de.d3web.we.kdom.report.message.NewObjectCreated;
import de.d3web.we.kdom.report.message.ObjectCreationError;
import de.d3web.we.kdom.report.message.UnexpectedSequence;
import de.d3web.we.kdom.subtreeHandler.IncrementalConstraint;
import de.d3web.we.reviseHandler.D3webSubtreeHandler;
import de.d3web.we.utils.KnowWEUtils;

/**
 * 
 * @author Jochen/Albrecht
 * @created 26.07.2010
 * 
 * 
 *          This is the type to be used for markup defining new (d3web-)
 *          Choice-Answers. It checks whether the corresponding question is
 *          existing and is compatible. In case it creates the Answer object in
 *          the knowledge base.
 * 
 * 
 * 
 */
public abstract class AnswerDefinition
		extends D3webTermDefinition<Choice>
		implements IncrementalConstraint<AnswerDefinition>{

	private static final String QUESTION_FOR_ANSWER_KEY = "QUESTION_FOR_ANSWER_KEY";

	public AnswerDefinition() {
		super(Choice.class);
		this.addSubtreeHandler(Priority.HIGH, new CreateAnswerHandler());
		this.setCustomRenderer(
				StyleRenderer.CHOICE);
		this.setOrderSensitive(true);
	}

	public abstract int getPosition(Section<? extends AnswerDefinition> s);

	/**
	 * 
	 * returns the section of the question this answer belongs to
	 * 
	 * @created 26.07.2010
	 * @param s
	 * @return
	 */
	public abstract Section<? extends QuestionDefinition> getQuestionSection(Section<? extends AnswerDefinition> s);

	@Override
	public boolean violatedConstraints(KnowWEArticle article, Section<AnswerDefinition> s) {
		return false;
	}

	@Override
	public String getTermIdentifier(Section<? extends KnowWETerm<Choice>> s) {
		// here we should return a unique identifier including the question name
		// as namespace
		KnowWETerm<Choice> knowWETerm = s.get();
		if (knowWETerm instanceof AnswerDefinition) {
			Section<AnswerDefinition> sec = ((Section<AnswerDefinition>) s);
			Section<? extends QuestionDefinition> questionSection = sec.get().getQuestionSection(
					sec);
			String question = questionSection.get().getTermIdentifier(questionSection);

			return createAnswerIdentifierForQuestion(super.getTermIdentifier(sec), question);
		}

		// should not happen
		return super.getTermIdentifier(s);
	}

	/*
	 * @Override
	 * 
	 * @SuppressWarnings("unchecked") public String
	 * getUniqueTermIdentifier(KnowWEArticle article, Section<? extends
	 * KnowWETerm<Choice>> s) {
	 * 
	 * String answer = s.get().getTermName(s);
	 * 
	 * Section<? extends QuestionDefinition> qdef = (Section<? extends
	 * QuestionDefinition>) KnowWEUtils.getStoredObject(article, s,
	 * QUESTION_FOR_ANSWER_KEY); if (qdef == null) { qdef =
	 * getQuestionSection((Section<AnswerDefinition>) s); }
	 * 
	 * String question = null; if (qdef == null) { // should not happen, if does
	 * check whether getQuestion() is // (correctly) overridden by the (custom)
	 * AnswerDefintion question = "questionNotFound";
	 * Logger.getLogger(this.getClass().getName()) .log(Level.WARNING,
	 * "QuestionSection for AnswerDefintion couldnt be found: '" + answer +
	 * "'!"); } else { question = qdef.get().getTermName(qdef); }
	 * 
	 * return question + " " + answer; }
	 */
	/**
	 * 
	 * @author Jochen
	 * @created 26.07.2010
	 * 
	 *          This handler actually creates the Answer as an object of the
	 *          knowledge base
	 */
	static class CreateAnswerHandler extends D3webSubtreeHandler<AnswerDefinition> {

		@Override
		public Collection<KDOMReportMessage> create(KnowWEArticle article,
				Section<AnswerDefinition> s) {

			String name = s.get().getTermIdentifier(s);

			Section<? extends QuestionDefinition> qDef = s.get().getQuestionSection(s);
			KnowWEUtils.storeObject(article, s, AnswerDefinition.QUESTION_FOR_ANSWER_KEY,
					qDef);
			// storing the current question needs to happen first, so the method
			// getUniqueTermIdentifier() can use the right question.

			if (!KnowWEUtils.getTerminologyHandler(article.getWeb()).registerTermDefinition(
					article, s)) {
				return new ArrayList<KDOMReportMessage>(0);
			}

			if (qDef == null) {
				// this situation can only occur with incremental update
				// -> fullparse
				if (article != s.getArticle()) {
					KnowWEEnvironment.getInstance().getArticleManager(s.getWeb()).registerArticle(
							KnowWEArticle.createArticle(
									s.getArticle().getSection().getOriginalText(),
									s.getTitle(),
									KnowWEEnvironment.getInstance().getRootType(),
									s.getWeb(), true), false);
				}
				article.setFullParse(CreateAnswerHandler.class);
				return null;
			}

			// if having error somewhere, do nothing and report error
			if (qDef.hasErrorInSubtree(article)) {
				return Arrays.asList((KDOMReportMessage) new ObjectCreationError(
						"no valid question - " + name,
						this.getClass()));
			}

			Question q = qDef.get().getTermObject(article, qDef);

			if (q instanceof QuestionChoice) {

				Choice a;

				// special treatment for QuestionYN
				// Answers are not created, but mapped to the already existing
				// Choices.
				if (q instanceof QuestionYN) {
					QuestionYN qyn = (QuestionYN) q;
					if (name.equals(qyn.getAnswerChoiceYes().getName())) {
						a = qyn.getAnswerChoiceYes();
					}
					else if (name.equals(qyn.getAnswerChoiceNo().getName())) {
						a = qyn.getAnswerChoiceNo();
					}
					else {
						return Arrays.asList((KDOMReportMessage) new UnexpectedSequence(
								"only '" + qyn.getAnswerChoiceYes().getName() + "' and '"
										+ qyn.getAnswerChoiceNo().getName()
										+ "' is allowed for this question type"));
					}
				}
				else {
					a = KnowledgeBaseUtils.addChoiceAnswer((QuestionChoice) q,
							getShortAnswerName(name, qDef.get().getTermIdentifier(qDef)),
							s.get().getPosition(s));

				}

				s.get().storeTermObject(article, s, a);

				return Arrays.asList((KDOMReportMessage) new NewObjectCreated(
						a.getClass().getSimpleName() + "  "
								+ a.getName()));

			}
			return Arrays.asList((KDOMReportMessage) new ObjectCreationError(
					"no choice question - " + name,
					this.getClass()));
		}

		@Override
		public void destroy(KnowWEArticle article, Section<AnswerDefinition>
				s) {

			KnowWEUtils.getTerminologyHandler(article.getWeb()).unregisterTermDefinition(
					article, s);
			// why does this work?
			// explanation:
			// the answer is (un)registered using the uniqueTermIdentifier,
			// which uses either the stored father question or, if there
			// is no stored father question, retrieves the father question
			// again... both variants work correctly:
			//
			// 1) the answer is not reused in the new KDOM... in this case
			// the uniqueTermIdentifier will not find a stores question
			// section, but since the answer wasn't reused it is still
			// hooked in the same father question -> new retrieval of the
			// father Question still returns the correct one
			//
			// 2) the answer section is reused but needs to be destroyed
			// anyway, because for example the position has changed -> since
			// the answer section is reused, uniqueTermIdentifier will find
			// the stored question which is still the one the answer was
			// hooked in in the last KDOM.
		}
	}

	/**
	 * 
	 * creates a global unique identifier for an answer by use of the question
	 * name (which is globally unique)
	 * 
	 * inverse function of:
	 * @see AnswerDefinition.getShortAnswerName()
	 * 
	 * IMPORTANT: Don't modify one without the other
	 * 
	 * @created 06.06.2011
	 * @param answer
	 * @param question
	 * @return
	 */
	public static String createAnswerIdentifierForQuestion(String answer, String question) {
		return question + " " + answer;
	}

	/**
	 * returns the short answer name, that is only unique within the scope of the question.
	 * 
	 * inverse function of:
	 * @see AnswerDefinition.createAnswerIdentifierForQuestion()
	 * 
	 * IMPORTANT: Don't modify one without the other
	 * 
	 * @created 06.06.2011
	 * @param answerindentifier
	 * @param question
	 * @return 
	 */
	public static String getShortAnswerName(String answerindentifier, String question) {
		return answerindentifier.substring(question.length() + 1).trim();
	}

}
