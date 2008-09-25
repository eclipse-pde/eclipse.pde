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
package org.eclipse.pde.api.tools.internal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
 * A collection of class file containers.
 * 
 * @since 1.0
 */
public class CompositeClassFileContainer extends AbstractClassFileContainer {
	
	private List fContainers;
	
	private String fOrigin;
	/**
	 * Constructs a composite container on the given list of containers.
	 * 
	 * @param containers list of containers
	 */
	public CompositeClassFileContainer(List containers, String origin) {
		this.fContainers = containers;
		this.fOrigin = origin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractClassFileContainer#createClassFileContainers()
	 */
	protected List createClassFileContainers() throws CoreException {
		return fContainers;
	}
	
	public String getOrigin() {
		return this.fOrigin;
	}

}
