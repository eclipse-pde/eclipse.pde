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
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePlugin;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleActivatorHeader;
import org.osgi.framework.Constants;

public class BundlePlugin extends BundlePluginBase implements IBundlePlugin {

	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPlugin#getClassName()
	 */
	public String getClassName() {
		return getValue(getClassHeader(), false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	public void setClassName(String className) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getClassName();
			String classHeader = getClassHeader();
			IManifestHeader header = bundle.getManifestHeader(classHeader);
			if (header instanceof BundleActivatorHeader)
				((BundleActivatorHeader) header).setClassName(className);
			else
				bundle.setHeader(getClassHeader(), className);
			model.fireModelObjectChanged(this, P_CLASS_NAME, old, className);
		}
	}

	private String getClassHeader() {
		IPluginImport[] imports = getImports();
		for (int i = 0; i < imports.length; i++) {
			if ("org.eclipse.core.runtime.compatibility".equals(imports[i].getId()))//$NON-NLS-1$
				return ICoreConstants.PLUGIN_CLASS;
		}
		return Constants.BUNDLE_ACTIVATOR;
	}

	public boolean hasExtensibleAPI() {
		return "true".equals(getValue(ICoreConstants.EXTENSIBLE_API, false)); //$NON-NLS-1$ 
	}

}
