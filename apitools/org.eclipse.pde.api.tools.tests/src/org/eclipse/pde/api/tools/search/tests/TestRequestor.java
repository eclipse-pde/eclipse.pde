/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.search.tests;

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Test search requestor for the {@link SearchEngineTests}
 *
 * @since 1.0.1
 */
public class TestRequestor implements IApiSearchRequestor {

	private IApiBaseline scopebaseline = null;
	private int searchmask = 0;
	private HashSet<String> excluded = new HashSet<>();
	private SearchTest test = null;
	private IApiScope scope = null;

	public TestRequestor(SearchTest test) {
		this.test = test;
	}

	@Override
	public boolean acceptComponent(IApiComponent component) {
		return encloses(component);
	}

	@Override
	public boolean acceptContainer(IApiTypeContainer container) {
		return true;
	}

	@Override
	public boolean acceptMember(IApiMember member) {
		return encloses(member);
	}

	/**
	 * Returns if the scope encloses the element
	 * @param element
	 * @return true if the scope encloses the element false otherwise
	 */
	private boolean encloses(IApiElement element) {
		try {
			return this.scope != null && this.scope.encloses(element);
		}
		catch(CoreException ce) {
			this.test.reportFailure(ce.getMessage());
		}
		return false;
	}

	@Override
	public boolean acceptReference(IReference reference) {
		try {
			IApiMember member = reference.getResolvedReference();
			if(member != null) {
				IApiComponent component = member.getApiComponent();
				if(!encloses(component)) {
					return false;
				}
				if(this.searchmask > 0) {
					if(includesAPI() && includesInternal()) {
						return true;
					}
					IApiAnnotations annots = component.getApiDescription().resolveAnnotations(member.getHandle());
					if(annots != null) {
						int vis = annots.getVisibility();
						if(VisibilityModifiers.isAPI(vis) && includesAPI()) {
							return true;
						}
						else if(VisibilityModifiers.isPrivate(vis) && includesInternal()) {
							return true;
						}
					}
				}
			}
		}
		catch(CoreException ce) {
			this.test.reportFailure(ce.getMessage());
		}
		return false;
	}

	@Override
	public int getReferenceKinds() {
		return IReference.MASK_REF_ALL & ~IReference.REF_CONSTANTPOOL;
	}

	@Override
	public IApiScope getScope() {
		if(this.scopebaseline == null) {
			return null;
		}
		if(this.scope == null) {
			try {
				IApiComponent[] comps = this.scopebaseline.getApiComponents();
				this.scope = new ApiScope();
				for (int i = 0; i < comps.length; i++) {
					if(comps[i].isSystemComponent()) {
						//never include system libraries in the tests
						continue;
					}
					if(acceptComponent0(comps[i])) {
						this.scope.addElement(comps[i]);
					}
				}
			}
			catch(Exception e) {
				this.test.reportFailure(e.getMessage());
			}
		}
		return this.scope;
	}

	/**
	 * Checks the given {@link IApiComponent} to see if we allow it to appear in the scope or not
	 * @param component
	 * @return true if the given component should be allowed in the scope false otherwise
	 * @throws CoreException
	 */
	private boolean acceptComponent0(IApiComponent component) throws CoreException {
		return component != null &&
				!this.excluded.contains(component.getSymbolicName()) &&
				isApiComponent(component);
	}

	/**
	 * Utility method to determine if the given {@link IApiComponent} represents a project that
	 * is API tools enabled
	 * @param component
	 * @return true if the project represented by the given component is API tools enabled false otherwise
	 */
	private boolean isApiComponent(IApiComponent component) {
		if(component instanceof ProjectComponent) {
			ProjectComponent comp = (ProjectComponent) component;
			return comp.hasApiDescription();
		}
		else {
			return Util.isApiToolsComponent(component);
		}
	}

	@Override
	public boolean includesAPI() {
		return (this.searchmask & INCLUDE_API) > 0;
	}

	@Override
	public boolean includesInternal() {
		return (this.searchmask & INCLUDE_INTERNAL) > 0;
	}

	@Override
	public boolean includesIllegalUse() {
		return (this.searchmask & INCLUDE_ILLEGAL_USE) > 0;
	}
	/**
	 * Sets the {@link IApiBaseline} to derive the scope from
	 * @param baseline
	 */
	void setScopeBaseline(IApiBaseline baseline) {
		this.scopebaseline = baseline;
	}

	/**
	 * Sets the search kinds to use
	 * @param searchmask
	 */
	void setSearchMask(int searchmask) {
		this.searchmask = searchmask;
	}

	/**
	 * Sets the listing of excluded elements to use
	 * @param excluded
	 */
	void setExcludedElements(HashSet<String> excluded) {
		if(excluded == null) {
			this.excluded = new HashSet<>();
		}
		else {
			this.excluded = excluded;
		}
	}
}
