package de.knowwe.ontology.kdom.turtle;

import java.util.ArrayList;
import java.util.List;

import de.d3web.strings.Strings;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.sectionFinder.SectionFinder;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;

public class FirstWordFinder implements SectionFinder {

	@Override
	public List<SectionFinderResult> lookForSections(String text, Section<?> father, Type type) {
		List<SectionFinderResult> result = new ArrayList<SectionFinderResult>(1);
		if (text.length() == 0) return result;

		String trimmedText = Strings.trim(text);
		int leadingWhiteSpaceChars = text.indexOf(trimmedText);
		int indexOfFirstWhitspace = Strings.indexOfUnquoted(trimmedText, " ", "\t", "\n", "\r");

		// if no whitespace is found, entire text is taken as one 'word'
		if (indexOfFirstWhitspace == Integer.MAX_VALUE) {
			indexOfFirstWhitspace = trimmedText.length();
		}
		// return first 'word' of input
		result.add(new SectionFinderResult(leadingWhiteSpaceChars, leadingWhiteSpaceChars
				+ indexOfFirstWhitspace));
		return result;
	}
}