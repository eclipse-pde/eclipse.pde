/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.search.tests;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.search.ApiUseSearchScope;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Test search requestor for the {@link SearchEngineTests}
 * 
 * @since 1.0.1
 */
public class TestRequestor implements IApiSearchRequestor {

	private IApiBaseline scopebaseline = null;
	private int searchmask = 0;
	private HashSet<String> excluded = new HashSet<String>();
	private SearchTest test = null;
	private IApiSearchScope scope = null;
	
	/**
	 * Constructor
	 * @param
	 */
	public TestRequestor(SearchTest test) {
		this.test = test;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptComponent(org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent)
	 */
	public boolean acceptComponent(IApiComponent component) {
		return encloses(component);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptMember(org.eclipse.pde.api.tools.internal.provisional.model.IApiMember)
	 */
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptReference(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return IReference.MASK_REF_ALL & ~IReference.REF_CONSTANTPOOL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#getScope()
	 */
	public IApiSearchScope getScope() {
		if(this.scopebaseline == null) {
			return null;
		}
		if(this.scope == null) {
			try {
				IApiComponent[] comps = this.scopebaseline.getApiComponents();
				ArrayList<IApiComponent> clist = new ArrayList<IApiComponent>(comps.length);
				for (int i = 0; i < comps.length; i++) {
					if(comps[i].isSystemComponent()) {
						//never include system libraries in the tests
						continue;
					}
					if(acceptComponent0(comps[i])) {
						clist.add(comps[i]);
					}
				}
				this.scope = new ApiUseSearchScope(clist.toArray(new IApiComponent[clist.size()]));
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
				!this.excluded.contains(component.getId()) && 
				isApiComponent(component);
	}
	
	/**
	 * Utility method to determine if the given {@link IApiComponent} represents a project that
	 * is API tools enabled
	 * @param component
	 * @return true if the project represented by the given component is API tools enabled false otherwise
	 */
	private boolean isApiComponent(IApiComponent component) {
		if(includesNonApiProjects()) {
			return true;
		}
		if(component instanceof PluginProjectApiComponent) {
			PluginProjectApiComponent comp = (PluginProjectApiComponent) component;
			return comp.hasApiDescription();
		}
		else {
			return Util.isApiToolsComponent(component);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#includesAPI()
	 */
	public boolean includesAPI() {
		return (this.searchmask & INCLUDE_API) > 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#includesInternal()
	 */
	public boolean includesInternal() {
		return (this.searchmask & INCLUDE_INTERNAL) > 0;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#includesNonApiProjects()
	 */
	public boolean includesNonApiProjects() {
		return (this.searchmask & INCLUDE_NON_API_ENABLED_PROJECTS) > 0;
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
			this.excluded = new HashSet<String>();
		}
		else {
			this.excluded = excluded;
		}
	}
}
