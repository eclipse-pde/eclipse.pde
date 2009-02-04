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
package org.eclipse.pde.api.tools.internal.provisional.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;

/**
 * An {@link ApiSearchScope} is used  by an {@link IApiSearchRequestor} to denote what
 * {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}s should be 
 * searched for any given invocation of an {@link ApiSearchEngine}
 * 
 * @since 1.0.0
 */
public interface IApiSearchScope {
	
	/**
	 * Returns the {@link IApiElement}s to be considered during a search.
	 * 
	 * @return the {@link IApiElement}s to search
	 */
	public IApiElement[] getScope() throws CoreException;
	
	/**
	 * Returns if this scope encloses the given element
	 * 
	 * @param element
	 * @return true if this scope encloses the given element, false otherwise
	 */
	public boolean encloses(IApiElement element) throws CoreException;
}
