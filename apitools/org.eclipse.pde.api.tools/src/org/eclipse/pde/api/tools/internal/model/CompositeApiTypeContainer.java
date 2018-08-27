/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	private List<IApiTypeContainer> fContainers;

	/**
	 * Constructs a composite container on the given list of containers.
	 *
	 * @param containers list of containers
	 */
	public CompositeApiTypeContainer(IApiElement parent, List<IApiTypeContainer> containers) {
		super(parent, IApiElement.API_TYPE_CONTAINER, "Composite Class File Container"); //$NON-NLS-1$
		this.fContainers = containers;
	}

	@Override
	protected List<IApiTypeContainer> createApiTypeContainers() throws CoreException {
		return fContainers;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("Composite Class File Container:\n"); //$NON-NLS-1$
		if (fContainers == null) {
			buff.append("\t<EMPTY>"); //$NON-NLS-1$
		} else {
			IApiTypeContainer container = null;
			for (Iterator<IApiTypeContainer> iter = fContainers.iterator(); iter.hasNext();) {
				container = iter.next();
				buff.append("\t" + container.toString()); //$NON-NLS-1$
			}
		}
		return buff.toString();
	}
}
