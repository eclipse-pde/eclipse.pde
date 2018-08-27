/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

import org.eclipse.pde.core.IWritable;

/**
 * Objects of this type are holding information about complex types defined
 * inside schema elements.
 */
public interface ISchemaComplexType extends ISchemaType, ISchemaAttributeProvider, IWritable {
	/**
	 * A complex type can have one root compositor.
	 *
	 * @return root complex type compositor
	 */
	public ISchemaCompositor getCompositor();

	/**
	 * Returns whether the content of the element that owns this type
	 * can mix child elements and text.
	 *
	 * @return true if element can mix text and other elements
	 */
	public boolean isMixed();
}
