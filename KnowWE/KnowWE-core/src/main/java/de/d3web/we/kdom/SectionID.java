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

package de.d3web.we.kdom;

/**
 * This class generates ids for Sections.
 * 
 * @author astriffler
 * 
 */
public class SectionID {

	private String id;

	private String specificID;

	public static final String SEPARATOR = "/";

	/**
	 * This Constructor should be used for assigning <b>nonspecific</b> IDs
	 */
	public SectionID(Section<? extends Type> father, Type type) {
		String typename;

		if (type instanceof SectionIDDeclarant) {
			typename = ((SectionIDDeclarant) type).createSectionID(father);
		}
		else {
			typename = type.getName();
		}
		createID(father.getArticle(), father.getID() + SEPARATOR + typename);
	}

	/**
	 * This Constructor should be used for assigning <b>nonspecific</b> IDs
	 */
	public SectionID(Section<? extends Type> father, String id) {
		createID(father.getArticle(), father.getID() + SEPARATOR + id);
	}

	/**
	 * This Constructor should be used for assigning <b>specific</b> IDs
	 */
	public SectionID(KnowWEArticle article, String id) {
		this.specificID = id;
		createID(article, escapeWeirdChars(article.getTitle()) + SEPARATOR + id);
	}

	/**
	 * Escapes all non-usual characters of the page title into normal
	 * alphanumeric characters.
	 * 
	 * @created 08.12.2010
	 * @param text the text to be escaped
	 * @return the escaped text
	 */
	private String escapeWeirdChars(String text) {
		StringBuilder newText = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_' || c == ' ' || c == '-') {
				newText.append(c);
			}
			else {
				newText.append('_').append(Integer.toHexString(c)).append('_');
			}
		}
		return newText.toString();
	}

	/**
	 * THIS SHOULD ONLY BE USED FOR THE ROOT SECTION OF THE ARTICLE!
	 */
	protected SectionID(String title) {
		this.id = escapeWeirdChars(title);
	}

	private void createID(KnowWEArticle article, String lid) {
		int idNum = article.checkID(lid);
		if (idNum > 1) {
			lid = lid + idNum;
		}
		this.id = lid;
	}

	public String getID() {
		return this.id;
	}

	/**
	 * This returns the part of the ID, that was specifically given for this ID
	 * to be used instead of the name of the ObjectType.
	 */
	public String getSpecificID() {
		return this.specificID;
	}

	public static String getArticleNameFromID(String kdomID) {
		int pos = kdomID.indexOf(SEPARATOR);
		if (pos == -1) return kdomID;
		return kdomID.substring(0, pos);
	}

	@Override
	public String toString() {
		return getID();
	}
}
