/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * Common implementation of an {@link IApiTypeContainer}
 *
 * @since 1.0.0
 */
public abstract class AbstractApiTypeContainer extends ApiElement implements IApiTypeContainer {

	/**
	 * Collection of {@link IApiTypeContainer}s
	 */
	private List<IApiTypeContainer> fApiTypeContainers = null;

	/**
	 * Constructor
	 *
	 * @param parent the parent {@link IApiElement} or <code>null</code> if
	 *            none.
	 * @param type the type of the container
	 * @param name the name
	 */
	protected AbstractApiTypeContainer(IApiElement parent, int type, String name) {
		super(parent, type, name);
	}

	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (IApiTypeContainer container : containers) {
			container.accept(visitor);
		}
	}

	@Override
	public synchronized void close() throws CoreException {
		if (fApiTypeContainers == null) {
			return;
		}
		// clean component cache elements
		ApiModelCache.getCache().removeElementInfo(this);

		MultiStatus multi = null;
		IStatus single = null;
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (IApiTypeContainer container : containers) {
			try {
				container.close();
			} catch (CoreException e) {
				if (single == null) {
					single = e.getStatus();
				} else {
					if (multi == null) {
						multi = new MultiStatus(ApiPlugin.PLUGIN_ID, single.getCode(), single.getMessage(), single.getException());
					}
					multi.add(e.getStatus());
				}
			}
		}
		if (multi != null) {
			throw new CoreException(multi);
		}
		if (single != null) {
			throw new CoreException(single);
		}
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String)
	 */
	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (IApiTypeContainer container : containers) {
			IApiTypeRoot file = container.findTypeRoot(qualifiedName);
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		IApiTypeContainer[] containers = getApiTypeContainers();
		String origin = null;
		IApiComponent comp = null;
		for (IApiTypeContainer container : containers) {
			comp = (IApiComponent) container.getAncestor(IApiElement.COMPONENT);
			if (comp != null) {
				origin = comp.getSymbolicName();
				// comp is the fragment - get the origin of root host
				if (origin != null && !origin.equals(id)) {
					if (comp.isFragment()) {
						IApiComponent rootComp = comp;
						while (rootComp != null && rootComp.isFragment()) {
							rootComp = rootComp.getHost();
						}
						if (rootComp != null) {
							origin = rootComp.getSymbolicName();
						}
					}

				}
			}
			if (origin == null) {
				IApiTypeRoot file = container.findTypeRoot(qualifiedName);
				if (file != null) {
					return file;
				}
			} else if (origin.equals(id)) {
				IApiTypeRoot file = container.findTypeRoot(qualifiedName, id);
				if (file != null) {
					return file;
				}
			}
		}
		return null;
	}

	@Override
	public String[] getPackageNames() throws CoreException {
		List<String> names = new ArrayList<>();
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (IApiTypeContainer container : containers) {
			String[] packageNames = container.getPackageNames();
			for (String packageName : packageNames) {
				names.add(packageName);
			}
		}
		String[] result = new String[names.size()];
		names.toArray(result);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Returns the {@link IApiTypeContainer}s in this container. Creates the
	 * containers if they are not yet created.
	 *
	 * @return the {@link IApiTypeContainer}s
	 */
	protected synchronized IApiTypeContainer[] getApiTypeContainers() throws CoreException {
		if (fApiTypeContainers == null) {
			fApiTypeContainers = createApiTypeContainers();
		}
		return fApiTypeContainers.toArray(new IApiTypeContainer[fApiTypeContainers.size()]);
	}

	/**
	 * Returns the {@link IApiTypeContainer}s in this container. Creates the
	 * containers if they are not yet created.
	 *
	 * @param id the given id
	 * @return the {@link IApiTypeContainer}s
	 */
	protected synchronized IApiTypeContainer[] getApiTypeContainers(String id) throws CoreException {
		if (fApiTypeContainers == null) {
			fApiTypeContainers = createApiTypeContainers();
		}
		List<IApiTypeContainer> containers = new ArrayList<>();
		String origin = null;
		IApiTypeContainer container = null;
		for (Iterator<IApiTypeContainer> iterator = fApiTypeContainers.iterator(); iterator.hasNext();) {
			container = iterator.next();
			origin = ((IApiComponent) container.getAncestor(IApiElement.COMPONENT)).getSymbolicName();
			if (origin != null && origin.equals(id)) {
				containers.add(container);
			}
		}
		return containers.toArray(new IApiTypeContainer[containers.size()]);
	}

	/**
	 * Creates and returns the {@link IApiTypeContainer}s for this component.
	 * Subclasses must override.
	 *
	 * @return list of {@link IApiTypeContainer}s for this component
	 */
	protected abstract List<IApiTypeContainer> createApiTypeContainers() throws CoreException;

	/**
	 * Sets the {@link IApiTypeContainer}s in this container.
	 *
	 * @param containers the {@link IApiTypeContainer}s to set
	 */
	protected synchronized void setApiTypeContainers(IApiTypeContainer[] containers) {
		if (fApiTypeContainers != null) {
			try {
				close();
			} catch (CoreException e) {
				// TODO log error
			}
			fApiTypeContainers.clear();
		} else {
			fApiTypeContainers = new ArrayList<>(containers.length);
		}
		for (IApiTypeContainer container : containers) {
			fApiTypeContainers.add(container);
		}
	}

	@Override
	public int getContainerType() {
		return 0;
	}

}
