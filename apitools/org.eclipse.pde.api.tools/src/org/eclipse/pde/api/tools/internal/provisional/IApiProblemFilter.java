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
package org.eclipse.pde.api.tools.internal.provisional;


/**
 * Describes API problems that should be filtered. 
 * <p>
 * Problem filters are created from an {@link IApiComponent}.
 * </p>
 * @since 1.0.0
 */
public interface IApiProblemFilter {

	/**
	 * Returns the identifier of the API component this filter applies to. Problems
	 * contained within this component are potentially filtered.
	 * 
	 * @return identifier of the API component this filter applies to
	 */
	public String getComponentId();
	
	/**
	 * Returns the underlying {@link IApiProblem} for this filter.
	 * 
	 * @return the underlying {@link IApiProblem} for this filter
	 */
	public IApiProblem getUnderlyingProblem();
}
