/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	private List fApiTypeContainers = null;	

	/**
	 * Constructor
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none.
	 * @param type the type of the container
	 * @param name the name
	 */
	protected AbstractApiTypeContainer(IApiElement parent, int type, String name) {
		super(parent, type, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (int i = 0; i < containers.length; i++) {
			containers[i].accept(visitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#close()
	 */
	public synchronized void close() throws CoreException {
		if (fApiTypeContainers == null) {
			return;
		}
		//clean component cache elements
		ApiModelCache.getCache().removeElementInfo(this);
		
		MultiStatus multi = null;
		IStatus single = null;
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (int i = 0; i < containers.length; i++) {
			try {
				containers[i].close();
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
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (int i = 0; i < containers.length; i++) {
			IApiTypeRoot file = containers[i].findTypeRoot(qualifiedName);
			if (file != null) {
				return file;
			}
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String, java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		IApiTypeContainer[] containers = getApiTypeContainers();
		String origin = null;
		IApiComponent comp = null;
		for (int i = 0; i < containers.length; i++) {
			comp = (IApiComponent) containers[i].getAncestor(IApiElement.COMPONENT);
			if(comp != null) {
				origin = comp.getSymbolicName();
			}
			if (origin == null) {
				IApiTypeRoot file = containers[i].findTypeRoot(qualifiedName);
				if (file != null) {
					return file;
				}
			} else if (origin.equals(id)) {
				IApiTypeRoot file = containers[i].findTypeRoot(qualifiedName, id);
				if (file != null) {
					return file;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		List names = new ArrayList();
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (int i = 0, max = containers.length; i < max; i++) {
			String[] packageNames = containers[i].getPackageNames();
			for (int j = 0, max2 = packageNames.length; j < max2; j++) {
				names.add(packageNames[j]);
			}
		}
		String[] result = new String[names.size()];
		names.toArray(result);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Returns the {@link IApiTypeContainer}s in this container. Creates the containers if
	 * they are not yet created.
	 * 
	 * @return the {@link IApiTypeContainer}s
	 */
	protected synchronized IApiTypeContainer[] getApiTypeContainers() throws CoreException {
		if (fApiTypeContainers == null) {
			fApiTypeContainers = createApiTypeContainers();
		}
		return (IApiTypeContainer[]) fApiTypeContainers.toArray(new IApiTypeContainer[fApiTypeContainers.size()]);
	}
	
	/**
	 * Returns the {@link IApiTypeContainer}s in this container. Creates the containers if
	 * they are not yet created.
	 * 
	 * @param id the given id
	 * @return the {@link IApiTypeContainer}s
	 */
	protected synchronized IApiTypeContainer[] getApiTypeContainers(String id) throws CoreException {
		if (fApiTypeContainers == null) {
			fApiTypeContainers = createApiTypeContainers();
		}
		List containers = new ArrayList();
		String origin = null;
		IApiTypeContainer container = null;
		for (Iterator iterator = this.fApiTypeContainers.iterator(); iterator.hasNext(); ) {
			container = (IApiTypeContainer) iterator.next();
			origin = ((IApiComponent)container.getAncestor(IApiElement.COMPONENT)).getSymbolicName();
			if (origin != null && origin.equals(id)) {
				containers.add(container);
			}
		}
		return (IApiTypeContainer[]) containers.toArray(new IApiTypeContainer[containers.size()]);
	}

	/**
	 * Creates and returns the {@link IApiTypeContainer}s for this component.
	 * Subclasses must override.
	 * 
	 * @return list of {@link IApiTypeContainer}s for this component
	 */
	protected abstract List createApiTypeContainers() throws CoreException;

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
			fApiTypeContainers = new ArrayList(containers.length);
		}
		for (int i = 0; i < containers.length; i++) {
			fApiTypeContainers.add(containers[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer#getContainerType()
	 */
	public int getContainerType() {
		return 0;
	}

}
