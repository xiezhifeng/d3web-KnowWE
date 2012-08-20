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
package de.d3web.we.ci4ke.action;

import java.io.IOException;

import de.d3web.core.io.progress.ProgressListenerManager;
import de.d3web.we.ci4ke.util.CIUtilities;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;


public class CIStopBuildAction extends AbstractAction{

	@Override
	public void execute(UserActionContext context) throws IOException {
		System.out.println("stop event");
		String dashboardID = context.getParameter("id");
		CIUtilities.removeBuildThread(dashboardID);
		ProgressListenerManager.getInstance().removeProgressListener(dashboardID);
	}

}