/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.search.IReferenceCollection;
import org.eclipse.pde.api.tools.internal.search.UseScanReferences;

/**
 * Common implementation of an API component as a composite class file
 * container.
 *
 * @since 1.0.0
 */
public abstract class Component extends AbstractApiTypeContainer implements IApiComponent {
	/**
	 * API description
	 */
	private volatile IApiDescription fApiDescription;

	/**
	 * API Filter store
	 */
	private volatile IApiFilterStore fFilterStore;

	/**
	 * References in API use scan reports
	 */
	private volatile IReferenceCollection fReferences;

	private volatile boolean disposed;

	/**
	 * Constructs an API component in the given {@link IApiBaseline}.
	 *
	 * @param baseline the parent {@link IApiBaseline}
	 */
	public Component(IApiBaseline baseline) {
		super(baseline, IApiElement.COMPONENT, null);
	}

	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			super.accept(visitor);
		}
		visitor.end(this);
	}

	@Override
	public IApiComponent getHost() throws CoreException {
		return null;
	}

	@Override
	public IApiBaseline getBaseline() {
		return (IApiBaseline) getAncestor(IApiElement.BASELINE);
	}

	@Override
	public void dispose() {
		try {
			close();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} finally {
			synchronized (this) {
				fApiDescription = null;
				disposed = true;
			}
		}
	}

	@Override
	public IApiComponent getApiComponent() {
		return this;
	}

	@Override
	public IApiDescription getApiDescription() throws CoreException {
		if (fApiDescription != null) {
			return fApiDescription;
		}
		synchronized (this) {
			fApiDescription = createApiDescription();
		}
		return fApiDescription;
	}

	/**
	 * Returns whether this component has created an API description.
	 *
	 * @return whether this component has created an API description
	 */
	protected boolean isApiDescriptionInitialized() {
		return fApiDescription != null;
	}

	/**
	 * Returns if this component has created an API filter store
	 *
	 * @return true if a store has been created, false other wise
	 */
	protected boolean hasApiFilterStore() {
		return fFilterStore != null;
	}

	@Override
	public IApiTypeContainer[] getApiTypeContainers() throws CoreException {
		return super.getApiTypeContainers();
	}

	@Override
	public IApiTypeContainer[] getApiTypeContainers(String id) throws CoreException {
		if (this.hasFragments()) {
			return super.getApiTypeContainers(id);
		} else {
			return super.getApiTypeContainers();
		}
	}

	/**
	 * Creates and returns the API description for this component.
	 *
	 * @return newly created API description for this component
	 */
	protected abstract IApiDescription createApiDescription() throws CoreException;

	@Override
	public IApiFilterStore getFilterStore() throws CoreException {
		if (fFilterStore == null) {
			synchronized (this) {
				if (fFilterStore == null) {
					fFilterStore = createApiFilterStore();
				}
			}
		}
		return fFilterStore;
	}

	@Override
	public IElementDescriptor getHandle() {
		return Factory.componentDescriptor(this.getSymbolicName(), this.getVersion());
	}

	@Override
	public int getContainerType() {
		return IApiTypeContainer.COMPONENT;
	}

	/**
	 * Lazily creates a new {@link IApiFilterStore} when it is requested
	 *
	 * @return the current {@link IApiFilterStore} for this component
	 */
	protected abstract IApiFilterStore createApiFilterStore() throws CoreException;

	@Override
	public IReferenceCollection getExternalDependencies() {
		if (fReferences == null) {
			synchronized (this) {
				if (fReferences == null) {
					fReferences = new UseScanReferences();
				}
			}
		}
		return fReferences;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}
}
