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
 * Represents the component implementation class
 * 
 * @since 3.4
 * @see IDSComponent
 * @see IDSObject
 * 
 */
public interface IDSImplementation extends IDSObject {

	/**
	 * Sets the java fully qualified name of the implementation class.
	 * 
	 * @param className
	 *            new java fully qualified name
	 */
	public void setClassName(String className);

	/**
	 * Returns the java fully qualified name of the implementation class.
	 * 
	 * @return String containing the java fully qualified name
	 */
	public String getClassName();

}
