/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Carver - STAR - bug 213255
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

/**
 * Document section is portion of the extension point schema
 * definition that will be taken and built into the final reference
 * HTML document. There are several predefined document sections
 * that PDE recognizes:
 * <ul>
 * <li>MARKUP - will be used for "Markup" section</li>
 * <li>EXAMPLES - will be used for "Examples" section</li>
 * <li>API_INFO - will be used for "API information" section</li>
 * <li>IMPLEMENTATION - will be used for "Supplied Implementation" section</li>
 * </ul>
 * Text that objects of this class carry can contain HTML tags that
 * will be copied into the target document as-is.
 */
public interface IDocumentSection extends ISchemaObject {
	/**
	 * Section Id for the "Markup" section of the target reference document
	 */
	String MARKUP = "markup"; //$NON-NLS-1$
	/**
	 * Section Id for the "Examples" section of the target reference document
	 */
	String EXAMPLES = "examples"; //$NON-NLS-1$
	/**
	 * Section Id for the "Supplied Implementation" section of the target reference document
	 */
	String IMPLEMENTATION = "implementation"; //$NON-NLS-1$
	/**
	 * Section Id for the "API Information" section of the target reference document
	 */
	String API_INFO = "apiinfo"; //$NON-NLS-1$
	/**
	 * Section Id for the copyright statement section of the target reference document
	 */
	String COPYRIGHT = "copyright"; //$NON-NLS-1$

	/**
	 * Section Id for the first version in which the extension point appears.
	 */
	String SINCE = "since"; //$NON-NLS-1$

	/**
	 * Returns the Id of this section.
	 */
	public String getSectionId();
}
