/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
		if (fNLHelper == null) {
			fNLHelper = createNLResourceHelper();
		}
		return fNLHelper;
	}

	public void resetNLResourceHelper() {
		fNLHelper = null;
	}

	@Override
	public void dispose() {
		if (fNLHelper != null) {
			fNLHelper.dispose();
			fNLHelper = null;
		}
		super.dispose();
	}

	@Override
	public String getResourceString(String key) {
		if (key == null) {
			return ""; //$NON-NLS-1$
		}

		if (fNLHelper == null) {
			fNLHelper = createNLResourceHelper();
		}

		return fNLHelper != null ? fNLHelper.getResourceString(key) : key;
	}

	protected abstract NLResourceHelper createNLResourceHelper();

	@Override
	@SuppressWarnings("unchecked")
	public <T> T  getAdapter(Class<T> adapter) {
		if (adapter == IResource.class) {
			IResource resource = getUnderlyingResource();
			return resource == null ? null : (T)resource.getProject();
		}
		return super.getAdapter(adapter);
	}

}
