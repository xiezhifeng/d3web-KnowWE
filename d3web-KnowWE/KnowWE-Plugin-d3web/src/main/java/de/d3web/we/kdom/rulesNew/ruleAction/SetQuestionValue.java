/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package de.d3web.we.kdom.rulesNew.ruleAction;

import java.util.ArrayList;
import java.util.List;

import de.d3web.abstraction.ActionSetValue;
import de.d3web.core.inference.PSAction;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.session.values.Choice;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.kopic.rules.ruleActionLine.Equals;
import de.d3web.we.kdom.objects.AnswerRef;
import de.d3web.we.kdom.objects.AnswerRefImpl;
import de.d3web.we.kdom.objects.QuestionRef;
import de.d3web.we.kdom.objects.QuestionRefImpl;
import de.d3web.we.kdom.sectionFinder.AllBeforeTypeSectionFinder;
import de.d3web.we.kdom.sectionFinder.AllTextFinderTrimmed;
import de.d3web.we.kdom.sectionFinder.SectionFinder;
import de.d3web.we.kdom.sectionFinder.SectionFinderResult;
import de.d3web.we.utils.SplitUtility;

/**
 * @author Johannes Dienst
 *
 */
public class SetQuestionValue extends D3webRuleAction<SetQuestionValue> {

	@Override
	public void init() {
		this.sectionFinder = new SetQuestionValueSectionFinder();
		Equals equals = new Equals();
		QuestionRef qr = new QuestionRefImpl<Question>();
		qr.setSectionFinder(new AllBeforeTypeSectionFinder(equals));
		this.childrenTypes.add(equals);
		this.childrenTypes.add(qr);

		AnswerRef a = new AnswerRefImpl();
		a.setSectionFinder(AllTextFinderTrimmed.getInstance());
		this.childrenTypes.add(a);

		// this.childrenTypes.add(qr);
		// AddedValue aA = new AddedValue();
		// aA.setSectionFinder(new SetValueSectionFinder());
		// this.childrenTypes.add(aA);
		// AddedValue aA2 = new AddedValue();
		// aA2.setSectionFinder(new WordSectionFinder());
		// this.childrenTypes.add(aA2);
	}

	/**
	 * This works because it is no DiagnosisRuleAction.
	 */
	private class SetQuestionValueSectionFinder extends SectionFinder {

		@Override
		public List<SectionFinderResult> lookForSections(String text, Section father) {

			if (SplitUtility.containsUnquoted(text, " =")) {

				List<SectionFinderResult> result = new ArrayList<SectionFinderResult>();
				result.add(new SectionFinderResult(0, text.length()));
				return result;
			}

			return null;
		}
	}

	private class SetValueSectionFinder extends SectionFinder {

		@Override
		public List<SectionFinderResult> lookForSections(String text, Section father) {

			// Formula Expressions
			// TODO Could also find AddQuestion Value!
			if (text.contains("(") && text.contains(")")) {
				List<SectionFinderResult> result = new ArrayList<SectionFinderResult>();
				result.add(new SectionFinderResult(text.indexOf("(") + 1,
									text.lastIndexOf(")")));
				return result;
			}

			return null;
		}
	}

	public class WordSectionFinder extends SectionFinder {

		@Override
		public List<SectionFinderResult> lookForSections(String text, Section father) {

			if (!text.equals(" ") && !text.equals("\"")
					&& !text.contains("(") && !text.contains(")")) {

				int start = 0;
				int end = text.length();
				while (text.charAt(start) == ' ' || text.charAt(start) == '"') {
					start++;
					if (start >= end) return null;
				}
				while (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '"') {
					end--;
					if (start >= end) return null;
				}

				List<SectionFinderResult> result = new ArrayList<SectionFinderResult>();
				result.add(new SectionFinderResult(start, end));
				return result;
			}
			return null;
		}

	}

	@Override
	public PSAction getAction(Section<SetQuestionValue> s) {
		Section<QuestionRef> qref = s.findSuccessor(QuestionRef.class);
		Question q = qref.get().getObject(qref);
		Section<AnswerRef> aref = s.findSuccessor(AnswerRef.class);
		Choice c = aref.get().getObject(aref);

		if (q != null && c != null) {
			ActionSetValue a = new ActionSetValue();
			a.setQuestion(q);
			a.setValue(c);
			return a;
		}
		return null;
	}
}
