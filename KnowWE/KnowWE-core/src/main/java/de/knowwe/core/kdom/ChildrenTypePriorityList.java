/*
 * Copyright (C) 2013 denkbares GmbH
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
package de.knowwe.core.kdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Auxiliary data structure that helps to build up the list of children during
 * the initialization process of all plugins.
 * 
 * @author Jochen Reutelshoefer
 * @created 26.08.2013
 */
public class ChildrenTypePriorityList {

	public static double DEFAULT_PRIORITY = 5;

	/**
	 * Data structure containing the children according to specified priorities
	 * (if such are given). For each priority value a list is managed. According
	 * to the final total order of the children types the types within a list
	 * have descending order.
	 */
	private final SortedMap<Double, List<Type>> children = new TreeMap<Double, List<Type>>();

	/**
	 * Caches the children order as defined by the priority map.
	 */
	private List<Type> cachedChildrenOrder = null;

	/**
	 * Replaces the first type with the passed type where the type is instance
	 * of the passed class, if such is existing.
	 * 
	 * @created 27.08.2013
	 * @param newType type to be inserted
	 * @param classToBeReplaced class to determine what type should be replaced
	 * @throws InvalidKDOMSchemaModificationOperation
	 * @return true if a replacement has been made
	 */
	public boolean replaceChildType(Type newType, Class<? extends Type> classToBeReplaced) {
		Set<Double> keySet = children.keySet();
		for (Double priorityValue : keySet) {
			List<Type> childrenForPriorityValue = children.get(priorityValue);
			Type toReplace = null;
			for (Type type : childrenForPriorityValue) {
				if (classToBeReplaced.isAssignableFrom(type.getClass())) {
					toReplace = type;
					break;
				}
			}
			if (toReplace != null) {
				int index = childrenForPriorityValue.indexOf(toReplace);
				childrenForPriorityValue.set(index, newType);
				clearCache();
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the final (at the point of execution) total order of the
	 * children.
	 * 
	 * @created 27.08.2013
	 * @return
	 */
	public List<Type> getChildrenTypes() {
		if (cachedChildrenOrder != null) {
			return cachedChildrenOrder;
		}
		List<Type> childrenOrder = new ArrayList<Type>();
		Set<Double> keySet = children.keySet();
		for (Double priorityValue : keySet) {
			List<Type> childrenForPriorityValue = children.get(priorityValue);
			childrenOrder.addAll(childrenForPriorityValue);
		}
		cachedChildrenOrder = childrenOrder;
		return Collections.unmodifiableList(childrenOrder);
	}

	/**
	 * Clears the children type data structure.
	 * 
	 * @created 27.08.2013
	 */
	public void clear() {
		children.clear();
	}

	private void clearCache() {
		this.cachedChildrenOrder = null;
	}

	/**
	 * Adds the type as a child of the current type with the given priority
	 * value. If there are already child types for the given priority it is
	 * appended at the end of the list (lower priority).
	 * 
	 * @created 27.08.2013
	 * @param priority the priority with which the type is added
	 * @param type the type to add
	 */
	public void addChildType(double priority, Type t) {
		List<Type> list = children.get(priority);
		if (list == null) {
			list = new ArrayList<Type>();
			children.put(priority, list);
		}
		list.add(t);
		clearCache();
	}

	/**
	 * Adds the type as achild with the given priority value. If there are
	 * already child types for the given priority it is appended at the end of
	 * the list (lower priority).
	 * 
	 * @created 27.08.2013
	 * @param pos the positioner where the child is added
	 * @param type the type to add
	 * @deprecated The position may change if other types are inserted into the
	 *             chain later. It is recommended to work with priorities,
	 *             therefore use {@link Type#addChildType(double, Type)}
	 */
	@Deprecated
	public void addChildTypeAtPosition(int pos, Type type) {
		List<Type> childrenTypes = this.getChildrenTypes();
		int index = 0;
		boolean foundIndex = false;
		for (Type childType : childrenTypes) {
			if (index == pos) {
				foundIndex = true;
				addBefore(childType, childType);
			}
			index++;
		}
		if (!foundIndex) {
			addLast(type);
		}
	}

	private void addBefore(Type indexType, Type newType) {
		Set<Double> keySet = children.keySet();
		for (Double priorityValue : keySet) {
			List<Type> childrenForPriorityValue = children.get(priorityValue);
			if (childrenForPriorityValue.contains(indexType)) {
				childrenForPriorityValue.add(childrenForPriorityValue.indexOf(indexType), newType);
			}
		}

	}

	/**
	 * Adds the type as the (currently) last child type with the default
	 * priority (which is 5).
	 * 
	 * @created 27.08.2013
	 * @param type the type to add
	 */
	public void addChildType(Type t) {
		addChildType(DEFAULT_PRIORITY, t);
	}

	/**
	 * Adds the given type at the end of the (current) priority chain.
	 * 
	 * @created 27.08.2013
	 * @param t
	 */
	public void addLast(Type t) {
		Double lastKey = DEFAULT_PRIORITY - 1;
		if (children.keySet().size() != 0) {
			lastKey = children.lastKey();
		}
		Double newLastKey = lastKey + 1;
		List<Type> set = new ArrayList<Type>();
		set.add(t);
		children.put(newLastKey, set);
		clearCache();
	}

	/**
	 * Removes the first occurrence (descending priority order) of a child type
	 * where the given class c is assignable from this type.
	 * 
	 * @created 27.08.2013
	 * @param c
	 * @return
	 */
	public Type removeChildType(Class<? extends Type> c) {
		Set<Double> keySet = children.keySet();
		for (Double priorityValue : keySet) {
			List<Type> childrenForPriorityValue = children.get(priorityValue);
			Type toRemove = null;
			for (Type type : childrenForPriorityValue) {
				if (c.isAssignableFrom(type.getClass())) {
					toRemove = type;
					break;
				}
			}
			if (toRemove != null) {
				childrenForPriorityValue.remove(toRemove);
				clearCache();
				return toRemove;
			}
		}
		return null;
	}

}