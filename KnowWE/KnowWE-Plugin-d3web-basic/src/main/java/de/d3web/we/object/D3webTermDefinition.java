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

import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.objects.KnowWETerm;
import de.d3web.we.kdom.objects.TermDefinition;
import de.d3web.we.utils.KnowWEUtils;

/**
 * 
 * This is an abstract class for types defining objects in d3web, such as
 * solutions, questions, questionnaires...
 * 
 * @author Jochen/Albrecht
 * @created 26.07.2010
 * @param <TermObject>
 */
public abstract class D3webTermDefinition<TermObject> extends TermDefinition<TermObject> {

	public D3webTermDefinition(Class<TermObject> termObjectClass) {
		super(termObjectClass);
	}

	@Override
	public String getTermIdentifier(Section<? extends KnowWETerm<TermObject>> s) {
		return KnowWEUtils.trimQuotes(s.getOriginalText());
	}

}
