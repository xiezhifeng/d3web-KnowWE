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

package de.knowwe.core.kdom.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;

import com.denkbares.strings.Strings;
import com.denkbares.utils.Log;
import de.knowwe.core.ArticleManager;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.Types;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;

/**
 * <p>
 * This class represents a node in the Knowledge-DOM of KnowWE. Basically it has some text, one type and a list
 * of children.
 * <p>
 * Further, it has a reference to its father and a positionOffset to its fathers text.
 * <p>
 * Further information can be attached to a node (TypeInformation), to connect the text-parts with external
 * resources, e.g. knowledge bases, OWL, User-feedback-DBs etc.
 *
 * @author Jochen
 */
public final class Section<T extends Type> implements Comparable<Section<? extends Type>> {

	/**
	 * Stores Sections by their IDs.
	 */
	private static final Map<Integer, Section<?>> sectionMap = new HashMap<>(2048);

	private List<Integer> position = null;

	private List<Integer> lastPositions = null;

	protected boolean isOrHasReusedSuccessor = false;

	protected Article article;

	private HashSet<String> packageNames = null;

	/**
	 * The unique ID of this Section (or -1, if not initialized).
	 */
	private int intID = -1;

	/**
	 * The text of this section. For memory footprint reasons, this is normally null and the text is instead calculated
	 * via offset from article and text length
	 */
	private String text;

	/**
	 * The length of the section's text, it will be used to calculate the text as soon as asked
	 */
	private int textLength;

	/**
	 * The child-nodes of this KDOM-node. This forms the tree-structure of KDOM.
	 */
	protected ArrayList<Section<? extends Type>> children;

	/**
	 * The father section of this KDOM-node. Used for upwards navigation through the tree
	 */
	private Section<? extends Type> parent;

	/**
	 * the position the text of this node starts related to the text of the parent node. Thus: for first child always
	 * 0,
	 * for 2nd firstChild.length() etc.
	 */
	private int offsetInParent = -1;

	/**
	 * the position the text of this node starts related to the article's text.
	 */
	private int offsetInArticle = -1;

	/**
	 * Contains all the stored objects
	 */
	private Map<Compiler, Map<String, Object>> store = null;

	/**
	 * Type of this node.
	 *
	 * @see Type Each type has its own parser and renderer
	 */
	protected final T type;

	/**
	 * Returns the type instance of this section.
	 *
	 * @return the type instance of this section.
	 */
	public T get() {
		return type;
	}

	/**
	 * Returns the result of the given method of this section's type applied to this section.
	 *
	 * @param method a method of this section's type
	 * @param <R>    the type of the returned object
	 * @return the result of the given method applied to this section
	 */
	public <R> R get(BiFunction<T, Section<T>, R> method) {
		return method.apply(get(), this);
	}

	public static <T extends Type> Section<T> createSection(String text, T o, Section<? extends Type> parent) {
		return new Section<>(text, o, parent);
	}

	/**
	 * Constructor of a node
	 * <p>
	 * Important: parses itself recursively by getting the allowed childrenTypes of the local type
	 *
	 * @param text       the part of (article-source) text of the node
	 * @param objectType type of the node
	 * @param parent     the parent section
	 */
	private Section(String text, T objectType, Section<?> parent) {
		this.parent = parent;
		this.type = objectType;
		this.textLength = text != null ? text.length() : 0;
		if (parent == null) {
			// Should only happen in text scenarios or while initializing... since we don't have an article, we set the
			// text manually. As soon as an article is set, the text is removed again.
			this.text = text;
		}
		else {
			this.parent.addChild(this);
			this.article = parent.getArticle();
		}
	}

	/*
	 * verbalizes this node
	 */
	@Override
	public String toString() {
		String typeString = type != null ? this.get().getClass().getSimpleName() : "Type=null";
		return typeString + " (" + getTitle() + "): '" + getText() + "'";
	}

