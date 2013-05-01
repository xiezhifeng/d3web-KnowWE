/*
 * Copyright (C) 2013 denkbares GmbH
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
package de.knowwe.core.kdom.sectionFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;

/**
 * Finds Sections starting with the opening and ending with the closing String
 * (given in the constructor). If there are nested sections with the same
 * opening and closing, they are ignored.
 * <p>
 * <b>Example:</b>
 * <p>
 * 
 * In the Text:<br>
 * <br>
 * "Calculation 5 - (2 * (4 / 2)) = 1"<br>
 * <br>
 * a new {@link NestedBracketsFinder}("(", ")") will find the Section<br>
 * <br>
 * "(2 * (4 / 2))"
 * 
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 24.04.2013
 */
public class NestedBracketsFinder implements SectionFinder {

	private final Pattern pattern;

	public NestedBracketsFinder(String opening, String optionalOpeningSuffix, String closing) {
		String optionalOpeningSuffixRegex = optionalOpeningSuffix == null ?
				"" : "(" + Pattern.quote(optionalOpeningSuffix) + ")?";
		String regex = "(" + Pattern.quote(opening) + ")" + optionalOpeningSuffixRegex + "|("
				+ Pattern.quote(closing) + "(?:\\r?\\n)?)";
		this.pattern = Pattern.compile(regex);
	}

	public NestedBracketsFinder(String opening, String closing) {
		this(opening, null, closing);
	}

	@Override
	public List<SectionFinderResult> lookForSections(String text, Section<?> father, Type type) {
		Matcher matcher = pattern.matcher(text);
		int depth = 0;
		int start = -1;
		List<SectionFinderResult> results = new ArrayList<SectionFinderResult>();
		while (matcher.find()) {
			int groupCount = matcher.groupCount();
			boolean foundNesting = matcher.group(1) != null;
			boolean foundOpening = foundNesting
					&& (groupCount == 3 ? matcher.group(2) != null : true);
			boolean foundClosing = matcher.group(groupCount) != null;
			if (foundOpening) {
				if (depth == 0) {
					start = matcher.start(1);
				}
			}
			if (foundNesting && start > -1) {
				depth++;
			}
			if (foundClosing) {
				depth--;
				if (depth == 0) {
					results.add(new SectionFinderResult(start, matcher.end(groupCount)));
					start = -1;
				}
			}

		}
		return results;
	}
}