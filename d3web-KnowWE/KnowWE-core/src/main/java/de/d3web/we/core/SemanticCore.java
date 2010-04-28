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

package de.d3web.we.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import de.d3web.we.core.semantic.IntermediateOwlObject;
import de.d3web.we.core.semantic.PropertyManager;
import de.d3web.we.core.semantic.UpperOntology;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.Section;
import de.d3web.we.logging.Logging;
import de.d3web.we.wikiConnector.KnowWEWikiConnector;

/**
 * This class provides an interface for semantic interaction with the
 * triplestore. SemanticCore manages namespaces, queries and the addition of
 * statementes. It is a singleton which is initialised at boottime (of KnowWE)
 * See also: {@link UpperOntology} {@link TaggingMangler}
 * 
 * @author FHaupt
 * @created Mar 25, 2010
 */
public class SemanticCore {

	private final UpperOntology uo;
	private final KnowWEEnvironment knowWEEnvironment;
	private static SemanticCore me;
	private static ResourceBundle settingsbundle;
	private final HashMap<String, Resource> contextmap;
	private final HashMap<String, String> settings;
	private final HashMap<String, List<Statement>> statementcache;
	private HashMap<String, String> namespaces;
	private HashMap<String, String> defaultnamespaces;

	private SemanticCore(KnowWEEnvironment ke) {
		this.knowWEEnvironment = ke;
		me = this;
		contextmap = new HashMap<String, Resource>();
		statementcache = new HashMap<String, List<Statement>>();
		String path = ke.getKnowWEExtensionPath();
		settingsbundle = ResourceBundle.getBundle("semanticdefaults");
		settings = new HashMap<String, String>();
		for (String cur : settingsbundle.keySet()) {
			settings.put(cur, settingsbundle.getString(cur));
		}
		uo = UpperOntology.getInstance(path);
		initnamespaces();
		readSettings();
		readIncludings();
	}

