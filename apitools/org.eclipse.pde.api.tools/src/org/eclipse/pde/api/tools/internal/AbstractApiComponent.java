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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * Common implementation of an API component as a composite class file container.
 * 
 * @since 1.0.0
 */
public abstract class AbstractApiComponent extends AbstractClassFileContainer implements IApiComponent {
	
	/**
	 * API description
	 */
	private IApiDescription fApiDescription = null;
		
	/**
	 * Api Filter store
	 */
	private IApiFilterStore fFilterStore = null;
	
	/**
	 * Owning profile
	 */
	private IApiBaseline fProfile = null;
	
	/**
	 * Constructs an API component in the given profile.
	 * 
	 * @param profile API profile
	 */
	public AbstractApiComponent(IApiBaseline profile) {
		fProfile = profile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			super.accept(visitor);
		}
		visitor.end(this);
	}	
		
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getProfile()
	 */
	public IApiBaseline getProfile() {
		return fProfile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#dispose()
	 */
	public void dispose() {
		try {
			close();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		fApiDescription = null;
		fProfile = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getApiDescription()
	 */
	public synchronized IApiDescription getApiDescription() throws CoreException {
		if (fApiDescription == null) {
			fApiDescription = createApiDescription();
		}
		return fApiDescription;
	}
	
	/**
	 * Returns whether this component has created an API description.
	 * 
	 * @return whether this component has created an API description
	 */
	protected synchronized boolean isApiDescriptionInitialized() {
		return fApiDescription != null;
	}

	/**
	 * Returns if this component has created an API filter store
	 * 
	 * @return true if a store has been created, false other wise
	 */
	protected synchronized boolean hasApiFilterStore() {
		return fFilterStore != null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#getClassFileContainers()
	 */
	public synchronized IClassFileContainer[] getClassFileContainers() {
		return super.getClassFileContainers();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#getClassFileContainers()
	 */
	public synchronized IClassFileContainer[] getClassFileContainers(String id) {
		if (this.hasFragments()) {
			return super.getClassFileContainers(id);
		} else {
			return super.getClassFileContainers();
		}
	}
	
	/**
	 * Creates and returns the API description for this component.
	 * 
	 * @return newly created API description for this component
	 */
	protected abstract IApiDescription createApiDescription() throws CoreException;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#getFilterStore()
	 */
	public IApiFilterStore getFilterStore() throws CoreException {
		if(fFilterStore == null) {
			fFilterStore = createApiFilterStore();
		}
		return fFilterStore;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#newProblemFilter(org.eclipse.pde.api.tools.internal.provisional.IApiProblem)
	 */
	public IApiProblemFilter newProblemFilter(IApiProblem problem) {
	//TODO either expose a way to make problems or change the method to accept the parts of a problem
		return new ApiProblemFilter(getId(), problem);
	}
	
	/**
	 * Lazily creates a new {@link IApiFilterStore} when it is requested
	 * 
	 * @return the current {@link IApiFilterStore} for this component
	 * @throws CoreException
	 */
	protected abstract IApiFilterStore createApiFilterStore() throws CoreException;
	
}
