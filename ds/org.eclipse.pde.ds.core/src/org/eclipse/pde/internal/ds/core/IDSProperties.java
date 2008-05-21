/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;



/**
 * Represents a set of properties from a bundle entry
 * 
 * @since 3.4
 * @see IDSObject
 */
public interface IDSProperties extends IDSObject {

	/**
	 * Sets the entry path relative to the root of the bundle
	 * 
	 * @param entry
	 *            New entry path
	 */
	public void setEntry(String entry);

	/**
	 * Returns the value of the entry path
	 * 
	 * @return String value of the entry path
	 */
	public String getEntry();

}