	private void initnamespaces() {
		namespaces = new HashMap<String, String>();
		defaultnamespaces = new HashMap<String, String>();
		try {
			uo.setLocaleNS(knowWEEnvironment.getWikiConnector().getBaseUrl());
		} catch (RepositoryException e1) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
					e1.getMessage());
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
					"using default");
		}
		defaultnamespaces.put("ns", uo.getBaseNS());
		defaultnamespaces.put("lns", uo.getLocaleNS());
		defaultnamespaces.put("rdf",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		defaultnamespaces.put("w", "http://www.umweltbundesamt.de/wisec#");
		defaultnamespaces.put("owl", "http://www.w3.org/2002/07/owl#");
		defaultnamespaces.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		defaultnamespaces.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	}

	public void addNamespace(String sh, String ns) {
		namespaces.put(sh, ns);
	}

	private void readIncludings() {
		File[] files = getImportList();
		if (files == null)
			return;
		for (File f : files) {
			uo.loadOwlFile(f);
		}
	}

	private void readSettings() {
		PropertyManager pm = PropertyManager.getInstance();
		KnowWEWikiConnector wiki = knowWEEnvironment.getWikiConnector();
		String settingspage = wiki.getArticleSource("SemanticSettings");
		if (settingspage == null) {
			String output = "";
			for (Entry<String, String> cur : settings.entrySet()) {
				output += cur.getKey() + "=" + cur.getValue()
						+ System.getProperty("line.separator")
						+ System.getProperty("line.separator");
			}
			output += "[{KnowWEPlugin OwlImport}]";
			wiki.createWikiPage("SemanticSettings", output, "semanticcore");
		} else {
			for (String piece : settingspage.split(System
					.getProperty("line.separator"))) {
				if (piece.contains("=")) {
					String[] s = piece.split("=");
					try {
						settings.put(s[0].trim(), s[1].trim());
					} catch (IndexOutOfBoundsException e) {
						Logger.getLogger(this.getClass().getName()).log(
								Level.WARNING, e.getMessage());
					}
				}
			}
			Pattern tagPattern = Pattern.compile("(<" + "properties"
					+ "([^>]+?)?>)\\s*(.+?)\\s*(</" + "properties" + ">)",
					Pattern.DOTALL);
			Matcher tagMatcher = tagPattern.matcher(settingspage);
			RepositoryConnection con = uo.getConnection();
			while (tagMatcher.find()) {
				if (tagMatcher.group(3) != null) {
					String propertieslist = tagMatcher.group(3);

					for (String cur : propertieslist.split("\r\n|\r|\n")) {
						String props = cur.trim();
						if (props.length() > 0) {
							addlocalPropertyData(con, pm, props);
						}
					}
				}

			}

		}
		if (settings.get("persistence").equalsIgnoreCase("enabled")) {
			if (uo.validPersistenceDir(settings.get("persistence.dir"))) {
				uo.setPersistenceDir(settings.get("persistence.dir"));
			}
		}

	}

	/**
	 * @param con
	 * @param pm
	 * @param props
	 */
	private void addlocalPropertyData(RepositoryConnection con,
			PropertyManager pm, String props) {
		do {
			IntermediateOwlObject io = UpperOntology.getInstance().getHelper()
					.createlocalProperty(props);
			List<Statement> allStatements = io.getAllStatements();

			statementcache.put("SemanticSettings", allStatements);
			try {
				for (Statement current : allStatements) {
					if (current != null) {
						con.add(current);
						Logger.getLogger(this.getClass().getName()).log(
								Level.INFO,
								"adding Property " + current.toString());
						con.commit();
					}
				}

				con.commit();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (!pm.isValid(props));
	}

	/**
	 * 
	 * @return current settings
	 */
	public HashMap<String, String> getSettings() {
		return settings;
	}

	/**
	 * 
	 * @return an instance, you're in trouble if it hasn't been initialized
	 */
	public static SemanticCore getInstance() {
		return me;
	}

	public static synchronized SemanticCore getInstance(KnowWEEnvironment ke) {
		if (me == null) {
			me = new SemanticCore(ke);
		}
		return me;
	}

	/**
	 * prevent cloning
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * @return an UpperOntology instance
	 */
	public UpperOntology getUpper() {
		return uo;
	}

	/**
	 * adds Statements to the repository
	 * 
	 * @param inputio
	 *            the output of the section
	 * @param sec
	 *            source section
	 */
	public void addStatements(IntermediateOwlObject inputio, Section sec) {
		
		List<Statement> allStatements = inputio.getAllStatements();
		clearContext(sec);
		statementcache.put(sec.getId().hashCode() + "", allStatements);
		Logger.getLogger(this.getClass().getName()).finer(
				"semantic core updating " + sec.getId() + "  "
						+ allStatements.size());

		addStaticStatements(inputio, sec);

	}

	/**
	 * Add static statements to the repository. Static statements are those
	 * statements that are not connected to a specific section and therefore are
	 * not updated during the wiki lifetime.
	 * 
	 * @param inputio
	 *            the statements to be added
	 * @author volker_belli
	 * @date 19.03.2010
	 */
	public void addStaticStatements(IntermediateOwlObject inputio, Section sec) {
		RepositoryConnection con = uo.getConnection();

		try {
			// con.setAutoCommit(false);
			List<Statement> allStatements = inputio.getAllStatements();
			con.add(allStatements, getContext(sec));
			// con.commit();
		} catch (RepositoryException e) {
			Logging.getInstance().severe(e.getMessage());
		}

	}

	/**
	 * Gets a contet for a section. Is used for differentiation of statements in
	 * the triple-store
	 * 
	 * @author FHaupt
	 * @created Mar 25, 2010
	 * @param sec
	 * @return
	 */
	public Resource getContext(Section sec) {
		String name = sec.getId().hashCode() + "";
		Resource context = contextmap.get(name);
		if (context == null) {
			RepositoryConnection con = uo.getConnection();
			context = uo.getHelper().createURI(name);
			contextmap.put(name, context);
		}
		return contextmap.get(name);
	}

	/**
	 * perform a spql tuplequery
	 * 
	 * @param query
	 * @return
	 * @throws QueryEvaluationException
	 */
	public TupleQueryResult tupleQuery(TupleQuery query)
			throws QueryEvaluationException {
		return query.evaluate();
	}

	/**
	 * perform a sparql graphquery
	 * 
	 * @param query
	 * @return
	 * @throws QueryEvaluationException
	 */
	public GraphQueryResult graphQuery(GraphQuery query)
			throws QueryEvaluationException {
		return query.evaluate();
	}

	/**
	 * perform a sparql booleanquery
	 * 
	 * @param query
	 * @return
	 * @throws QueryEvaluationException
	 */
	public boolean booleanQuery(BooleanQuery query)
			throws QueryEvaluationException {
		return query.evaluate();
	}

	/**
	 * @param topic
	 * @return
	 */
	public List<Statement> getTopicStatements(String topic) {
		Section<? extends KnowWEObjectType> rootsection = KnowWEEnvironment
				.getInstance().getArticle(KnowWEEnvironment.DEFAULT_WEB, topic)
				.getSection();
		return getSectionStatements(rootsection);
	}

	/**
	 * recursivley collects all statements saved for a section.
	 * 
	 * @return List of statements.
	 */
	public List<Statement> getSectionStatements(
			Section<? extends KnowWEObjectType> s) {
		List<Statement> allstatements = new ArrayList<Statement>();
		
		String key = s.getId().hashCode() + "";
		if (statementcache.get(key) != null) {
			// add statements of this section
			allstatements.addAll(statementcache.get(key));
		}
		
		// walk over all children
		for (Section<? extends KnowWEObjectType> current : s.getChildren()) {
			//add statemts from the current child
			key = current.getId().hashCode() + "";
			if (statementcache.get(key) != null) {
				// add statements of this section
				allstatements.addAll(statementcache.get(key));
			}
			// collect statements of the the children's descendants
			allstatements.addAll(getSectionStatements(current));
		}

		return allstatements;

	}

	/**
	 * @return
	 */
	public File[] getImportList() {
		String p = knowWEEnvironment.getWikiConnector().getSavePath();
		String inpath = (p != null) ? p : (knowWEEnvironment
				.getKnowWEExtensionPath()
				+ File.separatorChar + "owlincludes");
		File includes = new File(inpath);
		if (includes.exists()) {
			File[] files = includes.listFiles(new FilenameFilter() {

				public boolean accept(File f, String s) {
					return s.endsWith(".owl");
				}
			});
			return files;
		}
		return null;
	}

	/**
	 * @param filename
	 */
	public void removeFile(String filename) {
		RepositoryConnection con = uo.getConnection();
		String p = knowWEEnvironment.getWikiConnector().getSavePath();
		String inpath = (p != null) ? p : (knowWEEnvironment
				.getKnowWEExtensionPath()
				+ File.separatorChar + "owlincludes");
		File includes = new File(inpath);
		File file = new File(includes, filename);
		if (file.canWrite()) {
			file.delete();
			BNode filebnode = con.getValueFactory().createBNode(file.getName());
			clearStatements(filebnode);
		}
	}

	private void clearStatements(BNode key) {
		RepositoryConnection con = uo.getConnection();
		try {
			con.clear(key);
			// con.commit();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * removes all statements produced by a specific section
	 * 
	 * @param sec
	 */
	public void clearContext(Section sec) {
		// RepositoryConnection con = uo.getConnection();
		// try {
		// con.clear(getContext(sec));
		// }
		// catch (RepositoryException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		RepositoryConnection con = uo.getConnection();
		try {
			con.remove(getSectionStatements(sec));
			con.commit();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * removes all statements produced by a specific topic
	 * 
	 * @param sec
	 */
	public void clearContext(KnowWEArticle art) {
		clearContext(art.getSection());
	}


	public String getSparqlNamespaceShorts() {
		StringBuffer buffy = new StringBuffer();

		for (Entry<String, String> cur : namespaces.entrySet()) {
			buffy.append("PREFIX " + cur.getKey() + ": <" + cur.getValue()
					+ "> \n");
		}
		for (Entry<String, String> cur : defaultnamespaces.entrySet()) {
			buffy.append("PREFIX " + cur.getKey() + ": <" + cur.getValue()
					+ "> \n");
		}

		return buffy.toString();
	}

	/**
	 * creates an arraylist of a simple sparql query. the binding given in
	 * targetbinding is rendered into the list, the rest is ignored
	 * 
	 * @param inquery
	 * @param targetbinding
	 * @return
	 */
	public ArrayList<String> simpleQueryToList(String inquery,
			String targetbinding) {
		ArrayList<String> resultlist = new ArrayList<String>();
		String querystring = getSparqlNamespaceShorts();
		querystring = querystring + inquery;
		RepositoryConnection con = UpperOntology.getInstance().getConnection();
		Query query = null;
		try {
			query = con.prepareQuery(QueryLanguage.SPARQL, querystring);
		} catch (RepositoryException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
					e.getMessage());
		} catch (MalformedQueryException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
					e.getMessage());
		}
		TupleQueryResult result = null;
		if (query == null)
			return resultlist;
		try {
			result = ((TupleQuery) query).evaluate();
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
		}

		if (result != null) {
			try {
				while (result.hasNext()) {
					BindingSet b = result.next();
					Binding binding = b.getBinding(targetbinding);
					if (binding == null)
						continue;
					String tag = binding.toString();
					if (tag.split("#").length == 2)
						tag = tag.split("#")[1];
					try {
						tag = URLDecoder.decode(tag, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					if (tag.contains("=")) {
						tag = tag.split("=")[1];
					}
					if (tag.startsWith("\"")) {
						tag = tag.substring(1);
					}
					if (tag.endsWith("\"")) {
						tag = tag.substring(0, tag.length() - 1);
					}
					resultlist.add(tag.trim());
				}
			} catch (QueryEvaluationException e) {
				return resultlist;
			} finally {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					e.printStackTrace();
				}
			}
		}
		return resultlist;

	}

	/**
	 * @return
	 */
	public void writeDump(OutputStream stream) {
		uo.writeDump(stream);
	}

	public HashMap<String, String> getDefaultNameSpaces() {
		return defaultnamespaces;
	}

	public HashMap<String, String> getNameSpaces() {
		return namespaces;
	}

	public String expandNamespace(String ns) {
		for (Entry<String, String> cur : namespaces.entrySet()) {
			if (ns.equals(cur.getKey())) {
				ns = cur.getValue();
				break;
			}
		}
		for (Entry<String, String> cur : defaultnamespaces.entrySet()) {
			if (ns.equals(cur.getKey())) {
				ns = cur.getValue();
				break;
			}
		}

		return ns;
	}

	/**
	 * reduces any namespace to its shortcut
	 * 
	 * @param s
	 * @return
	 */
	public String reduceNamespace(String s) {
		for (Entry<String, String> cur : namespaces.entrySet()) {
			s = s.replaceAll(cur.getValue(), cur.getKey() + ":");
		}
		for (Entry<String, String> cur : defaultnamespaces.entrySet()) {
			s = s.replaceAll(cur.getValue(), cur.getKey() + ":");
		}

		return s;
	}

}