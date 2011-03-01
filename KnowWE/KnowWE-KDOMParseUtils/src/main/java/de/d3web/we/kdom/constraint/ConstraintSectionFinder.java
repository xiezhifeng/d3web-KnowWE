/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
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

package de.d3web.we.kdom.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.d3web.we.kdom.Type;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.sectionFinder.ISectionFinder;
import de.d3web.we.kdom.sectionFinder.SectionFinderResult;

/**
 * @author Jochen
 * 
 *         An abstract SectionFnder class that allows for the use of so called
 *         SectionFinderConstraints This simplifies the implementation of
 *         certain SectionFinders. E.g., if only the first occurrence of some
 *         text-entity should be allocated for a type, then the
 *         'AtMostOneFindingConstraint' can be applied, filtering out additional
 *         matches
 * 
 */
public class ConstraintSectionFinder implements ISectionFinder {

	private final ISectionFinder finder;

	public ConstraintSectionFinder(ISectionFinder finder) {
		this.finder = finder;
	}

	public ConstraintSectionFinder(ISectionFinder finder, SectionFinderConstraint c) {
		this.finder = finder;
		this.constraintList = new ArrayList<SectionFinderConstraint>();
		this.constraintList.add(c);
	}

	/**
	 * 
	 * TODO: remove - shouldnt be accessed externally
	 * 
	 * @created 20.07.2010
	 * @return
	 */
	public List<SectionFinderConstraint> getConstraints() {
		return constraintList;
	}

	private List<SectionFinderConstraint> constraintList;

	public void addConstraint(SectionFinderConstraint constraint) {
		if (constraintList == null) {
			constraintList = new ArrayList<SectionFinderConstraint>();

		}
		this.constraintList.add(constraint);
	}

	/**
	 * First the lookForSection-task is delegated the actual implementation.
	 * Then The SectionFinderConstraints are filtered
	 */
	@Override
	public List<SectionFinderResult> lookForSections(String text,
			Section<?> father, Type type) {

		List<SectionFinderResult> result = finder.lookForSections(text, father, type);
		if (result != null) {
			Collections.sort(result);
			applyConstraints(result, father, type.getClass());
		}

		return result;
	}

	/**
	 * Delegates the lookForSection-task to the actual implementation
	 * 
	 * @created 20.07.2010
	 * @param text
	 * @param father
	 * @return
	 */

	private void applyConstraints(List<SectionFinderResult> results, Section<?> father,
			Class<? extends Type> ob) {

		if (constraintList == null) return;

		for (SectionFinderConstraint sectionFinderConstraint : constraintList) {
			if (!sectionFinderConstraint.satisfiesConstraint(results, father, ob)) {
				sectionFinderConstraint.filterCorrectResults(results, father, ob);
			}
		}

	}

}
