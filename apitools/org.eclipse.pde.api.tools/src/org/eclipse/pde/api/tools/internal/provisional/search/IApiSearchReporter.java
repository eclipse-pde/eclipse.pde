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
	
}
