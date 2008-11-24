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
package org.eclipse.pde.api.tools.internal.model;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;

/**
 * A collection of class file containers.
 * 
 * @since 1.0
 */
public class CompositeApiTypeContainer extends AbstractApiTypeContainer {
	
	private List fContainers;
	
	/**
	 * Constructs a composite container on the given list of containers.
	 * 
	 * @param containers list of containers
	 */
	public CompositeApiTypeContainer(IApiElement parent, List containers) {
		super(parent, IApiElement.API_TYPE_CONTAINER, "Composite Class File Container"); //$NON-NLS-1$
		this.fContainers = containers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#createClassFileContainers()
	 */
	protected List createApiTypeContainers() throws CoreException {
		return fContainers;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Composite Class File Container:\n"); //$NON-NLS-1$
		if(fContainers == null) {
			buff.append("\t<EMPTY>"); //$NON-NLS-1$
		}
		else {
			IApiTypeContainer container = null;
			for(Iterator iter = fContainers.iterator(); iter.hasNext();) {
				container = (IApiTypeContainer) iter.next();
				buff.append("\t"+container.toString()); //$NON-NLS-1$
			}
		}
		return buff.toString();
	}
}
