/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import java.util.HashMap;

/**
 * CSCategoryTrackerUtil
 * Bi-directional hash map utility
 */
public class CSCategoryTrackerUtil {

	public final static int F_TYPE_NO_CATEGORY = 0;

	public final static int F_TYPE_NEW_CATEGORY = 1;

	public final static int F_TYPE_OLD_CATEGORY = 2;

	private HashMap<String, String> fCategoryNameMap;

	private HashMap<String, String> fCategoryIDMap;

	private HashMap<String, Integer> fCategoryTypeMap;

	public CSCategoryTrackerUtil() {

		// Look-up hashmap
		// Keys are category ids
		// Values are category names
		fCategoryIDMap = new HashMap<>();

		// Reverse look-up hashmap
		// Keys are category names
		// Values are category ids
		fCategoryNameMap = new HashMap<>();

		// Look-up hashmap
		// Keys are category ids
		// Values are category types: either new or old
		fCategoryTypeMap = new HashMap<>();

	}

	public void associate(String id, String name, int type) {
		fCategoryNameMap.put(name, id);
		fCategoryIDMap.put(id, name);
		fCategoryTypeMap.put(id, Integer.valueOf(type));
	}

	public boolean containsCategoryName(String name) {
		return fCategoryNameMap.containsKey(name);
	}

	public boolean containsCategoryID(String id) {
		return fCategoryIDMap.containsKey(id);
	}

	public String getCategoryName(String id) {
		return fCategoryIDMap.get(id);
	}

	public String getCategoryID(String name) {
		return fCategoryNameMap.get(name);
	}

	public int getCategoryType(String id) {
		Integer integer = fCategoryTypeMap.get(id);
		if (integer == null) {
			return F_TYPE_NO_CATEGORY;
		}
		return integer.intValue();
	}

}
