/*
 * Copyright (C) ${year} denkbares GmbH, Germany
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

package de.d3web.we.knowledgebase;

import de.knowwe.core.compile.packaging.DefaultMarkupPackageCompileTypeRenderer;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.user.UserContext;

/**
 * Renders a knowledge base markup into the wiki page.
 * 
 * @author Volker Belly (denkbares GmbH)
 * @created 13.10.2010
 */
public final class KnowledgeBaseTypeRenderer extends DefaultMarkupPackageCompileTypeRenderer {

	public KnowledgeBaseTypeRenderer() {
		super("KnowWEExtension/d3web/icon/knowledgebase24.png");
	}

	@Override
	protected void renderContents(Section<?> section, UserContext user, RenderResult string) {
		String title = KnowledgeBaseType.getContent(section).trim();
		String id = KnowledgeBaseType.getAnnotation(section, KnowledgeBaseType.ANNOTATION_ID);
		String author = KnowledgeBaseType.getAnnotation(section,
				KnowledgeBaseType.ANNOTATION_AUTHOR);
		String comment = KnowledgeBaseType.getAnnotation(section,
				KnowledgeBaseType.ANNOTATION_COMMENT);
		String version = KnowledgeBaseType.getAnnotation(section,
				KnowledgeBaseType.ANNOTATION_VERSION);
		String filename = KnowledgeBaseType.getAnnotation(section,
				KnowledgeBaseType.ANNOTATION_FILENAME);

		// render title line
		string.appendHtml("<b>" + title + "</b>");
		if (id != null) {
			string.append(" (").append(id).append(")");
		}
		string.append("\n");

		// render information block
		if (version != null || author != null || comment != null || filename != null) {
			string.appendHtml("<div style='padding-top:1em;'>");

			if (version != null) {
				string.appendHtml("<img src='KnowWEExtension/d3web/icon/date16.png' /> ");
				string.append(version).append("\n");
			}
			if (author != null) {
				string.appendHtml("<img src='KnowWEExtension/d3web/icon/author16.png' /> ");
				string.append(author).append("\n");
			}
			if (comment != null) {
				string.appendHtml("<img src='KnowWEExtension/d3web/icon/comment16.png' /> ");
				string.append(comment).append("\n");
			}
			if (filename != null) {
				string.appendHtml("<img src='KnowWEExtension/d3web/icon/download16.gif' /> ");
				string.append(filename).append("\n");
			}

			string.appendHtml("</div>");
		}

		super.renderContents(section, user, string);

	}

}