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
 * Represents the provide element that define the service interfaces.
 * 
 * @since 3.4
 * @see IDSService
 * @see IDSObject
 */
public interface IDSProvide extends IDSObject {

	/**
	 * Sets the name of the interface that this service is registered under.
	 * This name must be the fully qualified name of a Java class.
	 * 
	 * @param interfaceName
	 *            new fully qualified name of a Java class.
	 */
	public void setInterface(String interfaceName);

	/**
	 * Returns the name of the interface that this service is registered under.
	 * 
	 * @return String containing a fully qualified name of a Java class.
	 */
	public String getInterface();

}
