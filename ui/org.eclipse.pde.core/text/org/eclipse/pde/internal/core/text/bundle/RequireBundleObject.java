/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
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
        setModel(fHeader.getBundle().getModel());
	}

	public RequireBundleObject(ManifestHeader header, ManifestElement manifestElement) {
		super(header, manifestElement);
        setModel(fHeader.getBundle().getModel());
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
        setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version);
        fHeader.update();
        firePropertyChanged(this, Constants.BUNDLE_VERSION_ATTRIBUTE, old, version);
	}
	
	public String getVersion() {
		return getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
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
		
		return "true".equals(getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE));
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
		
		return "true".equals(getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE));
	}

}
