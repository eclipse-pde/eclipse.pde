/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.ctxhelp;

/**
 * Interface containing constants used for the context help editor.
 * 
 * @since 3.4
 */
public interface ICtxHelpConstants {

	// Elements

	public static final String ELEMENT_ROOT = "contexts"; //$NON-NLS-1$

	public static final String ELEMENT_CONTEXT = "context"; //$NON-NLS-1$

	public static final String ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$

	public static final String ELEMENT_TOPIC = "topic"; //$NON-NLS-1$

	public static final String ELEMENT_COMMAND = "command"; //$NON-NLS-1$

	// Attributes

	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	public static final String ATTRIBUTE_TITLE = "title"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_HREF = "href"; //$NON-NLS-1$

	public static final String ATTRIBUTE_SERIAL = "serialization"; //$NON-NLS-1$	

	public static final String ATTRIBUTE_FILTER = "filter"; //$NON-NLS-1$	

	// Types

	public static final int TYPE_ROOT = 0;

	public static final int TYPE_CONTEXT = 1;

	public static final int TYPE_DESCRIPTION = 2;

	public static final int TYPE_TOPIC = 3;

	public static final int TYPE_COMMAND = 4;

}
