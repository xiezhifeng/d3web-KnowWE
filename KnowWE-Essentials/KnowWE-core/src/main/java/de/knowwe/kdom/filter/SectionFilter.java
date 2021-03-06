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

package de.knowwe.kdom.filter;

import java.util.Iterator;

import com.denkbares.collections.FilterIterator;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;

public interface SectionFilter {

	boolean accept(Section<?> section);

	SectionFilter ALL_SECTIONS = section -> true;

	SectionFilter NO_SECTIONS = section -> false;

	static <T extends Type> Iterator<Section<T>> filter(Iterator<Section<T>> base, SectionFilter filter) {
		return new FilterIterator<Section<T>>(base) {
			@Override
			public boolean accept(Section<T> item) {
				return filter.accept(item);
			}
		};
	}
}
