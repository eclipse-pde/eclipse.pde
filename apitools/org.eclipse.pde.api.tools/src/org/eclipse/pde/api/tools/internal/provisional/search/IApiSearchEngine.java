/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Searches an API profile for references to specified elements.
 * 
 * @since 1.0.0
 */
public interface IApiSearchEngine {
		
	/**
	 * Searches for references in the specified scope that meet the given conditions.
	 * Returns results that match each condition, or an empty collection if the search
	 * (progress monitor) is canceled.
	 * 
	 * @param sourceScope the scope searched for references
	 * @param conditions search conditions 
	 * @param monitor progress monitor or <code>null</code>
	 * @return references found, possibly an empty collection
	 * @throws CoreException
	 */
	public IApiSearchResult[] search(IApiSearchScope sourceScope, IApiSearchCriteria[] conditions, IProgressMonitor monitor) throws CoreException;	

	/**
	 * Resolves the given references.
	 * 
	 * @param references
	 * @param monitor
	 * @throws CoreException
	 */
	public void resolveReferences(IReference[] references, IProgressMonitor monitor) throws CoreException;
}
