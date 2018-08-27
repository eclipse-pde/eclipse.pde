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
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiScopeVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * ApiScope visitor implementation to run the comparison on all elements of the
 * scope.
 */
public class CompareApiScopeVisitor extends ApiScopeVisitor {

	Set<IDelta> deltas;
	IApiBaseline referenceBaseline;
	int visibilityModifiers;
	boolean force;
	boolean continueOnResolverError = false;
	boolean containsErrors = false;
	SubMonitor monitor;

	public CompareApiScopeVisitor(final Set<IDelta> deltas, final IApiBaseline baseline, final boolean force, final int visibilityModifiers, final boolean continueOnResolverError, final SubMonitor monitor) {
		this.deltas = deltas;
		this.referenceBaseline = baseline;
		this.visibilityModifiers = visibilityModifiers;
		this.force = force;
		this.continueOnResolverError = continueOnResolverError;
		this.monitor = monitor;
	}

	@Override
	public boolean visit(IApiBaseline baseline) throws CoreException {
		this.monitor.setWorkRemaining(100).split(1);
		IDelta delta = ApiComparator.compare(this.referenceBaseline, baseline, this.visibilityModifiers, this.force, null);
		if (delta != null) {
			delta.accept(new DeltaVisitor() {
				@Override
				public void endVisit(IDelta localDelta) {
					if (localDelta.getChildren().length == 0) {
						CompareApiScopeVisitor.this.deltas.add(localDelta);
					}
				}
			});
		} else {
			this.containsErrors = true;
		}
		return false;
	}

	@Override
	public boolean visit(IApiTypeContainer container) throws CoreException {
		SubMonitor localMonitor = this.monitor.setWorkRemaining(100).split(1);
		container.accept(new ApiTypeContainerVisitor() {
			@Override
			public void visit(String packageName, IApiTypeRoot typeroot) {
				localMonitor.setWorkRemaining(100).split(1);
				try {
					compareApiTypeRoot(typeroot);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		});
		return false;
	}

	@Override
	public boolean visit(IApiComponent component) throws CoreException {
		SubMonitor subMonitor = this.monitor.setWorkRemaining(100).split(1).setWorkRemaining(100);
		if (component.isSourceComponent() || component.isSystemComponent()) {
			return false;
		}
		if (component.getErrors() != null) {
			this.containsErrors = true;
			if (!continueOnResolverError) {
				return false;
			}
		}
		IApiComponent referenceComponent = this.referenceBaseline.getApiComponent(component.getSymbolicName());
		// referenceComponent can be null if this is an added component
		if (referenceComponent != null && referenceComponent.getErrors() != null) {
			this.containsErrors = true;
			if (!continueOnResolverError) {
				return false;
			}
		}

		subMonitor.split(50);
		final Delta globalDelta = new Delta();
		globalDelta.add(ApiComparator.compare(referenceComponent, component, this.visibilityModifiers, null));
		if (referenceComponent != null) {
			String versionString = referenceComponent.getVersion();
			String versionString2 = component.getVersion();
			IDelta bundleVersionChangesDelta = ApiComparator.checkBundleVersionChanges(component, referenceComponent.getSymbolicName(), versionString, versionString2);
			if (bundleVersionChangesDelta != null) {
				globalDelta.add(bundleVersionChangesDelta);
			}
		}
		globalDelta.accept(new DeltaVisitor() {
			@Override
			public void endVisit(IDelta localDelta) {
				if (localDelta.getChildren().length == 0) {
					CompareApiScopeVisitor.this.deltas.add(localDelta);
				}
			}
		});
		return false;
	}

	@Override
	public void visit(IApiTypeRoot root) throws CoreException {
		this.monitor.setWorkRemaining(100).split(1);
		compareApiTypeRoot(root);
	}

	void compareApiTypeRoot(IApiTypeRoot root) throws CoreException {
		IApiComponent apiComponent = root.getApiComponent();
		if (apiComponent == null || apiComponent.isSystemComponent() || apiComponent.isSourceComponent()) {
			return;
		}
		if (apiComponent.getErrors() != null) {
			this.containsErrors = true;
			if (!continueOnResolverError) {
				return;
			}
		}
		IApiComponent referenceComponent = this.referenceBaseline.getApiComponent(apiComponent.getSymbolicName());
		if (referenceComponent == null) {
			return;
		}
		if (referenceComponent.getErrors() != null) {
			this.containsErrors = true;
			if (!continueOnResolverError) {
				return;
			}
		}
		IApiBaseline baseline = referenceComponent.getBaseline();
		IDelta delta = ApiComparator.compare(root, referenceComponent, apiComponent, null, this.referenceBaseline, baseline, this.visibilityModifiers, null);
		if (delta != null) {
			delta.accept(new DeltaVisitor() {
				@Override
				public void endVisit(IDelta localDelta) {
					if (localDelta.getChildren().length == 0) {
						CompareApiScopeVisitor.this.deltas.add(localDelta);
					}
				}
			});
		} else {
			this.containsErrors = true;
		}
	}

	public boolean containsError() {
		return this.containsErrors;
	}
}
