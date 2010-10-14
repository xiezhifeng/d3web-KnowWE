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

package de.d3web.we.action;

import java.util.List;

import de.d3web.core.knowledge.terminology.QASet;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.session.Value;
import de.d3web.we.basic.DPSEnvironmentManager;
import de.d3web.we.core.DPSEnvironment;
import de.d3web.we.core.KnowWEAttributes;
import de.d3web.we.core.KnowWEParameterMap;
import de.d3web.we.core.broker.Broker;
import de.d3web.we.core.knowledgeService.D3webKnowledgeService;
import de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession;

public class QuestionStateReportAction extends DeprecatedAbstractKnowWEAction {

	/**
	 * Used by GuidelineModul edit in GuidelineRenderer: Method:
	 * d3webVariablesScript Don´t change output syntax
	 */
	@SuppressWarnings("deprecation")
	@Override
	public String perform(KnowWEParameterMap parameterMap) {
		String namespace = java.net.URLDecoder.decode(parameterMap.get(KnowWEAttributes.SEMANO_NAMESPACE));
		String questionID = parameterMap.get(KnowWEAttributes.SEMANO_OBJECT_ID);
		String questionName = parameterMap.get(KnowWEAttributes.TERM);
		if (questionName == null) {
			questionName = parameterMap.get(KnowWEAttributes.SEMANO_TERM_NAME);
		}
		// String valueid = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_ID);
		// String valuenum =
		// parameterMap.get(KnowWEAttributes.SEMANO_VALUE_NUM);
		// String valueids =
		// parameterMap.get(KnowWEAttributes.SEMANO_VALUE_IDS);
		String user = parameterMap.get(KnowWEAttributes.USER);
		String web = parameterMap.get(KnowWEAttributes.WEB);

		String result = "Error on finding question state -wrong id/name ?";

		DPSEnvironment env = DPSEnvironmentManager.getInstance().getEnvironments(web);
		Broker broker = env.getBroker(user);

		D3webKnowledgeServiceSession kss = broker.getSession().getServiceSession(namespace);
		D3webKnowledgeService service = env.getService(namespace);
		Question q = null;
		if (service instanceof D3webKnowledgeService) {

			if (questionID != null) {

				QASet set = (service).getBase()
						.searchQASet(questionID);
				if (set instanceof Question) {

					q = ((Question) set);
				}
			}

			if (questionName != null) {

				List<Question> questionList = (service).getBase().getQuestions();
				for (Question question : questionList) {
					if (question.getName().equals(questionName)) {

						q = question;
					}
				}
			}
		}

		// if(kss instanceof
		// de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession){
		//
		// }

		if (kss instanceof de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession
				&& q != null) {

			D3webKnowledgeServiceSession d3kss = (kss);
			de.d3web.core.session.Session case1 = d3kss.getSession();
			List<? extends Question> answeredQuestions = case1.getBlackboard().getAnsweredQuestions();
			if (answeredQuestions.contains(q)) {
				Value theanswer = case1.getBlackboard().getValue(q);
				result = "#" + q.getName() + ":";
				if (theanswer != null) {
					result += theanswer.getValue().toString() + ";";
				}
				else {
					result += "no answer object";
				}
			}
			else {
				result = "undefined";
			}
		}

		return result;
	}

}
