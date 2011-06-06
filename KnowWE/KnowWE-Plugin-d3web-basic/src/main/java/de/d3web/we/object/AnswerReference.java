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

import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.we.basic.D3webModule;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.objects.KnowWETerm;
import de.d3web.we.kdom.objects.NotUniqueKnowWETerm;
import de.d3web.we.kdom.objects.TermReference;
import de.d3web.we.kdom.rendering.StyleRenderer;

/**
 * 
 * @author Jochen/Albrecht
 * @created 26.07.2010
 * 
 *          This is the type to be used in markup for referencing (d3web-)
 *          Choice-Answers. It checks whether the referenced object is existing.
 *          In case it creates the Answer object in the knowledge base.
 * 
 */
public abstract class AnswerReference
		extends D3webTermReference<Choice> {

	public AnswerReference() {
		super(Choice.class);
		this.setCustomRenderer(StyleRenderer.CHOICE);
	}
	
	@Override
	public String getTermIdentifier(Section<? extends KnowWETerm<Choice>> s) {
		// here we should return a unique identifier including the question name as namespace
		
		KnowWETerm<Choice> knowWETerm = s.get();
		if(knowWETerm instanceof AnswerReference) {
			Section<AnswerReference> sec = ((Section<AnswerReference>)s);
			Section<QuestionReference> questionSection = sec.get().getQuestionSection(sec);
			String question = questionSection.get().getTermIdentifier(questionSection);
			
			return AnswerDefinition.createAnswerIdentifierForQuestion(super.getTermIdentifier(sec), question);
		}
		
		//should not happen
		return super.getTermIdentifier(s);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Choice getTermObjectFallback(KnowWEArticle article, Section<? extends
			TermReference<Choice>> s) {

		if (s.get() instanceof AnswerReference) {
			Section<AnswerReference> sec = (Section<AnswerReference>) s;

			Section<QuestionReference> ref = sec.get().getQuestionSection(sec);
			String questionName = ref.get().getTermIdentifier(ref);

			String answerName = sec.get().getTermIdentifier(sec);

			KnowledgeBase kb =
					D3webModule.getKnowledgeRepresentationHandler(
							s.getArticle().getWeb())
							.getKB(article.getTitle());

			Question question = kb.getManager().searchQuestion(questionName);
			if (question != null && question instanceof QuestionChoice) {
				return KnowledgeBaseUtils.findChoice((QuestionChoice) question,
						answerName);

			}

		}

		return null;

	}

	/**
	 * returns the section of the corresponding question-reference for this
	 * answer
	 * 
	 * @param s
	 * @return
	 */
	/**
	 * 
	 * @created 26.07.2010
	 * @param s
	 * @return
	 */
	public abstract Section<QuestionReference> getQuestionSection(Section<? extends AnswerReference> s);

	/*
	@Override
	@SuppressWarnings("unchecked")
	public String getUniqueTermIdentifier(KnowWEArticle article, Section<? extends KnowWETerm<Choice>> s) {

		String answer = s.get().getTermName(s);

		Section<QuestionReference> questionSection = getQuestionSection((Section<? extends AnswerReference>) s);
		
		String question = null;
		if (questionSection == null) {
			// should not happen, if does check whether getQuestion() is
			// (correctly) overridden by the (custom) AnswerDefintion
			question = "questionNotFound";
			Logger.getLogger(this.getClass().getName())
					.log(Level.WARNING,
							"QuestionSection for AnswerDefintion couldnt be found: '" +
									answer + "'!");
		}
		return createAnswerIdentifierForQuestion(answer, question);
	}
*/


	@Override
	public String getTermObjectDisplayName() {
		return "Choice";
	}

}
