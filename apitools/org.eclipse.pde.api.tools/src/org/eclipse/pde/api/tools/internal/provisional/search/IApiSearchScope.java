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

import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Describes a scope of API components or elements within API components.
 * A search scope is a class file container. A search scope visits all
 * class files referenced by a scope.
 * 
 * @since 1.0.0
 */
public interface IApiSearchScope extends IClassFileContainer {

	/**
	 * Returns whether this scope encloses the given element in the specified component.
	 * 
	 * @param componentId identifier of the API component the given element is being considered in
	 * @param element element descriptor
	 * @return whether this scope encloses the given element in the specified component
	 */
	public boolean encloses(String componentId, IElementDescriptor element);
	
}
