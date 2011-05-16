/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private HashMap fCategoryNameMap;

	private HashMap fCategoryIDMap;

	private HashMap fCategoryTypeMap;

	/**
	 * 
	 */
	public CSCategoryTrackerUtil() {

		// Look-up hashmap
		// Keys are category ids
		// Values are category names		
		fCategoryIDMap = new HashMap();

		// Reverse look-up hashmap
		// Keys are category names
		// Values are category ids
		fCategoryNameMap = new HashMap();

		// Look-up hashmap
		// Keys are category ids
		// Values are category types: either new or old
		fCategoryTypeMap = new HashMap();

	}

	/**
	 * @param id
	 * @param name
	 */
	public void associate(String id, String name, int type) {
		fCategoryNameMap.put(name, id);
		fCategoryIDMap.put(id, name);
		fCategoryTypeMap.put(id, new Integer(type));
	}

	/**
	 * @param name
	 * @return
	 */
	public boolean containsCategoryName(String name) {
		return fCategoryNameMap.containsKey(name);
	}

	/**
	 * @param id
	 * @return
	 */
	public boolean containsCategoryID(String id) {
		return fCategoryIDMap.containsKey(id);
	}

	/**
	 * @param id
	 * @return
	 */
	public String getCategoryName(String id) {
		return (String) fCategoryIDMap.get(id);
	}

	/**
	 * @param name
	 * @return
	 */
	public String getCategoryID(String name) {
		return (String) fCategoryNameMap.get(name);
	}

	/**
	 * @param id
	 * @return
	 */
	public int getCategoryType(String id) {
		Integer integer = (Integer) fCategoryTypeMap.get(id);
		if (integer == null) {
			return F_TYPE_NO_CATEGORY;
		}
		return integer.intValue();
	}

}
