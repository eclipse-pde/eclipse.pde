/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.search;

public class PluginSearchInput {
	public static final int ELEMENT_PLUGIN = 1;
	public static final int ELEMENT_FRAGMENT = 2;
	public static final int ELEMENT_EXTENSION_POINT = 3;

	public static final int LIMIT_DECLARATIONS = 1;
	public static final int LIMIT_REFERENCES = 2;
	public static final int LIMIT_ALL = 3;

	private String searchString = null;
	private boolean caseSensitive = true;
	private int searchElement = 0;
	private int searchLimit = 0;
	private PluginSearchScope searchScope;

	public String getSearchString() {
		return searchString;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean value) {
		caseSensitive = value;
	}

	public void setSearchString(String name) {
		searchString = name;
	}

	public int getSearchElement() {
		return searchElement;
	}

	public void setSearchElement(int element) {
		searchElement = element;
	}

	public int getSearchLimit() {
		return searchLimit;
	}

	public void setSearchLimit(int limit) {
		searchLimit = limit;
	}

	public PluginSearchScope getSearchScope() {
		return searchScope;
	}

	public void setSearchScope(PluginSearchScope scope) {
		searchScope = scope;
	}

}
