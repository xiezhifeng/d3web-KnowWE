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

package de.d3web.we.ci4ke.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.testing.BuildResult;
import de.d3web.testing.Message.Type;
import de.d3web.testing.TestExecutor;
import de.d3web.we.ci4ke.build.CIBuildRenderer;
import de.d3web.we.ci4ke.build.Dashboard;
import de.d3web.we.ci4ke.handling.CIDashboardType;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.core.utils.Strings;
import de.knowwe.core.wikiConnector.WikiConnector;

public class CIUtilities {

	private static Map<String, TestExecutor> runningBuilds = new HashMap<String, TestExecutor>();



	/**
	 * Registers a running build for a specific dashboard.
	 * 
	 * @created 13.08.2012
	 * @param dashBoardID
	 * @param th
	 */
	public static void registerBuildExecutor(String dashBoardID, TestExecutor th) {
		runningBuilds.put(dashBoardID, th);
	}

	/**
	 * Removes a running build process for a specific dashboard.
	 * If it is still alive it will stop (interrupted and after time-out by force).
	 * 
	 * @created 13.08.2012
	 * @param dashBoardID
	 * @param user
	 */
	public static void removeBuildThread(String dashBoardID) {
		TestExecutor executor = runningBuilds.get(dashBoardID);
		if (executor != null) {
			executor.terminate();
			// finally remove executor from register
			runningBuilds.remove(dashBoardID);
			System.out.println("build thread removed");
		}
	}

	/**
	 * Returns the savepath for ci-builds. If the path does not exist, it will
	 * instantly be created!
	 * 
	 * @return the save path for ci-build xml´s
	 */
	public static File getCIBuildDir() {
		String wikiDir = getWikiContentDirectory();
		File buildDir = new File(wikiDir + "ci-builds");
		// check if the path exists
		if (!buildDir.exists()) {
			buildDir.mkdirs();
		}
		return buildDir;
	}

	/**
	 * 
	 * @created 10.11.2010
	 * @return
	 */
	private static String getWikiContentDirectory() {
		WikiConnector con = Environment.getInstance().getWikiConnector();
		String wikiDir = con.getSavePath();
		if (wikiDir == null || wikiDir.isEmpty()) {
			Logger.getLogger(CIUtilities.class.getName()).log(
					Level.WARNING, "Wiki SavePath could not be retrieved! (null or empty!)");
		}

		if (!wikiDir.endsWith(File.separator)) {
			wikiDir = wikiDir + File.separator;
		}
		return wikiDir;
	}

	/**
	 * Returns the attachment-directory for a specific article.
	 * 
	 * @created 10.11.2010
	 * @param articleTitle
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static File getAttachmentsDirectory(String articleTitle) {
		String wikiDir = getWikiContentDirectory();
		String folderName = Strings.encodeURL(articleTitle) + "-att" + File.separator;
		File attachmentsDir = new File(wikiDir + folderName);
		if (!attachmentsDir.exists()) {
			attachmentsDir.mkdirs();
		}
		return attachmentsDir;
	}

	/**
	 * This method finds a CIDashboard section only by its dashboard ID, by
	 * iterating over all wiki articles.
	 * 
	 * @param dashboardName the dashboard ID to look for
	 * @return the section where the dashboard with the given ID is defined, or
	 *         null if no section with this ID can be found
	 */
	public static Collection<Section<CIDashboardType>> findCIDashboardSection(String dashboardName) {
		List<Section<CIDashboardType>> found = new ArrayList<Section<CIDashboardType>>();
		for (Article article : Environment.getInstance().
				getArticleManager(Environment.DEFAULT_WEB).getArticles()) {

			List<Section<CIDashboardType>> list = new ArrayList<Section<CIDashboardType>>();

			Sections.findSuccessorsOfType(article.getRootSection(), CIDashboardType.class, list);

			for (Section<CIDashboardType> sec : list) {
				if (CIDashboardType.getAnnotation(sec, CIDashboardType.NAME_KEY).equals(
						dashboardName)) {
					found.add(sec);
				}
			}
		}
		return found;
	}

	/**
	 * This method finds a CIDashboard section by its dashboard ID, searching on
	 * the article with the given title.
	 * 
	 * @param dashboardArticleTitle
	 * @param dashboardName
	 * @return
	 */
	public static Section<CIDashboardType> findCIDashboardSection(
			String dashboardArticleTitle, String dashboardName) {
		// get the article
		Article article = Environment.getInstance().
				getArticleManager(Environment.DEFAULT_WEB).
				getArticle(dashboardArticleTitle);
		// get all CIDashboardType-sections on this article
		List<Section<CIDashboardType>> list = new ArrayList<Section<CIDashboardType>>();
		Sections.findSuccessorsOfType(article.getRootSection(), CIDashboardType.class, list);
		// iterate all sections and look for the given dashboard ID
		for (Section<CIDashboardType> sec : list) {
			String otherDashName = CIDashboardType.getAnnotation(sec, CIDashboardType.NAME_KEY);
			if (otherDashName != null && otherDashName.equals(dashboardName)) {
				return sec;
			}
		}
		return null;
	}

	// RENDER - HELPERS

	public static String renderResultType(Type resultType, int pixelSize, String dashboardName) {

		String imageURL = "KnowWEExtension/ci4ke/images/"
				+
				pixelSize + "x" + pixelSize
				+ "/%s.png";

		String imgBulb = "<img width='" + pixelSize + "'id='state_" + dashboardName
				+ "' src='" + imageURL + "' alt='%<s' align='absmiddle' title='%s'>";

		switch (resultType) {
		case SUCCESS:
			imgBulb = String.format(imgBulb, "green", "Build successful!");
		case FAILURE:
			imgBulb = String.format(imgBulb, "red", "Build failed!");
		case ERROR:
			imgBulb = String.format(imgBulb, "grey", "Build has errors!");
		}

		return imgBulb;
	}