	/**
	 * If the compared Sections are from different articles, a value less than 0 will be returned, if the title of this
	 * Section is lexicographically less than the title of the arguments Section, greater than 0 if the title of
	 * arguments Section is lexicographically greater.<br/> If the Sections are from the same article, a value less
	 * than 0 will be returned, if the Section is textually located above the argument Section, a value greater than 0,
	 * if below.<br/> If a Sections is compared with itself, 0 will be returned.
	 */
	@Override
	public int compareTo(@NotNull Section<? extends Type> section) {
		if (this == section) return 0;

		String thisTitle = getTitle();
		String otherTitle = section.getTitle();
		int comp = 0;
		if (thisTitle != null && otherTitle != null) {
			comp = thisTitle.compareTo(otherTitle);
		}
		else if (thisTitle != null) {
			comp = -1;
		}
		else if (otherTitle != null) {
			comp = 1;
		}
		if (comp == 0) {
			List<Integer> thisPos = getPositionInKDOM();
			List<Integer> otherPos = section.getPositionInKDOM();
			Iterator<Integer> thisIter = thisPos.iterator();
			Iterator<Integer> otherIter = otherPos.iterator();

			while (comp == 0 && thisIter.hasNext() && otherIter.hasNext()) {
				comp = thisIter.next().compareTo(otherIter.next());
			}
			if (comp == 0) {
				comp = thisPos.size() - otherPos.size();
			}
		}
		return comp;
	}

	/**
	 * Adds a child to this node. Use for KDOM creation and editing only!
	 */
	public void addChild(Section<?> s) {
		this.addChild(children == null ? 0 : children.size(), s);
	}

	/**
	 * Adds a child to this node. Use for KDOM creation and editing only!
	 */
	public void addChild(int index, Section<?> child) {
		if (children == null) children = new ArrayList<>(5);
		children.add(index, child);
		if (get() instanceof AbstractType && !(child.get() instanceof RootType)) {
			Class<?> childTypeClass = child.get().getClass();
			if (!Types.canHaveSuccessorOfType((AbstractType) type, childTypeClass)) {
				Log.severe("Added section of type '"
						+ childTypeClass.getSimpleName()
						+ "' to parent section of type '"
						+ type.getClass().getSimpleName()
						+ "', but parent type does not expect this child type. "
						+ "This has to be fixed, otherwise functionality based on a "
						+ "well defined type tree will not work properly.");
			}
		}
	}

	/**
	 * @return the text of this Section/Node
	 */
	public String getText() {
		if (text == null) {
			return getArticle().getText().substring(getOffsetInArticle(), getOffsetInArticle() + textLength);
		}
		else {
			return text;
		}
	}

	public void setText(String newText) {
		this.textLength = newText.length();
		if (getArticle() == null) this.text = newText;
		Section<? extends Type> parent = getParent();
		if (parent != null) {
			// we have to reset the offsets in all siblings after this section
			boolean found = false;
			for (Section<? extends Type> sibling : parent.getChildren()) {
				if (sibling == this || !found) {
					found = true;
					continue;
				}
				new Sections<>(sibling).successor().forEach(successor -> {
					successor.offsetInArticle = -1;
					successor.offsetInParent = -1;
				});
			}
			// to same for ancestors recursively
			parent.setText(parent.collectTextsFromChildren());
		}
	}

	public void setArticle(Article article) {
		this.article = article;
		this.text = null;
	}

	/**
	 * Returns the character index position in the articles text where this section starts.
	 *
	 * @return the start position of this section in the article text
	 * @created 25.11.2013
	 */
	public int getOffsetInArticle() {
		if (offsetInArticle == -1) {
			int parentOffset = parent != null ? parent.getOffsetInArticle() : 0;
			offsetInArticle = getOffsetInParent() + parentOffset;
		}
		return offsetInArticle;
	}

	public void removeAllChildren() {
		if (children != null) children = null;
	}

	/**
	 * @return the list of child nodes
	 */
	public List<Section<? extends Type>> getChildren() {
		if (children == null) return Collections.emptyList();
		return Collections.unmodifiableList(children);

	}

	/**
	 * Returns the parent section of this section.
	 *
	 * @return the parent section
	 */
	public Section<? extends Type> getParent() {
		return parent;
	}

	/**
	 * Returns the character index position in the parent's section text where this section starts.
	 *
	 * @return the start position of this section in the parent's section text
	 * @created 25.11.2013
	 */
	public int getOffsetInParent() {
		if (offsetInParent == -1) {
			int temp = 0;
			if (parent != null) {
				for (Section<?> child : parent.getChildren()) {
					if (child == this) break;
					temp += child.textLength;
				}
			}
			offsetInParent = temp;
		}
		return offsetInParent;
	}

