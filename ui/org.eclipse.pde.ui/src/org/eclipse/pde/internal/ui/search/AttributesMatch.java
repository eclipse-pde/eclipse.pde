/*******************************************************************************
 *  Copyright (c) 2011, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sascha Becher <s.becher@qualitype.de> - bug 360894
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.search.ui.text.Match;

/**
 * An extension to {@link Match} in order to present matching plugins which resulted 
 * in a search queried from the extensions page of the {@link ManifestEditor} 
 */
public class AttributesMatch extends Match {

	/**
	 * A constant expressing that the {@link Match} resulted in a search queried from 
	 * the extensions page of the {@link ManifestEditor} 
	 */
	public static final int UNIT_ATTRIBUTE_SEARCH_PATTERN = 3;

	protected String searchPattern;

	public AttributesMatch(Object element, String searchPattern) {
		super(element, UNIT_LINE, 0, 0);
		this.searchPattern = searchPattern;
	}

	public String getSearchPattern() {
		return searchPattern;
	}

	public int getBaseUnit() {
		return UNIT_ATTRIBUTE_SEARCH_PATTERN;
	}

}