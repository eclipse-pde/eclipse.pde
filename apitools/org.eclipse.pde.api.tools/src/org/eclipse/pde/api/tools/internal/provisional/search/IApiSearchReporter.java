/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.search;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;

/**
 * Describes a reporter called out to by the {@link ApiSearchEngine} when 
 * a pre-determined set of results have been collected.
 * 
 * @since 1.0.0
 */
public interface IApiSearchReporter {

	/**
	 * Reports the given results to the user (implementation independent)
	 * 
	 * @param element the element that was searched
	 * @param references the raw list of references from the {@link ApiSearchEngine}
	 */
	public void reportResults(IApiElement element, final IReference[] references);
	
	/**
	 * Reports the current listing of objects that were not searched for whatever reason. 
	 * @param notsearched array of elements not searched 
	 */
	public void reportNotSearched(final IApiElement[] elements);
	
	/**
	 * Reports the given metadata object out to the report directory.
	 * Does no work if the metadata is <code>null</code>
	 * @param data the data object to write out
	 */
	public void reportMetadata(IMetadata data);
	
	/**
	 * Reports the current total count of references that have been reported by 
	 * this reporter since its creation.
	 */
	public void reportCounts();
}
