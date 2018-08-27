/*******************************************************************************
 *  Copyright (c) 2011, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public int getBaseUnit() {
		return UNIT_ATTRIBUTE_SEARCH_PATTERN;
	}

}