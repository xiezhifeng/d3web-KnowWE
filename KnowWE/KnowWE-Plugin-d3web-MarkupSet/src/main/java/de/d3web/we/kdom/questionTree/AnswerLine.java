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

package de.d3web.we.kdom.questionTree;

import java.util.Arrays;
import java.util.Collection;

import de.d3web.core.knowledge.InfoStore;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.info.BasicProperties;
import de.d3web.core.knowledge.terminology.info.MMInfo;
import de.d3web.we.basic.D3webModule;
import de.d3web.we.kdom.AbstractType;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.Sections;
import de.d3web.we.kdom.objects.IncrementalMarker;
import de.d3web.we.kdom.rendering.StyleRenderer;
import de.d3web.we.kdom.report.KDOMReportMessage;
import de.d3web.we.kdom.report.message.ObjectCreatedMessage;
import de.d3web.we.kdom.report.message.ObjectCreationError;
import de.d3web.we.kdom.sectionFinder.AllTextFinderTrimmed;
import de.d3web.we.kdom.sectionFinder.AllTextSectionFinder;
import de.d3web.we.kdom.sectionFinder.ConditionalSectionFinder;
import de.d3web.we.kdom.sectionFinder.MatchUntilEndFinder;
import de.d3web.we.kdom.sectionFinder.OneOfStringEnumFinder;
import de.d3web.we.kdom.sectionFinder.StringSectionFinderUnquoted;
import de.d3web.we.kdom.subtreeHandler.IncrementalConstraint;
import de.d3web.we.kdom.subtreeHandler.SubtreeHandler;
import de.d3web.we.object.AnswerDefinition;
import de.d3web.we.object.QuestionDefinition;
import de.d3web.we.reviseHandler.D3webSubtreeHandler;
import de.d3web.we.utils.SplitUtility;
import de.knowwe.core.dashtree.DashTreeElement;
import de.knowwe.core.dashtree.DashTreeUtils;

/**
 * Answerline of the questionTree; a dashTreeElement is an AnswerLine if its
 * DashTree father is a Question (and it hasn't been allocated as question also
 * before)
 * 
 * @author Jochen
 * 
 */
public class AnswerLine extends AbstractType {

	@Override
	protected void init() {
		this.sectionFinder = new ConditionalSectionFinder(new AllTextSectionFinder()) {

			@Override
			protected boolean condition(String text, Section<?> father) {

				Section<?> dashTreeElement = father.getFather();
				if (dashTreeElement.get() instanceof DashTreeElement) {
					Section<? extends DashTreeElement> dashFather = DashTreeUtils
							.getFatherDashTreeElement(dashTreeElement);
					if (dashFather != null
							&& Sections.findSuccessor(dashFather, QuestionLine.class) != null) {
						return true;
					}
				}

				return false;
			}
		};

		// description text - startet by '~'
		this.childrenTypes.add(new AnswerText());
		
		QuestionTreeAnswerDefinition aid = new QuestionTreeAnswerDefinition();
		aid.setSectionFinder(new AllTextFinderTrimmed());
		this.childrenTypes.add(aid);
		

	}

	/**
	 * Allows for the definition of abstract-flagged questions Syntax is:
	 * "<init>"
	 * 
	 * The subtreehandler creates the corresponding
	 * BasicProperties.INIT in the knoweldge base
	 * 
	 * 
	 * @author Jochen
	 * 
	 */
	static class InitFlag extends AbstractType {

		public InitFlag() {
			this.sectionFinder = new OneOfStringEnumFinder(new String[] {
					"<init>" });
			this.setCustomRenderer(StyleRenderer.KEYWORDS);

			this.addSubtreeHandler(new SubtreeHandler<InitFlag>() {

				@Override
				public Collection<KDOMReportMessage> create(KnowWEArticle article, Section<InitFlag> s) {

					Section<AnswerDefinition> aDef = Sections.findSuccessor(
							s.getFather(), AnswerDefinition.class);

					Section<? extends QuestionDefinition> qdef = aDef.get().getQuestionSection(
							aDef);

					if (qdef != null) {

						Question question = qdef.get().getTermObject(article, qdef);

						String answerName = aDef.get().getTermObject(article, aDef).getName();

						InfoStore infoStore = question.getInfoStore();
						Object p = infoStore.getValue(BasicProperties.INIT);

						if (p == null) {
							infoStore.addValue(BasicProperties.INIT, answerName);
						}
						else {
							if (p instanceof String) {
								String newValue = ((String) p).concat(";" + answerName);
								infoStore.addValue(BasicProperties.INIT, newValue);
							}

						}
						return Arrays.asList((KDOMReportMessage) new ObjectCreatedMessage(
								D3webModule.getKwikiBundle_d3web()
										.getString("KnowWE.questiontree.abstractquestion")));

					}
					return Arrays.asList((KDOMReportMessage) new ObjectCreationError(
							D3webModule.getKwikiBundle_d3web()
									.getString("KnowWE.questiontree.abstractflag"),
							this.getClass()));
				}
			});
		}
	}
	
	/**
	 * A type to allow for the definition of (extended) question-text for a
	 * question leaded by '~'
	 * 
	 * the subtreehandler creates the corresponding DCMarkup using
	 * MMInfoSubject.PROMPT for the question object
	 * 
	 * @author Jochen
	 * 
	 */
	static class AnswerText extends AbstractType implements IncrementalMarker, IncrementalConstraint<AnswerText> {

		private static final String QTEXT_START_SYMBOL = "~";

		@Override
		public boolean violatedConstraints(KnowWEArticle article, Section<AnswerText> s) {
			return QuestionDashTreeUtils.isChangeInRootQuestionSubtree(article, s);
		}

		@Override
		protected void init() {
			this.sectionFinder = new MatchUntilEndFinder(new StringSectionFinderUnquoted(
					QTEXT_START_SYMBOL));

			this.setCustomRenderer(StyleRenderer.PROMPT);
			this.addSubtreeHandler(new D3webSubtreeHandler<AnswerText>() {

				@Override
				public Collection<KDOMReportMessage> create(KnowWEArticle article, Section<AnswerText> sec) {

					Section<AnswerDefinition> aDef = Sections.findSuccessor(
							sec.getFather(), AnswerDefinition.class);
					
					Section<? extends QuestionDefinition> qSec = aDef.get().getQuestionSection(aDef); 

					if (aDef != null && qSec != null) {

						Question question = qSec.get().getTermObject(article, qSec);
						Choice choice = aDef.get().getTermObject(article, aDef);

						if (question != null && choice != null) {
							choice.getInfoStore().addValue(MMInfo.PROMPT,
									AnswerText.getAnswerText(sec));
							return Arrays.asList((KDOMReportMessage) new ObjectCreatedMessage(
									"Answer text set"));
						}
					}
					return Arrays.asList((KDOMReportMessage) new ObjectCreationError(
							D3webModule.getKwikiBundle_d3web()
									.getString("KnowWE.questiontree.questiontext"),
							this.getClass()));
				}

				@Override
				public void destroy(KnowWEArticle article, Section<AnswerText> sec) {
					// text is destroyed together with object
				}
			});
		}

		public static String getAnswerText(Section<AnswerText> s) {
			String text = s.getOriginalText();
			if (text.startsWith(QTEXT_START_SYMBOL)) {
				text = text.substring(1).trim();
			}

			return SplitUtility.unquote(text);
		}
	}

}
