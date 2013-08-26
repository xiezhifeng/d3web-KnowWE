/*
 * Copyright (C) 2012 denkbares GmbH
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

package de.knowwe.core.utils.progress;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.knowwe.core.Attributes;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;

/**
 * This action handles the ajax upate request of the ci-build progress bar on
 * the dashboard.
 * 
 * @author Jochen Reutelshöfer (denkbares GmbH)
 * @created 18.07.2012
 */
public class GetProgressAction extends AbstractAction {

	@Override
	public void execute(UserActionContext context) throws IOException {

		String sectionID = context.getParameter(Attributes.SECTION_ID);
		Section<?> section = Sections.getSection(sectionID);
		if (section == null) {
			context.sendError(404, "no such section");
			return;
		}

		try {
			JSONArray result = new JSONArray();
			for (LongOperation operation : LongOperationUtils.getLongOperations(section)) {
				AjaxProgressListener listener =
						ProgressListenerManager.getInstance().getProgressListener(operation);
				if (listener == null) continue;

				JSONObject progress = new JSONObject();
				progress.put("operationID",
						LongOperationUtils.getRegistrationID(section, operation));
				progress.put("progress", listener.getCurrentProgress());
				progress.put("message", listener.getCurrentMessage());
				progress.put("error", listener.getError());
				progress.put("running", listener.isRunning());
				UserContext user = listener.getUserContext();
				if (user != null) progress.put("user", user.getUserName());
				result.put(progress);
			}
			result.write(context.getWriter());
		}
		catch (JSONException e) {
			java.util.logging.Logger.getLogger(this.getClass().getName()).severe(
					"Error while writing JSON message: " + e.getMessage());
		}
	}

}