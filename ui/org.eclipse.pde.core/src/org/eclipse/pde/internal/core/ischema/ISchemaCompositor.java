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
package org.eclipse.pde.internal.core.ischema;

/**
 * Compositor is a container that can contain other compositors or
 * references to element defined at the global scope. Compositors
 * are used to recursively define content model for the type.
 * Compositor kind (all, choice, sequence or group) defines
 * how to interpret its children.
 */
public interface ISchemaCompositor extends ISchemaObject, ISchemaRepeatable {
	/**
	 * Indicates the root parent of the compositor.
	 */
	public static final int ROOT = -1;
	/**
	 * Indicates that the children can be in any order and cardinality.
	 */
	public static final int ALL = 0;
	/**
	 * Indicates that an only one of the compositor's children can
	 * appear at this location (DTD eq: "|")
	 */
	public static final int CHOICE = 1;
	/**
	 * Indicates that the children must appear in sequence in the schema documents (DTD eq: "," )
	 */
	public static final int SEQUENCE = 2;
	/**
	 * Indicates that this compositor simply serves as a group (DTD eq: "()" )
	 */
	public static final int GROUP = 3;
	/**
	 * Keyword table for compositors.
	 */
	public static final String[] kindTable = {"all", "choice", "sequence", "group"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/**
	 * Returns the number of children of this compositor.
	 * @return number of compositor children
	 */
	public int getChildCount();

	/**
	 * Returns children of this compositor.
	 * @return compositor children
	 */
	public ISchemaObject[] getChildren();

	/**
	 * Returns a flag that defines how the children of this compositors should be
	 * treated when computing type grammar (one of ALL, CHOICE, GROUP, SEQUENCE).
	 * @return compositor kind value
	 */
	public int getKind();
}
