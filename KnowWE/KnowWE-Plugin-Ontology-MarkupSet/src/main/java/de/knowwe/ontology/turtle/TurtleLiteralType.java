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
package de.knowwe.ontology.turtle;

import java.util.regex.Pattern;

import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.XSD;

import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.RegexSectionFinder;
import de.knowwe.core.utils.Patterns;
import de.knowwe.kdom.renderer.StyleRenderer;
import de.knowwe.ontology.turtle.compile.NodeProvider;
import de.knowwe.rdf2go.Rdf2GoCompiler;
import de.knowwe.rdf2go.Rdf2GoCore;

public class TurtleLiteralType extends AbstractType implements NodeProvider<TurtleLiteralType> {

	public static final String XSD_PATTERN = "(?:\\^\\^xsd:(\\w+))";
	public static final String TRIPLE_QUOTED = "(?:\"\"\"(?:(?!\"\"\")\\w|\\W)+\"\"\")";
	public static final String LANGUAGE_TAG = "(?:@\\w+)";
	public static final String LITERAL_SUFFIX = "(?:" + LANGUAGE_TAG + "|" + XSD_PATTERN + ")";

	/**
	 * Either single quoted word and optionally xsd type or normal quote and mandatory xsd type.
	 */
	private static final String LITERAL_PATTERN = TRIPLE_QUOTED + LANGUAGE_TAG + "?|"
			+ Patterns.SINGLE_QUOTED + LITERAL_SUFFIX + "?|" + Patterns.QUOTED + LITERAL_SUFFIX + "?";

	public TurtleLiteralType() {
		this.setSectionFinder(new RegexSectionFinder(
				LITERAL_PATTERN));
		this.setRenderer(StyleRenderer.CONTENT);
		this.addChildType(new LiteralPart());
		this.addChildType(new XSDPart());
		this.addChildType(new LanguageTagPart());
	}

	public Literal getLiteral(Rdf2GoCore core, Section<TurtleLiteralType> section) {
		Section<LiteralPart> literalPartSection = Sections.child(section,
				LiteralPart.class);
		Section<XSDPart> xsdPartSection = Sections.child(section, XSDPart.class);
		Section<LanguageTagPart> langTagPartSection = Sections.child(section,
				LanguageTagPart.class);
		String literal = literalPartSection.get()
				.getLiteral(literalPartSection);
		if (langTagPartSection != null) {
			return core.createLanguageTaggedLiteral(literal,
					langTagPartSection.get().getTag(langTagPartSection));
		}
		URI xsdType = null;
		if (xsdPartSection != null) {
			xsdType = xsdPartSection.get().getXSDType(xsdPartSection);
		}
		if (xsdType == null) {
			// sectionizer takes care that this has to be plain type
			return core.createLiteral(literal);
		}
		return core.createLiteral(literal, xsdType);
	}

	private static class LiteralPart extends AbstractType {

		public LiteralPart() {
			this.setSectionFinder(new RegexSectionFinder(TRIPLE_QUOTED + "|" + Patterns.SINGLE_QUOTED + "|" + Patterns.QUOTED));
		}

		public String getLiteral(Section<LiteralPart> section) {
			return Rdf2GoCore.unquoteTurtleLiteral(section.getText());
		}
	}

	private static class LanguageTagPart extends AbstractType {

		public LanguageTagPart() {
			this.setSectionFinder(new RegexSectionFinder(LANGUAGE_TAG));
		}

		public String getTag(Section<LanguageTagPart> section) {
			return section.getText().substring(1);
		}
	}

	private static class XSDPart extends AbstractType {

		public XSDPart() {
			this.setSectionFinder(new RegexSectionFinder(Pattern.compile(XSD_PATTERN), 1));
		}

		public URI getXSDType(Section<XSDPart> section) {
			return new URIImpl(XSD.XSD_NS + section.getText(), false);
		}
	}

	@Override
	public Node getNode(Section<TurtleLiteralType> section, Rdf2GoCompiler core) {
		return getLiteral(core.getRdf2GoCore(), section);
	}

}