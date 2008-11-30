/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.toc;

/**
 * ITocConstants This interface contains all of the constants relevant to the
 * TOC editor
 * 
 * @since 3.4
 */
public interface ITocConstants {

	// Elements

	public static final String ELEMENT_TOC = "toc"; //$NON-NLS-1$

	public static final String ELEMENT_TOPIC = "topic"; //$NON-NLS-1$

	public static final String ELEMENT_ANCHOR = "anchor"; //$NON-NLS-1$

	public static final String ELEMENT_LINK = "link"; //$NON-NLS-1$

	// Attributes

	public static final String ATTRIBUTE_LINK_TO = "link_to"; //$NON-NLS-1$

	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_TOPIC = ELEMENT_TOPIC;

	public static final String ATTRIBUTE_HREF = "href"; //$NON-NLS-1$		

	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	public static final String ATTRIBUTE_TOC = ELEMENT_TOC;

	// Types

	public static final int TYPE_TOC = 0;

	public static final int TYPE_TOPIC = 1;

	public static final int TYPE_ANCHOR = 2;

	public static final int TYPE_LINK = 3;

}
