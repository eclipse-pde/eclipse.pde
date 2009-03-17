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
package org.eclipse.pde.api.tools.internal.provisional.model;

import org.eclipse.core.runtime.CoreException;

/**
 * Describes a set of {@link IApiElement}.
 * <p>The api elements can be of the following types:</p>
 * <ul>
 * <li>{@link IApiElement#BASELINE}</li>
 * <li>{@link IApiElement#COMPONENT}</li>
 * <li>{@link IApiElement#API_TYPE_CONTAINER}</li>
 * <li>{@link IApiElement#API_TYPE_ROOT}</li>
 * </ul>
 * 
 * @since 1.1.0
 */
public interface IApiScope {
	/**
	 * Returns all API elements contained within this scope
	 * 
	 * @return all API elements contained within this scope
	 */
	IApiElement[] getApiElements();
	
	/**
	 * Visits all {@link IApiElement} in this scope.
	 * 
	 * @param visitor class file visitor.
	 * @exception CoreException if unable to visit this scope
	 */
	void accept(ApiScopeVisitor visitor) throws CoreException;
}
