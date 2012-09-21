/*
 * Copyright (C) 2012 denkbares GmbH, Germany
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
package de.d3web.we.ci4ke.build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.testing.BuildResult;
import de.d3web.we.ci4ke.handling.CIDashboardType;
import de.knowwe.core.ArticleManager;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;

/**
 * This class represents a dashboard data structure, managing the build results,
 * their persistence and provides an appropriate renderer.
 * 
 * @author volker_belli
 * @created 19.05.2012
 */
public class CIDashboard {

	private final String web;
	private final String dashboardArticle;
	private final String dashboardName;
	private final CIRenderer renderer;
	private final CIPersistence persistence;

	private CIDashboard(String web, String dashboardArticle, String dashboardName) {
		this.web = web;
		this.dashboardArticle = dashboardArticle;
		this.dashboardName = dashboardName;
		this.renderer = new CIRenderer(this);
		this.persistence = new CIPersistence(this);
	}

	public String getWeb() {
		return web;
	}

	public String getDashboardArticle() {
		return dashboardArticle;
	}

	public String getDashboardName() {
		return dashboardName;
	}

	public CIRenderer getRenderer() {
		return renderer;
	}

	public CIPersistence getPersistence() {
		return persistence;
	}

	/**
	 * Returns the latest build stored in this dashboard or null if there is no
	 * build stored.
	 * 
	 * @created 19.05.2012
	 * @return the latest build
	 */
	public BuildResult getLatestBuild() {
		int latest = persistence.getLatestBuildVersion();
		if (latest == 0) return null;
		return getBuildIfPossible(latest, true);
	}

	/**
	 * Returns the build of the specified buildNumber stored in this dashboard
	 * or null if this build does not exist.
	 * 
	 * @created 19.05.2012
	 * @param buildNumber the build to be returned
	 * @return the specified build
	 */
	public BuildResult getBuild(int buildNumber) {
		return getBuildIfPossible(buildNumber, true);
	}

	/**
	 * Returns an array of builds that have the specified index in the list of
	 * versions available in this dashboard. The first build to be accessed
	 * starts at fromIndex, the latest build is before toIndex (exclusively). If
	 * the specified indexes exceed the list of history, the array contains less
	 * than "toIndex - fromIndex" values. If there is no build between the
	 * indexes, an empty array is returned.
	 * 
	 * @created 19.05.2012
	 * @param buildNumber the build to be returned
	 * @return the specified build
	 */
	public List<BuildResult> getBuildsByIndex(int fromIndex, int numberOfBuilds) {
		fromIndex = cap(fromIndex);
		List<BuildResult> results = new ArrayList<BuildResult>(numberOfBuilds);
		int index = fromIndex;
		while (results.size() < numberOfBuilds && index > 0) {
			BuildResult build = getBuildIfPossible(index, false);
			if (build != null && build.getBuildNumber() <= fromIndex) {
				results.add(build);
			}
			index--;
		}
		return results;
	}

	/**
	 * If the buildNumber is to high, the highest available buildNumber is
	 * returned;
	 * 
	 * @created 18.09.2012
	 * @return the highest available build number if the given is higher that
	 *         that
	 */
	private int cap(int buildIndex) {
		BuildResult latestBuild = getLatestBuild();
		if (latestBuild == null) return 0;
		int latestBuildNumber = latestBuild.getBuildNumber();
		if (latestBuildNumber < buildIndex) return latestBuildNumber;
		return buildIndex;
	}

	/**
	 * Adds a new build to the dashboard and makes the underlying persistence to
	 * store that build. If the build is lower or equal than the latest build
	 * number, an {@link IllegalArgumentException} is thrown.
	 * 
	 * @created 19.05.2012
	 * @param build the build to be added
	 * @throws IllegalArgumentException the build number is not higher than the
	 *         existing ones
	 */
	public synchronized void addBuild(BuildResult build) throws IllegalArgumentException {
		if (build == null) throw new IllegalArgumentException("build is null!");

		// attach to wiki if possible
		try {
			persistence.write(build);
		}
		catch (IOException e) {
			// we cannot store the build as attachment
			// so log this and continue as usual
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"cannot attached build information due to internal error", e);
		}
	}

	private BuildResult getBuildIfPossible(int buildVersion, boolean logging) {
		BuildResult result = null;
		try {
			result = persistence.read(buildVersion);
		}
		catch (Exception e) {
			if (logging) {
				Logger.getLogger(getClass().getName()).warning(
						"unable to access build " + buildVersion);
			}
		}
		return result;
	}

	private static final Map<String, CIDashboard> dashboards = new HashMap<String, CIDashboard>();

	/**
	 * Get the {@link CIDashboard} instance responsible for a specific
	 * dashboardName-dashboardArticle-combination. If no handler exists for this
	 * combination, a new handler is created.
	 */
	public static synchronized CIDashboard getDashboard(String web, String dashboardArticleTitle, String dashboardName) {
		String key = web + "/" + dashboardArticleTitle + "/" + dashboardName;
		CIDashboard dashboard = dashboards.get(key);
		if (dashboard == null) {
			dashboard = new CIDashboard(web, dashboardArticleTitle, dashboardName);
			dashboards.put(key, dashboard);
		}
		return dashboard;
	}

	/**
	 * Checks if there is a {@link CIDashboard} instance responsible for a
	 * specific dashboardName-dashboardArticle-combination. If no dashboard
	 * exists for this combination, false is returned.
	 * 
	 * @param dashboardArticleTitle the article where the dashboard is located
	 * @param dashboardName the name of the dashboard
	 */
	public static boolean hasDashboard(String web, String dashboardArticleTitle, String dashboardName) {
		ArticleManager articleManager = Environment.getInstance().getArticleManager(web);
		Article article = articleManager.getArticle(dashboardArticleTitle);
		if (article == null) {
			return false;
		}
		List<Section<CIDashboardType>> sections =
				Sections.findSuccessorsOfType(article.getRootSection(), CIDashboardType.class);
		for (Section<CIDashboardType> section : sections) {
			String name = CIDashboardType.getDashboardName(section);
			if (name != null && name.equalsIgnoreCase(dashboardName)) {
				return true;
			}
		}
		return false;
	}

}