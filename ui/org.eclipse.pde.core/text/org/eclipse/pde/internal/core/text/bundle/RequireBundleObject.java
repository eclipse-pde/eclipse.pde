/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.osgi.framework.Constants;

public class RequireBundleObject extends PDEManifestElement {

	private static final long serialVersionUID = 1L;

	public RequireBundleObject(ManifestHeader header, String value) {
		super(header, value);
	}

	public RequireBundleObject(ManifestHeader header, ManifestElement manifestElement) {
		super(header, manifestElement);
	}

	public void setId(String id) {
		String old = getId();
		setValue(id);
		fHeader.update();
		firePropertyChanged(this, fHeader.getName(), old, id);
	}

	public String getId() {
		return getValue();
	}

	public void setVersion(String version) {
		String old = getVersion();
		// Reset the previous value
		setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, null);
		// Parse the version String into segments
		String[] values = ManifestElement.getArrayFromList(version);
		// If there are values, add them
		if ((values != null) && (values.length > 0)) {
			for (int i = 0; i < values.length; i++) {
				addAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, values[i]);
			}
		}
		fHeader.update();
		firePropertyChanged(this, Constants.BUNDLE_VERSION_ATTRIBUTE, old, version);
	}

	public String getVersion() {
		String[] versionSegments = getAttributes(Constants.BUNDLE_VERSION_ATTRIBUTE);
		StringBuffer version = new StringBuffer();
		if (versionSegments == null) {
			return null;
		} else if (versionSegments.length == 0) {
			return null;
		} else if (versionSegments.length == 1) {
			version.append(versionSegments[0]);
		} else if (versionSegments.length == 2) {
			version.append(versionSegments[0]);
			version.append(',');
			version.append(versionSegments[1]);
		}
		return version.toString();
	}

	public void setOptional(boolean optional) {
		boolean old = isOptional();
		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(fHeader.getBundle());
		if (optional) {
			if (bundleManifestVersion > 1)
				setDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
			else
				setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true"); //$NON-NLS-1$
		} else {
			if (bundleManifestVersion > 1)
				setDirective(Constants.RESOLUTION_DIRECTIVE, null);
			else
				setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, null);
		}
		fHeader.update();
		firePropertyChanged(this, Constants.RESOLUTION_DIRECTIVE, Boolean.toString(old), Boolean.toString(optional));
	}

	public boolean isOptional() {
		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(fHeader.getBundle());
		if (bundleManifestVersion > 1)
			return Constants.RESOLUTION_OPTIONAL.equals(getDirective(Constants.RESOLUTION_DIRECTIVE));

		return "true".equals(getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
	}

	public void setReexported(boolean export) {
		boolean old = isReexported();
		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(fHeader.getBundle());
		if (export) {
			if (bundleManifestVersion > 1)
				setDirective(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT);
			else
				setAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, "true"); //$NON-NLS-1$
		} else {
			if (bundleManifestVersion > 1)
				setDirective(Constants.VISIBILITY_DIRECTIVE, null);
			else
				setAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, null);
		}
		fHeader.update();
		firePropertyChanged(this, Constants.VISIBILITY_DIRECTIVE, Boolean.toString(old), Boolean.toString(export));
	}

	public boolean isReexported() {
		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(fHeader.getBundle());
		if (bundleManifestVersion > 1)
			return Constants.VISIBILITY_REEXPORT.equals(getDirective(Constants.VISIBILITY_DIRECTIVE));

		return "true".equals(getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE)); //$NON-NLS-1$
	}

}
