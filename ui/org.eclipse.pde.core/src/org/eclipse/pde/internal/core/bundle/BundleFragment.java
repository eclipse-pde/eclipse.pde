/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.osgi.framework.*;

public class BundleFragment extends BundlePluginBase implements IBundleFragment {

	private static final long serialVersionUID = 1L;

	public String getPluginId() {
		return parseSingleValuedHeader(Constants.FRAGMENT_HOST);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		String version = getAttribute(Constants.FRAGMENT_HOST, Constants.BUNDLE_VERSION_ATTRIBUTE);
		try {
			VersionRange versionRange = new VersionRange(version);
			if (versionRange != null) {
				return versionRange.getMinimum() != null ? versionRange.getMinimum().toString() : version;
			}
		} catch (NumberFormatException e) {
		}
		return version;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getRule()
	 */
	public int getRule() {
		String version = getAttribute(Constants.FRAGMENT_HOST, Constants.BUNDLE_VERSION_ATTRIBUTE);
		VersionRange versionRange = new VersionRange(version);
		return PluginBase.getMatchRule(versionRange);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginId(java.lang.String)
	 */
	public void setPluginId(String id) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String oldValue = getPluginId();
			bundle.setHeader(Constants.FRAGMENT_HOST, writeFragmentHost(id, getPluginVersion()));
			model.fireModelObjectChanged(this, P_PLUGIN_ID, oldValue, id);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String oldValue = getPluginVersion();
			bundle.setHeader(Constants.FRAGMENT_HOST, writeFragmentHost(getPluginId(), version));
			model.fireModelObjectChanged(this, P_PLUGIN_VERSION, oldValue, version);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
	}
	
	private String writeFragmentHost(String id, String version) {
		StringBuffer buffer = new StringBuffer();
		if (id != null)
			buffer.append(id);
		
		if (version != null && version.trim().length() > 0) {
			buffer.append(";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + version.trim() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return buffer.toString();
	}
}
