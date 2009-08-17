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
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;


public class SkippedComponent implements IApiElement{
	/**
	 * If the skipped component has no .api_description
	 */
	private boolean noapidescription = false;
	/**
	 * If the skipped component was skipped because it was found in an exclude list
	 */
	private boolean inexcludelist = false;
	/**
	 * If the skipped component has resolution errors
	 */
	private boolean resolveerrors = false;
	/**
	 * the id of of the skipped component
	 */
	private String componentid;
	/**
	 * the set of resolution errors barring the component from being scanned
	 */
	private ResolverError[] errors = null;

	/**
	 * Constructor
	 * @param noapidescription
	 * @param inexcludelist
	 * @param componentid
	 * @param errors the {@link ResolverError}s, if any, that prevented this component from being scanned
	 */
	public SkippedComponent(String componentid, boolean noapidescription, boolean inexcludelist, boolean resolveerrors, ResolverError[] errors) {
		this.noapidescription = noapidescription;
		this.inexcludelist = inexcludelist;
		this.resolveerrors = resolveerrors;
		this.componentid = componentid;
		this.errors = errors;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof SkippedComponent) {
			return this.componentid.equals(((SkippedComponent)obj).componentid);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.componentid.hashCode();
	}
	
	/**
	 * @return the component id of the skipped component
	 */
	public String getComponentId() {
		return this.componentid;
	}
	
	/**
	 * @return true if the the skipped component has no .api_description file
	 */
	public boolean hasNoApiDescription() {
		return this.noapidescription;
	}
	
	/**
	 * @return true if the component was skipped because it appeared in an exclude list
	 */
	public boolean wasExcluded() {
		return this.inexcludelist;
	}
	
	/**
	 * @return true if the the component had resolution errors
	 */
	public boolean hasResolutionErrors() {
		return this.resolveerrors;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getAncestor(int)
	 */
	public IApiElement getAncestor(int ancestorType) {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getApiComponent()
	 */
	public IApiComponent getApiComponent() {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getName()
	 */
	public String getName() {
		return this.componentid;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getParent()
	 */
	public IApiElement getParent() {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getType()
	 */
	public int getType() {
		return IApiElement.COMPONENT;
	}
	
	/**
	 * @return the errors
	 */
	public ResolverError[] getErrors() {
		return this.errors;
	}
}