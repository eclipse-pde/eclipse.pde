/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

/**
 * Objects that implement this interface can
 * have attributes.
 */
public interface ISchemaAttributeProvider {
	/**
	 * Returns an attribute definition if one with the matching name is found
	 * in this provider.
	 * @return attribute object or <samp>null</samp> if none with the matching name is found.
	 */
	public ISchemaAttribute getAttribute(String name);

	public int getAttributeCount();

	public ISchemaAttribute[] getAttributes();
}
