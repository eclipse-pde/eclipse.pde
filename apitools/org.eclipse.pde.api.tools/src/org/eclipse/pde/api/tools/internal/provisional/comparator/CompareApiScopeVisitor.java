/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiScopeVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * ApiScope visitor implementation to run the comparison on all elements of the scope.
 */
public class CompareApiScopeVisitor extends ApiScopeVisitor {

	Set deltas;
	IApiBaseline referenceBaseline;
	int visibilityModifiers;
	boolean containsErrors = false;

	public CompareApiScopeVisitor(final Set deltas, final IApiBaseline baseline, final int visibilityModifiers) {
		this.deltas = deltas;
		this.referenceBaseline = baseline;
		this.visibilityModifiers = visibilityModifiers;
	}
	
	public boolean visit(IApiBaseline baseline) throws CoreException {
		IDelta delta = ApiComparator.compare(this.referenceBaseline, baseline, this.visibilityModifiers);
		if (delta != null) {
			delta.accept(new DeltaVisitor() {
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

	public boolean visit(IApiTypeContainer container) throws CoreException {
		container.accept(new ApiTypeContainerVisitor() {
			public void visit(String packageName, IApiTypeRoot typeroot) {
				try {
					CompareApiScopeVisitor.this.visit(typeroot);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		});
		return false;
	}

	public boolean visit(IApiComponent component) throws CoreException {
		if (component.getErrors() != null) {
			this.containsErrors = true;
			return false;
		}
		IApiComponent referenceComponent = this.referenceBaseline.getApiComponent(component.getId());
		if (referenceComponent.getErrors() != null) {
			this.containsErrors = true;
			return false;
		}
		IDelta delta = ApiComparator.compare(referenceComponent, component, this.visibilityModifiers);
		if (delta != null) {
			delta.accept(new DeltaVisitor() {
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
	
	public void visit(IApiTypeRoot root) throws CoreException {
		IApiComponent apiComponent = root.getApiComponent();
		if (apiComponent == null) {
			return;
		}
		if (apiComponent.getErrors() != null) {
			this.containsErrors = true;
			return;
		}
		IApiComponent referenceComponent = this.referenceBaseline.getApiComponent(apiComponent.getId());
		if (referenceComponent == null) return;
		if (referenceComponent.getErrors() != null) {
			this.containsErrors = true;
			return;
		}
		IApiBaseline baseline = referenceComponent.getBaseline();
		IDelta delta = ApiComparator.compare(
				root,
				apiComponent,
				referenceComponent,
				this.referenceBaseline,
				baseline,
				this.visibilityModifiers);
		if (delta != null) {
			delta.accept(new DeltaVisitor() {
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
