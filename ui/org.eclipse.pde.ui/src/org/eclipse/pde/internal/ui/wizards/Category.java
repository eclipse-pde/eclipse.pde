/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.*;
import org.eclipse.core.runtime.*;

public class Category {
	private IConfigurationElement config;
	private String [] parentCategoryPath;
	public static final String ATT_ID="id";
	public static final String ATT_CATEGORY="parentCategory";
	public static final String ATT_NAME="name";

public Category(IConfigurationElement aConfig) {
	config = aConfig;
}
public String getID() {
	return config.getAttribute(ATT_ID);
}
public String getLabel() {
	return config.getAttribute(ATT_NAME);
}
public String[] getParentCategoryPath() {
	if (parentCategoryPath!=null) return parentCategoryPath;
	String category = config.getAttribute(ATT_CATEGORY);
	if (category==null) return null;
	StringTokenizer stok = new StringTokenizer(category, "/");
	parentCategoryPath = new String [stok.countTokens()];
	for (int i=0; stok.hasMoreTokens(); i++) {
		parentCategoryPath[i]=stok.nextToken();
	}
	return parentCategoryPath;
	

}
}
