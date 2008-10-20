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
package org.eclipse.pde.api.tools.internal.provisional.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Analyzes references in a scope for use and leak problems.
 * 
 * @since 1.1
 */
public interface IReferenceAnalyzer {

	/**
	 * Analyzes references in the specified scope for problems.
	 * Returns any problems, or an empty collection if the search
	 * (progress monitor) is canceled.
	 * 
	 * @param component the component being analyzed
	 * @param sourceScope the scope within the component to analyze
	 * @param monitor progress monitor or <code>null</code>
	 * @return problems found, possibly an empty collection
	 * @throws CoreException
	 */
	public IApiProblem[] analyze(IApiComponent component, IApiSearchScope sourceScope, IProgressMonitor monitor) throws CoreException;		
}
