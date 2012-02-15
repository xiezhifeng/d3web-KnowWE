/*
 * Copyright (C) 2012 University Wuerzburg, Computer Science VI
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

import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.terminology.NamedObject;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.compile.terminology.TermRegistrationScope;
import de.knowwe.core.kdom.KnowWEArticle;
import de.knowwe.core.kdom.objects.SimpleTermReferenceRegistrationHandler;
import de.knowwe.core.kdom.parsing.Section;

/**
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 13.02.2012
 */
public class NamedObjectReference extends D3webTermReference<NamedObject> {

	public NamedObjectReference() {
		this.setRenderer(new NamedObjectRenderer());
		this.addSubtreeHandler(new SimpleTermReferenceRegistrationHandler(
				TermRegistrationScope.LOCAL));
	}

	@Override
	public Class<?> getTermObjectClass() {
		return NamedObject.class;
	}

	@Override
	public NamedObject getTermObject(KnowWEArticle article, Section<? extends D3webTerm<NamedObject>> section) {
		String termIdentifier = section.get().getTermIdentifier(section);
		KnowledgeBase knowledgeBase = D3webUtils.getKnowledgeBase(article.getWeb(),
				article.getTitle());
		if (termIdentifier.equals("KNOWLEDGEBASE")
				|| termIdentifier.equals(knowledgeBase.getName())) {
			return knowledgeBase;
		}
		return super.getTermObject(article, section);
	}

}