	public static String renderHeaderResultType(Type resultType, int pixelSize, String dashboardName) {

		if (CIUtilities.buildRunning(dashboardName)) {
			// if currently a build is running show animated icon
			String imageURL = "KnowWEExtension/ci4ke/images/16x16/yellow_anime.gif";
			String imgBulb = "<img width='" + pixelSize + "'id='state_" + dashboardName
					+ "' src='" + imageURL + "' alt='%<s' align='absmiddle' title='%s'>";
			return imgBulb;
		}

		return renderResultType(resultType, pixelSize, dashboardName);
	}

	public static String renderDashboardHeader(Dashboard dashboard, BuildResult latestBuild) {
		StringBuilder string = new StringBuilder();
		String dashboardName = dashboard.getDashboardName();

		string.append("<h3>");

		if (latestBuild != null || CIUtilities.buildRunning(dashboardName)) {
			CIBuildRenderer renderer = dashboard.getRenderer();
			string.append(renderer.renderCurrentBuildStatus(22)).append("  ");
			string.append(renderer.renderBuildHealthReport(22)).append("  ");
		}
		string.append(dashboardName);

		// insert tag for progress bar
		// open/close div progress_container
		if (CIUtilities.buildRunning(dashboardName)) {
			string.append("<div class='progressInfo' dashboardname='" + dashboardName
					+ "' dashboardarticle='" + dashboard.getDashboardArticle()
					+ "' onload=\"refreshBuildProgress('"
					+ dashboardName
					+ "', '" + dashboard.getDashboardArticle()
					+ "');\" id='progress_container' style='display:inline;'>");

			string.append("<div style='display:inline;'> <a href=\"javascript:stopRunningBuild('"
					+ dashboardName
					+ "', '"
					+ dashboard.getDashboardArticle()
					+ "', '"
					+ KnowWEUtils.getURLLink(dashboard.getDashboardArticle() + "#" + dashboardName)
					+ "');undefined;\"><img height=\"14\" title=\"Stops the current build\" src=\"KnowWEExtension/images/cross.png\"></img></a></div>     <div style='display:inline' class='prog-meter-wrap' ><div class='prog-meter-value' id='progress_value'>&nbsp;0 %</div>  </div><div class='prog-meter-text' style='display:inline' id='progress_text'>build running...</div>");
			string.append("</div>");
		}
		string.append("</h3>");
		return string.toString();
	}

	public static String renderForecastIcon(int buildCount, int failedCount, int pixelSize) {

		int score = (buildCount > 0) ? score = (100 * (buildCount - failedCount)) / buildCount : 0;
		String imgForecast = "<img src='KnowWEExtension/ci4ke/images/" +
				pixelSize + "x" + pixelSize + "/%s.png' align='absmiddle' alt='%<s' title='%s'>";

		if (score == 0) {
			imgForecast = String.format(imgForecast, "health-00to19",
					"All recent builds failed.");
		}
		else if (score == 100) {
			imgForecast = String.format(imgForecast, "health-80plus",
					"No recent builds failed.");
		}
		else {
			String summary =
					failedCount + " out of the last " + buildCount + " builds failed.";
			if (score <= 20) {
				imgForecast = String.format(imgForecast, "health-00to19", summary);
			}
			else if (score <= 40) {
				imgForecast = String.format(imgForecast, "health-20to39", summary);
			}
			else if (score <= 60) {
				imgForecast = String.format(imgForecast, "health-40to59", summary);
			}
			else if (score <= 80) {
				imgForecast = String.format(imgForecast, "health-60to79", summary);
			}
			else {
				imgForecast = String.format(imgForecast, "health-80plus", summary);
			}
		}

		return imgForecast;
	}

	/**
	 * Replaces the relevant entities inside the String. All &amp; &gt;, &lt;,
	 * and &quot; are replaced by their respective names.
	 * 
	 * @param src The source string.
	 * @return The encoded string.
	 */
	public static String replaceEntities(String src) {
		src = replaceString(src, "&", "&amp;");
		src = replaceString(src, "<", "&lt;");
		src = replaceString(src, ">", "&gt;");
		src = replaceString(src, "\"", "&quot;");

		return src;
	}

	/**
	 * Replaces a string with an other string.
	 * 
	 * @param orig Original string. Null is safe.
	 * @param src The string to find.
	 * @param dest The string to replace <I>src</I> with.
	 * @return A string with the replacement done.
	 */
	public static final String replaceString(String orig, String src, String dest) {
		if (orig == null) {
			return null;
		}
		if (src == null || dest == null) {
			throw new NullPointerException();
		}
		if (src.length() == 0) {
			return orig;
		}

		StringBuffer res = new StringBuffer(orig.length() + 20); // Pure
		// guesswork
		int start = 0;
		int end = 0;
		int last = 0;

		while ((start = orig.indexOf(src, end)) != -1) {
			res.append(orig.substring(last, start));
			res.append(dest);
			end = start + src.length();
			last = start + src.length();
		}

		res.append(orig.substring(end));

		return res.toString();
	}

	/**
	 * Escapes a string using UTF-8
	 * 
	 * @created 01.12.2010
	 * @param toEscape
	 * @return
	 */
	public static String utf8Escape(String toEscape) {
		return Strings.encodeURL(toEscape);
	}

	/**
	 * Looks up whether there is currently a build process running for this
	 * dashboard
	 * 
	 * @created 16.08.2012
	 * @param dashboardName
	 * @return
	 */
	public static boolean buildRunning(String dashboardName) {
		return runningBuilds.get(dashboardName) != null;
	}
}
