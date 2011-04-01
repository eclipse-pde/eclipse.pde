/*******************************************************************************
 *  Copyright (c) 2003, 2011 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.eclipse.pde.internal.core.text.bundle.FragmentHostHeader;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class BundleFragment extends BundlePluginBase implements IBundleFragment {

	private static final long serialVersionUID = 1L;

	public String getPluginId() {
		return getValue(Constants.FRAGMENT_HOST, true);
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
		} catch (IllegalArgumentException e) {
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
			IManifestHeader header = getManifestHeader(Constants.FRAGMENT_HOST);
			if (header instanceof FragmentHostHeader) {
				((FragmentHostHeader) header).setHostId(id);
			} else {
				bundle.setHeader(Constants.FRAGMENT_HOST, writeFragmentHost(id, getPluginVersion()));
			}
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
			IManifestHeader header = getManifestHeader(Constants.FRAGMENT_HOST);
			if (header instanceof FragmentHostHeader) {
				((FragmentHostHeader) header).setHostRange(version);
			} else {
				bundle.setHeader(Constants.FRAGMENT_HOST, writeFragmentHost(getPluginId(), version));
			}
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

	private String getAttribute(String key, String attribute) {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		String value = bundle.getHeader(key);
		if (value == null)
			return null;
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(key, value);
			if (elements.length > 0)
				return elements[0].getAttribute(attribute);
		} catch (BundleException e) {
		}
		return null;
	}

	public boolean isPatch() {
		return "true".equals(getValue(ICoreConstants.PATCH_FRAGMENT, false)); //$NON-NLS-1$ 
	}

}
