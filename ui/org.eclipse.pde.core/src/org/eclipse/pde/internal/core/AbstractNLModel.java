/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.IResource;

public abstract class AbstractNLModel extends AbstractModel {

	private static final long serialVersionUID = 1L;
	protected transient NLResourceHelper fNLHelper;

	public NLResourceHelper getNLResourceHelper() {
		if (fNLHelper == null)
			fNLHelper = createNLResourceHelper();
		return fNLHelper;
	}

	public void resetNLResourceHelper() {
		fNLHelper = null;
	}

	public void dispose() {
		if (fNLHelper != null) {
			fNLHelper.dispose();
			fNLHelper = null;
		}
		super.dispose();
	}

	public String getResourceString(String key) {
		if (key == null)
			return ""; //$NON-NLS-1$

		if (fNLHelper == null)
			fNLHelper = createNLResourceHelper();

		return fNLHelper != null ? fNLHelper.getResourceString(key) : key;
	}

	protected abstract NLResourceHelper createNLResourceHelper();

	public Object getAdapter(Class adapter) {
		if (adapter == IResource.class) {
			IResource resource = getUnderlyingResource();
			return resource == null ? null : resource.getProject();
		}
		return super.getAdapter(adapter);
	}

}
