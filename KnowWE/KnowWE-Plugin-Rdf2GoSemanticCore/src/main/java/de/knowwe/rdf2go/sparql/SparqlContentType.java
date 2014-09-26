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
package de.knowwe.rdf2go.sparql;

import java.util.concurrent.TimeUnit;

import org.ontoware.aifbcommons.collection.ClosableIterable;
import org.ontoware.rdf2go.model.Statement;

import de.knowwe.core.compile.DefaultGlobalCompiler;
import de.knowwe.core.compile.DefaultGlobalCompiler.DefaultGlobalScript;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.AllTextFinder;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.kdom.renderer.AsynchronRenderer;
import de.knowwe.rdf2go.Rdf2GoCore;
import de.knowwe.rdf2go.sparql.utils.RenderOptions;
import de.knowwe.rdf2go.utils.Rdf2GoUtils;

public class SparqlContentType extends AbstractType implements SparqlType {

	public SparqlContentType() {
		this.setSectionFinder(AllTextFinder.getInstance());
		this.setRenderer(new AsynchronRenderer(new SparqlContentRenderer()));
		this.addCompileScript(new SparqlConstructHandler());
	}

	@Override
	public String getSparqlQuery(Section<? extends SparqlType> section, UserContext context) {
		Section<SparqlMarkupType> markupSection = Sections.ancestor(section, SparqlMarkupType.class);
		Rdf2GoCore core = Rdf2GoUtils.getRdf2GoCore(markupSection);
		return Rdf2GoUtils.createSparqlString(core, section.getText());
	}

	@Override
	public RenderOptions getRenderOptions(Section<? extends SparqlType> section, UserContext context) {
		Section<SparqlMarkupType> markupSection = Sections.ancestor(section, SparqlMarkupType.class);

		RenderOptions renderOpts = new RenderOptions(section.getID(), context);

		Rdf2GoCore core = Rdf2GoUtils.getRdf2GoCore(markupSection);
		renderOpts.setRdf2GoCore(core);
		setRenderOptions(markupSection, renderOpts);

		return renderOpts;
	}


	private void setRenderOptions(Section<SparqlMarkupType> markupSection, RenderOptions renderOpts) {
		renderOpts.setRawOutput(checkAnnotation(markupSection, SparqlMarkupType.RAW_OUTPUT));
		renderOpts.setSorting(checkSortingAnnotation(markupSection,
				SparqlMarkupType.SORTING));
		renderOpts.setZebraMode(checkAnnotation(markupSection, SparqlMarkupType.ZEBRAMODE, true));
		renderOpts.setTree(Boolean.valueOf(DefaultMarkupType.getAnnotation(markupSection,
				SparqlMarkupType.TREE)));
		renderOpts.setBorder(checkAnnotation(markupSection, SparqlMarkupType.BORDER, true));
		renderOpts.setNavigation(checkAnnotation(markupSection, SparqlMarkupType.NAVIGATION));

		renderOpts.setTimeout(getTimeout(markupSection));
	}

	private boolean checkSortingAnnotation(Section<SparqlMarkupType> markupSection, String sorting) {
		String annotationString = DefaultMarkupType.getAnnotation(markupSection,
				sorting);
		return annotationString == null || annotationString.equals("true");
	}

	private boolean checkAnnotation(Section<?> markupSection, String annotationName, boolean defaultValue) {
		String annotationString = DefaultMarkupType.getAnnotation(markupSection,
				annotationName);
		return annotationString == null ? defaultValue : annotationString.equals("true");
	}

	private boolean checkAnnotation(Section<?> markupSection, String annotationName) {
		return checkAnnotation(markupSection, annotationName, false);
	}


	private long getTimeout(Section<SparqlMarkupType> markupSection) {
		String timeoutString = DefaultMarkupType.getAnnotation(markupSection, SparqlMarkupType.TIMEOUT);
		long timeOutMillis = Rdf2GoCore.DEFAULT_TIMEOUT;
		if (timeoutString != null) {
			timeOutMillis = (long) (Double.parseDouble(timeoutString) * TimeUnit.SECONDS.toMillis(1));
		}
		return timeOutMillis;
	}


	private class SparqlConstructHandler extends DefaultGlobalScript<SparqlContentType> {

		@Override
		public void compile(DefaultGlobalCompiler compiler, Section<SparqlContentType> section) throws CompilerMessage {
			String sparqlString = section.getText();
			sparqlString = sparqlString.trim();
			sparqlString = sparqlString.replaceAll("\n", "");
			sparqlString = sparqlString.replaceAll("\r", "");
			if (sparqlString.toLowerCase().startsWith("construct")) {
				try {
					ClosableIterable<Statement> sparqlConstruct = Rdf2GoCore.getInstance().sparqlConstruct(
							sparqlString);

					for (Statement aSparqlConstruct : sparqlConstruct) {
						Rdf2GoCore.getInstance().addStatements(section, aSparqlConstruct);
					}
				}
				catch (Exception e) {
					throw CompilerMessage.error(e.getMessage());
				}
			}
		}
	}

}
