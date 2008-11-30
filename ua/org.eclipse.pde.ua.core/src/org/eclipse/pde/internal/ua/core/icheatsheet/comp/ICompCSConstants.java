/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gaetano Santoro <gaetano.santoro@st.com> - Bug 211754
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.icheatsheet.comp;

import org.eclipse.pde.internal.ua.core.icheatsheet.ICSConstants;

/**
 * ICompCSConstants
 * 
 */
public interface ICompCSConstants extends ICSConstants {

	// Elements

	public static final String ELEMENT_COMPOSITE_CHEATSHEET = "compositeCheatsheet"; //$NON-NLS-1$

	public static final String ELEMENT_TASKGROUP = "taskGroup"; //$NON-NLS-1$

	public static final String ELEMENT_TASK = "task"; //$NON-NLS-1$

	public static final String ELEMENT_INTRO = "intro"; //$NON-NLS-1$

	public static final String ELEMENT_ONCOMPLETION = "onCompletion"; //$NON-NLS-1$	

	public static final String ELEMENT_DEPENDENCY = "dependsOn"; //$NON-NLS-1$	

	public static final String ELEMENT_PARAM = "param"; //$NON-NLS-1$	

	// Attributes

	public static final String ATTRIBUTE_KIND = "kind"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_SKIP = "skip"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$

	public static final String ATTRIBUTE_TASK = ELEMENT_TASK;

	// Attribute Values:
	// Element: param
	// Attribute: name

	public static final String ATTRIBUTE_VALUE_ID = ATTRIBUTE_ID;

	public static final String ATTRIBUTE_VALUE_PATH = "path"; //$NON-NLS-1$

	public static final String ATTRIBUTE_VALUE_SHOWINTRO = "showIntro"; //$NON-NLS-1$

	// Attribute Values:
	// Element: taskGroup
	// Attribute: kind

	public static final String ATTRIBUTE_VALUE_SET = "set"; //$NON-NLS-1$

	public static final String ATTRIBUTE_VALUE_SEQUENCE = "sequence"; //$NON-NLS-1$

	public static final String ATTRIBUTE_VALUE_CHOICE = "choice"; //$NON-NLS-1$

	// Attribute Values:
	// Element: task
	// Attribute: kind

	public static final String ATTRIBUTE_VALUE_CHEATSHEET = "cheatsheet"; //$NON-NLS-1$

	// Types

	public static final int TYPE_COMPOSITE_CHEATSHEET = 0;

	public static final int TYPE_TASKGROUP = 1;

	public static final int TYPE_TASK = 2;

	public static final int TYPE_INTRO = 3;

	public static final int TYPE_ONCOMPLETION = 4;

	public static final int TYPE_DEPENDENCY = 5;

	public static final int TYPE_PARAM = 6;

}
