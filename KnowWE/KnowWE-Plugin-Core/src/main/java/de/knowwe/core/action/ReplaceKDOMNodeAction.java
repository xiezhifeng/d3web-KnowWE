/*
 * Copyright (C) 2009-2011 Chair of Artificial Intelligence and Applied
 * Informatics Computer Science VI, University of Wuerzburg
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

package de.knowwe.core.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.knowwe.core.Attributes;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.utils.Strings;

/**
 * This Action replaces a single KDOM node's content. Before performing the
 * change, the users privileges are checked.
 * 
 * <p>
 * Needed Parameters:
 * </p>
 * <ul>
 * <li><tt>{@link Attributes.TARGET}:</tt> The KDOM node of which the content
 * will be replaced</li>
 * <li><tt>{@link KnowWEAtrributes.TEXT}:</tt> The new node content</li>
 * </ul>
 */
public class ReplaceKDOMNodeAction extends AbstractAction {

	private String perform(UserActionContext context) throws IOException {
		String nodeID = context.getParameter(Attributes.TARGET);
		String name = context.getTitle();
		String newText = context.getParameter(Attributes.TEXT);

		// Check for user access
		if (!Environment.getInstance().getWikiConnector().userCanEditArticle(name,
				context.getRequest())) {
			return "perm";
		}

		newText = Strings.decodeURL(newText);

		// Remove any extra whitespace that might have gotten appended by
		// JSPWiki
		newText = newText.replaceAll("\\s*$", "");

		Map<String, String> nodesMap = new HashMap<String, String>();
		nodesMap.put(nodeID, newText);
		Sections.replaceSections(context, nodesMap);

		return "done";
	}

	@Override
	public void execute(UserActionContext context) throws IOException {
		String result = perform(context);
		if (result != null && context.getWriter() != null) {
			if (result.equals("perm")) {
				context.sendError(403, "You do not have the permission to edit this page.");
			}
			else {
				context.setContentType("text/html; charset=UTF-8");
				context.getWriter().write(result);
			}
		}
	}

}