	int getTextLength() {
		return textLength;
	}

	void setOffsetInParent(int offset) {
		this.offsetInParent = offset;
	}

	/**
	 * Returns the title of the article this section belongs to.
	 */
	public String getTitle() {
		return article == null ? null : article.getTitle();
	}

	public boolean addPackageName(String packageName) {
		if (getPackageNames().contains(packageName)) {
			return false;
		}
		else {
			if (packageNames == null) {
				packageNames = new HashSet<>(4);
			}
			packageNames.add(packageName);
			return true;
		}
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean removePackageName(String packageName) {
		boolean removed = packageNames != null && packageNames.remove(packageName);
		if (packageNames != null && packageNames.isEmpty()) packageNames = null;
		return removed;
	}

	public Set<String> getPackageNames() {
		if (parent == null) {
			if (packageNames == null) {
				return Collections.emptySet();
			}
			else {
				return Collections.unmodifiableSet(packageNames);
			}
		}
		else {
			Set<String> fatherPackageNames = parent.getPackageNames();
			if (packageNames == null) {
				return fatherPackageNames;
			}
			else {
				if (fatherPackageNames.isEmpty()) {
					return Collections.unmodifiableSet(packageNames);
				}
				else {
					Set<String> tempNamespaces = new HashSet<>(fatherPackageNames.size()
							+ packageNames.size());
					tempNamespaces.addAll(packageNames);
					tempNamespaces.addAll(fatherPackageNames);
					return Collections.unmodifiableSet(tempNamespaces);
				}
			}
		}
	}

	public String getWeb() {
		return this.article.getWeb();
	}

	public Article getArticle() {
		return this.article;
	}

	/**
	 * @return the depth of this Section inside the KDOM
	 */
	public int getDepth() {
		if (get() instanceof RootType) {
			return 1;
		}
		else {
			return parent.getDepth() + 1;
		}
	}

	/**
	 * Verbalizes this node
	 *
	 * @return the node information as text
	 */
	public String verbalize() {
		StringBuilder result = new StringBuilder();
		String simpleName = this.get().getClass().getSimpleName();
		if (Strings.containsIgnoreCase(simpleName, "anonymous")) {
			simpleName += "(" + this.get().getName() + ")";
		}
		result.append(simpleName);
		result.append(", ID: ").append(getID());
		result.append(", length: ")
				.append(this.getText().length())
				.append(" (")
				.append(getOffsetInParent())
				.append(")")
				.append(", children: ")
				.append(getChildren().size());
		String text = getText();
		String ot = this.textLength < 50 ? text : text.substring(0, 50) + "...";
		ot = ot.replaceAll("\\n", "\\\\n");
		result.append(", \"").append(ot);
		result.append("\"");
		return result.toString();
	}

	/**
	 * Returns the unique ID of this section.
	 */
	public String getID() {
		if (!hasID()) {
			intID = generateAndRegisterSectionID(this);
		}
		return Integer.toHexString(intID);
	}

	private String getSignatureString() {
		List<Integer> positionInKDOM = this.getPositionInKDOM();
		String positionInKDOMString = positionInKDOM.toString();
		return getWeb() + getTitle() + positionInKDOMString + this.getText();
	}

	private boolean hasID() {
		return this.intID != -1;
	}

	/**
	 * Collects the text of the section based on the leave sections only
	 *
	 * @return the appended text of the leave nodes
	 */
	public String collectTextsFromLeaves() {
		StringBuilder builder = new StringBuilder();
		collectTextsFromLeaves(builder);
		return builder.toString();
	}

	/**
	 * Collects the text of the section based on the leave sections only
	 *
	 * @param buffer the buffer to append the text to
	 */
	public void collectTextsFromLeaves(StringBuilder buffer) {
		if (!this.getChildren().isEmpty()) {
			getChildren().forEach(section -> section.collectTextsFromLeaves(buffer));
		}
		else {
			buffer.append(this.getText());
		}
	}

	/**
	 * Collects the text of the section based on the children sections only
	 *
	 * @return the appended text of the children nodes
	 */
	public String collectTextsFromChildren() {
		StringBuilder builder = new StringBuilder();
		collectTextsFromChildren(builder);
		return builder.toString();
	}

	/**
	 * Collects the text of the section based on the children sections only
	 *
	 * @param buffer the buffer to append the text to
	 */
	public void collectTextsFromChildren(StringBuilder buffer) {
		for (Section<?> s : this.getChildren()) {
			buffer.append(s.getText());
		}
	}

	public void setParent(Section<?> parent) {
		this.parent = parent;
	}

	/**
	 * Method that looks (recursively down) for this section whether some message of the specified type has been stored
	 * in that subtree.
	 */
	public boolean hasErrorInSubtree() {
		return hasMessageInSubtree(Message.Type.ERROR);
	}

	/**
	 * Method that looks (recursively down) for this section whether some errors has been stored in that subtree.
	 */
	public boolean hasMessageInSubtree(Message.Type type) {
		Map<Compiler, Collection<Message>> errors = Messages.getMessagesMap(this, type);
		if (!errors.isEmpty()) return true;
		for (Section<?> child : getChildren()) {
			if (child.hasErrorInSubtree()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method that looks (recursively down) for this section whether some messages of the specified type has been
	 * stored in that subtree for the given article.
	 */
	public boolean hasErrorInSubtree(Compiler compiler) {
		return hasMessageInSubtree(compiler, Message.Type.ERROR);
	}

	/**
	 * Method that looks (recursively down) for this section whether some errors has been stored in that subtree for
	 * the given compiler or compiler independent.
	 */
	public boolean hasMessageInSubtree(Compiler compiler, Message.Type type) {
		Map<Compiler, Collection<Message>> errors = Messages.getMessagesMap(
				this, type);
		if (errors.get(compiler) != null) return true;
		if (errors.get(null) != null) return true;
		for (Section<?> child : getChildren()) {
			boolean err = child.hasErrorInSubtree(compiler);
			if (err) {
				return true;
			}
		}

		return false;
	}

	protected void setPositionInKDOM(List<Integer> positionInKDOM) {
		position = positionInKDOM;
	}

	protected void clearPositionInKDOM() {
		position = null;
	}

	public List<Integer> getPositionInKDOM() {
		if (position == null) {
			position = calcPositionInKDOM();
		}
		return Collections.unmodifiableList(position);
	}

	protected void setLastPositionInKDOM(List<Integer> lastPositions) {
		this.lastPositions = lastPositions;
	}

	public List<Integer> getLastPositionInKDOM() {
		return lastPositions == null ? null :
				Collections.unmodifiableList(lastPositions);
	}

	private List<Integer> calcPositionInKDOM() {

		// this can only occur in the exceptional case
		// when Sections are created independently of articles
		if (getArticle() == null || getArticle().getRootSection() == null) {
			ArrayList<Integer> positions = new ArrayList<>(1);
			positions.add(0);
			return positions;
		}

		return calcPositionTil(getArticle().getRootSection());
	}

	public List<Integer> calcPositionTil(Section<?> end) {
		LinkedList<Integer> positions = new LinkedList<>();
		Section<?> temp = this;
		Section<?> tempFather = temp.getParent();
		while (temp != end && tempFather != null) {
			List<Section<?>> childrenList = tempFather.getChildren();
			int indexOf = getIndex(temp, childrenList);
			positions.addFirst(indexOf);
			temp = tempFather;
			tempFather = temp.getParent();
		}
		return new ArrayList<>(positions); // for a smaller memory footprint
	}

	/**
	 * Calculates the index of a contained object without using the equals-method but using object identity instead.
	 *
	 * @param object the object to get the index for
	 * @param list   the list of objects to search in
	 * @return the index or -1 if the section is not contained in the list
	 * @created 10.07.2012
	 */
	private int getIndex(Object object, List<?> list) {
		int index = 0;
		for (Object item : list) {
			// this == comparator is on purpose on this place as this method is
			// used in the equals method
			if (item == object) {
				return index;
			}
			index++;
		}
		return -1;
	}

	public ArticleManager getArticleManager() {
		return article.getArticleManager();
	}

	/**
	 * Generates an ID for the given Section and also registers the Section in a HashMap to allow searches for this
	 * Section later. The ID is the hash code of the following String: Title of the Article containing the Section, the
	 * position in the KDOM and the content of the Section. Hash collisions are resolved.
	 *
	 * @param section is the Section for which the ID is generated and which is then registered
	 * @return the ID for the given Section
	 * @created 05.09.2011
	 */
	private static int generateAndRegisterSectionID(Section<?> section) {
		int idCandidate = section.getSignatureString().hashCode();
		if (section.getArticle().isTemporary()) return idCandidate;
		synchronized (sectionMap) {
			Section<?> existingSection = sectionMap.get(idCandidate);
			while (existingSection != null || idCandidate == -1) {
				++idCandidate;
				existingSection = sectionMap.get(idCandidate);
			}
			sectionMap.put(idCandidate, section);
		}
		return idCandidate;
	}

	/**
	 * This method removes the entries of sections in the section map, if the sections are no longer used - for example
	 * after an article is changed and build again.<br/> This method also takes care, that the equal section in the new
	 * article gets an id. Since ids are created lazy while rendering and often the article is not rendered completely
	 * again after changing just a few isolated spots in the article, those ids would otherwise be gone, although they
	 * might stay the same and are still usable.<br/> We just look at the same spot/position in the KDOM. If we find a
	 * Section and the Section also has the same type, we generate and register the id. Of course we will only catch
	 * the right Sections if the KDOM has not changed in this part, but if the KDOM has changed, also the ids will have
	 * changed and therefore we don't need them anyway. We only do this, to allow already rendered tools to still work,
	 * if it is possible.
	 *
	 * @param section    the old, no longer used Section
	 * @param newArticle the new article which potentially contains an equal section
	 * @created 04.12.2012
	 */
	public static void unregisterOrUpdateSectionID(Section<?> section, Article newArticle) {
		if (section.hasID()) {
			unregisterID(section);
			if (newArticle == null) return; // if there is a new version, try to salvage
			Section<?> newSection = Sections.get(newArticle, section.getPositionInKDOM());
			if (newSection != null
					&& newSection.get().getClass().equals(section.get().getClass())) {
				// to not add ids to completely different sections if the
				// article changed a lot, we only generate section ids for the
				// same type of sections that had ids in the old article
				newSection.getID();
			}
		}
	}

	private static void unregisterID(Section<?> section) {
		synchronized (sectionMap) {
			sectionMap.remove(section.intID);
		}
	}

	/**
	 * This method is protected, use {@link Sections#get(String)} instead.
	 *
	 * @param id is the ID of the Section to be returned
	 * @return the Section for the given ID or null if no Section exists for this ID.
	 */
	protected static Section<?> get(String id) {
		synchronized (sectionMap) {
			// We have to parse long and convert to int, because when converting a int to a hex string, the negative
			// sign is lost, resulting in for Integer.parseInt() not parsable values. Parsing long and casting
			// to int will restore the negative sign.
			return sectionMap.get((int) Long.parseLong(id, 16));
		}
	}

	/**
	 * Has the same behavior as {@link #getObject(Compiler, String)}
	 * with <tt>null</tt> as the {@link Compiler} argument. Use this if the
	 * Object was stored the same way (<tt>null</tt> as the {@link Compiler}
	 * argument or the method {@link #storeObject(String, Object)}.
	 *
	 * @param key is the key for which the object was stored
	 * @return the previously stored Object for this key or <tt>null</tt>, if no
	 * Object was stored
	 * @created 08.07.2011
	 */
	public Object getObject(String key) {
		//noinspection RedundantCast
		return getObject((Compiler) null, key);
	}

	/**
	 * All objects stored in this Section with the given <tt>key</tt>
	 * are collected and returned. The {@link Map} stores them by the title of
	 * the {@link Article} they were stored for. If an object was stored without
	 * an argument {@link Article} (article independent), the returned
	 * {@link Map} contains this object with <tt>null</tt> as the key.
	 *
	 * @param key is the key for which the objects were stored
	 * @created 16.02.2012
	 */
	public synchronized Map<Compiler, Object> getObjects(String key) {
		if (store == null) return Collections.emptyMap();
		Map<Compiler, Object> objects = new HashMap<>(store.size());
		for (Map.Entry<Compiler, Map<String, Object>> entry : store.entrySet()) {
			Compiler compiler = entry.getKey();
			if (compiler != null && !compiler.getCompilerManager().contains(compiler)) continue;
			Object object = entry.getValue().get(key);
			if (object != null) objects.put(compiler, object);
		}
		return Collections.unmodifiableMap(objects);
	}

	/**
	 * @param compiler is the {@link Article} for which the Object was stored
	 * @param key      is the key, for which the Object was stored
	 * @return the previously stored Object for the given key and article, or
	 * <tt>null</tt>, if no Object was stored
	 * @created 08.07.2011
	 */
	public synchronized Object getObject(Compiler compiler, String key) {
		if (compiler != null && !compiler.getCompilerManager().contains(compiler)) return null;
		Map<String, Object> storeForArticle = getStoreForCompiler(compiler);
		if (storeForArticle == null) return null;
		return storeForArticle.get(key);
	}

	/**
	 * Stores the given Object for the given key.<br/>
	 * <b>Attention:</b> Be aware, that some times an Object should only be
	 * stored in the context of a certain {@link Compiler}. Example: An Object
	 * was created, because the Section was compiled for a certain
	 * {@link Article}. If the Section however gets compiled again for another
	 * {@link Article}, the Object would not be created or a different
	 * {@link Object} would be created. In this case you have to use the method
	 * {@link #storeObject(Compiler, String, Object)} to be able to
	 * differentiate between the {@link Compiler}s.
	 *
	 * @param key    is the key for which the Object should be stored
	 * @param object is the object to be stored
	 * @created 08.07.2011
	 */
	public void storeObject(String key, Object object) {
		//noinspection RedundantCast
		storeObject((Compiler) null, key, object);
	}

	/**
	 * Removes the Object stored for the given key.<br/>
	 *
	 * @param key is the key for which the Object should be removed
	 * @created 16.03.2014
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Object removeObject(String key) {
		//noinspection RedundantCast
		return removeObject((Compiler) null, key);
	}

	/**
	 * Stores the given Object for the given key and {@link Compiler}.
	 * <b>Attention:</b> If the Object you want to store is independent from the
	 * {@link Compiler} that will or has compiled this Section,
	 * you can either set the {@link Compiler} argument to
	 * <tt>null</tt> or use the method
	 * {@link #storeObject(String, Object)} instead (same for
	 * applies for retrieving the Object later via the getObject - methods).
	 *
	 * @param compiler the compiler to store the object for
	 * @param key      the key to store the object for
	 * @param object   the object to be stored
	 * @created 08.07.2011
	 */
	public synchronized void storeObject(Compiler compiler, String key, Object object) {
		Map<String, Object> storeForCompiler = getStoreForCompiler(compiler);
		if (storeForCompiler == null) {
			storeForCompiler = new HashMap<>(4);
			putStoreForCompiler(compiler, storeForCompiler);
		}
		storeForCompiler.put(key, object);
	}

	/**
	 * Removes the Object stored for the given key and {@link Compiler}.
	 * <b>Attention:</b> If the Object you want to remove is independent from the
	 * {@link Compiler} that will or has compiled this Section, you can either
	 * set the {@link Compiler} argument to  <tt>null</tt> or use the method
	 * {@link #removeObject(String)} instead.
	 *
	 * @param compiler the compiler to store the object for
	 * @param key      is the key for which the Object should be removed
	 * @created 16.03.2014
	 */
	public synchronized Object removeObject(Compiler compiler, String key) {
		Map<String, Object> storeForCompiler = getStoreForCompiler(compiler);
		if (storeForCompiler == null) return null;
		Object removed = storeForCompiler.remove(key);
		if (storeForCompiler.isEmpty()) store.remove(compiler);
		if (store.isEmpty()) store = null;
		return removed;
	}

	private Map<String, Object> getStoreForCompiler(Compiler compiler) {
		if (store == null) return null;
		return store.get(compiler);
	}

	private void putStoreForCompiler(Compiler compiler, Map<String, Object> storeForCompiler) {
		if (store == null) {
			store = new WeakHashMap<>(4);
		}
		store.put(compiler, storeForCompiler);
	}

	public boolean isEmpty() {
		return store == null;
	}

}
