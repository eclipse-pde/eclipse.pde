/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ibundle.*;
import org.osgi.framework.*;

public class BundlePlugin extends BundlePluginBase implements IBundlePlugin {

	private static final long serialVersionUID = 1L;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPlugin#getClassName()
	 */
	public String getClassName() {
		return parseSingleValuedHeader(getClassHeader());
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
			bundle.setHeader(getClassHeader(), className);
			model.fireModelObjectChanged(this, P_CLASS_NAME, old, className);
		}
	}
	private String getClassHeader() {
		boolean compatibilityMode = false;
		IPluginImport[] imports = getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			String id = iimport.getId();
			if (id.equals("org.eclipse.core.runtime.compatibility")) { //$NON-NLS-1$
				compatibilityMode = true;
				break;
			}
		}
		return compatibilityMode ? "Plugin-Class" : Constants.BUNDLE_ACTIVATOR; //$NON-NLS-1$
	}

	public boolean hasExtensibleAPI() {
		return "true".equals(parseSingleValuedHeader("Eclipse-ExtensibleAPI")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}