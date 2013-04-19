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

package de.d3web.we.basic;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.d3web.core.io.PersistenceManager;
import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.strings.Strings;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.Article;
import de.knowwe.knowRep.KnowledgeRepresentationHandler;

/**
 * D3webKnowledgeHandler. Handles Knowledge and its recycling.
 * 
 * @author astriffler
 */
public class D3webKnowledgeHandler implements KnowledgeRepresentationHandler {

	private String web;

	/**
	 * Map for all articles and their KBMs.
	 */
	private static Map<String, KnowledgeBase> kbs = Collections.synchronizedMap(new HashMap<String, KnowledgeBase>());

	/**
	 * Map to store the last version of the KnowledgeBase.
	 */
	private static Map<String, KnowledgeBase> lastKB = Collections.synchronizedMap(new HashMap<String, KnowledgeBase>());

	/**
	 * Stores for each Article if the jar file already got built
	 */
	// We no longer can cache knowledge bases, because attachments added as
	// resources could change without anyone knowing. As long as we do not
	// get any notification from JSPWiki that attachments have changed, we
	// need to create a new knowledge base every time.
	// private static Map<String, Boolean> savedToJar = new HashMap<String,
	// Boolean>();

	/**
	 * <b>This constructor SHOULD NOT BE USED!</b>
	 * <p/>
	 * Use D3webModule.getInstance().getKnowledgeRepresentationHandler(String
	 * web) instead!
	 */
	public D3webKnowledgeHandler(String web) {
		this.web = web;
	}

	public D3webKnowledgeHandler() {
		this.web = Environment.DEFAULT_WEB;
	}

	/**
	 * @returns the KB for the given article
	 */
	public KnowledgeBase getKnowledgeBase(String title) {
		KnowledgeBase kb = kbs.get(title);
		if (kb == null) {
			kb = KnowledgeBaseUtils.createKnowledgeBase();
			kb.setId(title);
			kbs.put(title, kb);
		}
		return kb;
	}

	public void clearKnowledgeBase(String title) {
		kbs.remove(title);
	}

	/**
	 * Returns all topics of this web that own a compiled d3web knowledge base.
	 * 
	 * @created 14.10.2010
	 * @return all topics with a compiled d3web knowledge base
	 */
	public Set<String> getKnowledgeArticles() {
		/*
		 * Iterators are not automatically synchronized in synchronized
		 * collections. Since the iterator of the key set is needed when adding
		 * it to a new set, we synchronize it manually on the same mutex as the
		 * map uses.
		 */
		synchronized (kbs) {
			return new TreeSet<String>(kbs.keySet());
		}
	}

	/**
	 * This gets called when an new Article or a new version of an Article gets
	 * build. Prepares it for new d3web knowledge.
	 */
	@Override
	public void initArticle(Article art) {
		if (!art.isSecondBuild()) {
			lastKB.put(art.getTitle(), getKnowledgeBase(art.getTitle()));
		}
		if (art.isFullParse()) {
			clearKnowledgeBase(art.getTitle());
		}
		// savedToJar.put(art.getTitle(), false);
	}

	@Override
	public URL saveKnowledge(String title) throws IOException {
		KnowledgeBase base = getKnowledgeBase(title);
		URL home = D3webUtils.getKnowledgeBaseURL(web, base.getId());
		// We no longer can cache knowledge bases, because attachments added as
		// resources could change without anyone knowing. As long as we do not
		// get any notification from JSPWiki that attachments have changed, we
		// need to create a new knowledge base every time.
		// if (!savedToJar.get(title)) {
		PersistenceManager.getInstance().save(base,
				new File(Strings.decodeURL(home.getFile())));
		// savedToJar.put(title, true);
		// }
		return home;
	}

	/**
	 * Sets a knowledgebase at the specified article
	 * 
	 * @created 6/08/2012
	 * @param title title of the article
	 */
	public void setKnowledgeBase(String title, KnowledgeBase kb) {
		kbs.put(title, kb);

	}

	@Override
	public String getKey() {
		return "d3web";
	}

	@Override
	public void setWeb(String web) {
		this.web = web;
	}

}
