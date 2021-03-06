/*
 * Copyright (C) 2014 denkbares GmbH, Germany
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

package de.knowwe.core.tools;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.user.UserContext;
import de.knowwe.tools.DefaultTool;
import de.knowwe.tools.Tool;
import de.knowwe.tools.ToolProvider;
import de.knowwe.util.Icon;

/**
 * ToolProvider for deactivating a markup
 *
 * @author Veronika Sehne (denkbares GmbH)
 * @created 11.06.2014
 */
public class OffToolProvider implements ToolProvider {

	@Override
	public Tool[] getTools(Section<?> section, UserContext userContext) {

		String js = "KNOWWE.core.plugin.setMarkupSectionActivationStatus('" + section.getID() + "', 'off')";
		Tool help = new DefaultTool(
				Icon.TOGGLE_OFF,
				"Deactivate",
				"Deactivates this section.",
				js,
				Tool.CATEGORY_EDIT);
		return new Tool[] { help };
	}

	@Override
	public boolean hasTools(Section<?> section, UserContext userContext) {
		return true;
	}

}
