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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

import org.eclipse.pde.internal.ua.core.icheatsheet.ICSConstants;

/**
 * ISimpleCSConstants
 * 
 */
public interface ISimpleCSConstants extends ICSConstants {

	// Elements

	public static final String ELEMENT_CHEATSHEET = "cheatsheet"; //$NON-NLS-1$

	public static final String ELEMENT_INTRO = "intro"; //$NON-NLS-1$

	public static final String ELEMENT_ITEM = "item"; //$NON-NLS-1$

	public static final String ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$	

	public static final String ELEMENT_ACTION = "action"; //$NON-NLS-1$	

	public static final String ELEMENT_COMMAND = "command"; //$NON-NLS-1$	

	public static final String ELEMENT_PERFORM_WHEN = "perform-when"; //$NON-NLS-1$	

	public static final String ELEMENT_SUBITEM = "subitem"; //$NON-NLS-1$	

	public static final String ELEMENT_REPEATED_SUBITEM = "repeated-subitem"; //$NON-NLS-1$	

	public static final String ELEMENT_CONDITIONAL_SUBITEM = "conditional-subitem"; //$NON-NLS-1$	

	public static final String ELEMENT_ONCOMPLETION = "onCompletion"; //$NON-NLS-1$	

	public static final String ELEMENT_BR = "br"; //$NON-NLS-1$		

	// Attributes

	public static final String ATTRIBUTE_TITLE = "title"; //$NON-NLS-1$

	public static final String ATTRIBUTE_CONTEXTID = "contextId"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_HREF = "href"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_DIALOG = "dialog"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_SKIP = "skip"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_WHEN = "when"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_TRANSLATE = "translate"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_CONDITION = "condition"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_VALUES = "values"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_PLUGINID = "pluginId"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_PARAM = "param"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_CONFIRM = "confirm"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_REQUIRED = "required"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_SERIALIZATION = "serialization"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_RETURNS = "returns"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_ONCOMPLETION = "onCompletion"; //$NON-NLS-1$	

	// Attribute Values

	// Types

	public static final int TYPE_CHEAT_SHEET = 0;

	public static final int TYPE_ACTION = 1;

	public static final int TYPE_COMMAND = 2;

	public static final int TYPE_CONDITIONAL_SUBITEM = 3;

	public static final int TYPE_DESCRIPTION = 4;

	public static final int TYPE_INTRO = 5;

	public static final int TYPE_ITEM = 6;

	public static final int TYPE_ON_COMPLETION = 7;

	public static final int TYPE_PERFORM_WHEN = 8;

	public static final int TYPE_REPEATED_SUBITEM = 9;

	public static final int TYPE_SUBITEM = 10;

}
