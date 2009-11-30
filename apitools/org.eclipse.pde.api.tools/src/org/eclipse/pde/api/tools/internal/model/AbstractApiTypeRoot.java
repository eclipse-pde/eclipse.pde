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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * Common implementation for {@link IApiTypeRoot}
 * 
 * @since 1.0.0
 */
public abstract class AbstractApiTypeRoot extends ApiElement implements IApiTypeRoot {

	/**
	 * Constructor
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param name the name of the type root
	 */
	protected AbstractApiTypeRoot(IApiElement parent, String name) {
		super(parent, IApiElement.API_TYPE_ROOT, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot#getContents()
	 */
	public abstract byte[] getContents() throws CoreException;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot#getStructure()
	 */
	public IApiType getStructure() throws CoreException {
		ApiModelCache cache = ApiModelCache.getCache();
		IApiComponent comp = getApiComponent();
		IApiType type = null;
		if(comp != null) {
			IApiBaseline baseline = comp.getBaseline();
			type = (IApiType) cache.getElementInfo(baseline.getName(), comp.getSymbolicName(), this.getTypeName(), IApiElement.TYPE);
		}
		if(type == null) {
			type = TypeStructureBuilder.buildTypeStructure(getContents(), getApiComponent(), this);
			if(type == null) {
				return null;
			}
			cache.cacheElementInfo(type);
		}
		return type;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot#getApiComponent()
	 */
	public IApiComponent getApiComponent() {
		return (IApiComponent) getAncestor(IApiElement.COMPONENT);
	}
}
