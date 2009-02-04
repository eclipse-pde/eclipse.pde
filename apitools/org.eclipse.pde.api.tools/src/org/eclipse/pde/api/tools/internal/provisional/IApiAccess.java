/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

/**
 * This interface describes a type of access to packages in a bundle.
 * <br>
 * An example type of access would be friend access to an internal package
 * or SPI access to an API package.
 * 
 * @since 1.0.1
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IApiAccess {
	
	/**
	 * Describes the case where a bundle has 'normal access' to packages from
	 * another bundle.
	 */
	public static final int NORMAL = 0;
	
	/**
	 * Describes an access level for the <code>x-friends</code> directive from a manifest file
	 */
	public static final int FRIEND = 1;
	
	/**
	 * Returns the access level.
	 * Will be one of the constants defined in {@link IApiAccess}.
	 * @return the access level
	 */
	public int getAccessLevel();

}
