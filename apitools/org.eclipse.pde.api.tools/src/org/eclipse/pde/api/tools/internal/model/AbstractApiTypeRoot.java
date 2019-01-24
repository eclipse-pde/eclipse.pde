/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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

import java.util.Collections;
import java.util.Set;

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

	private IApiType fType;

	/**
	 * Constructor
	 *
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param name the name of the type root
	 */
	protected AbstractApiTypeRoot(IApiElement parent, String name) {
		super(parent, IApiElement.API_TYPE_ROOT, name);
	}

	public abstract byte[] getContents() throws CoreException;

	@Override
	public IApiType getStructure() throws CoreException {
		// if exists return
		if (fType != null) {
			return fType;
		}

		ApiModelCache cache = ApiModelCache.getCache();
		IApiComponent comp = getApiComponent();
		IApiType type = null;
		if (comp != null) {
			IApiBaseline baseline = comp.getBaseline();
			type = (IApiType) cache.getElementInfo(baseline.getName(), comp.getSymbolicName(), this.getTypeName(), IApiElement.TYPE);
		}
		if (type == null) {
			type = TypeStructureBuilder.buildTypeStructure(getContents(), getApiComponent(), this);
			if (type == null) {
				return null;
			}
			Set<IApiComponent> apiComponentMultiple = Collections.emptySet();
			if (comp != null) {
				IApiBaseline baseline = comp.getBaseline();
				apiComponentMultiple = baseline.getAllApiComponents(comp.getSymbolicName());
			}
			// cache only if 1 version is there - else optimising would cause
			// issues if both the versions have the same type.
			if (apiComponentMultiple.isEmpty()) {
				cache.cacheElementInfo(type);
			}
		}

		fType = type;
		return fType;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot#getApiComponent()
	 */
	@Override
	public IApiComponent getApiComponent() {
		return (IApiComponent) getAncestor(IApiElement.COMPONENT);
	}
}
