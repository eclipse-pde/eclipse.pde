/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.bundle.BundleActivatorHeader;
import org.osgi.framework.Constants;

public class BundlePlugin extends BundlePluginBase implements IBundlePlugin {

	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPlugin#getClassName()
	 */
	@Override
	public String getClassName() {
		return getValue(Constants.BUNDLE_ACTIVATOR, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	@Override
	public void setClassName(String className) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getClassName();
			String classHeader = Constants.BUNDLE_ACTIVATOR;
			IManifestHeader header = bundle.getManifestHeader(classHeader);
			if (header instanceof BundleActivatorHeader)
				((BundleActivatorHeader) header).setClassName(className);
			else
				bundle.setHeader(Constants.BUNDLE_ACTIVATOR, className);
			model.fireModelObjectChanged(this, P_CLASS_NAME, old, className);
		}
	}

	@Override
	public boolean hasExtensibleAPI() {
		return "true".equals(getValue(ICoreConstants.EXTENSIBLE_API, false)); //$NON-NLS-1$ 
	}

}
