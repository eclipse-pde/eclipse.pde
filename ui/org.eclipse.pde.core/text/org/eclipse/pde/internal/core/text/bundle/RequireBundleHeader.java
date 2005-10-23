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

import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

public class RequireBundleHeader extends CompositeManifestHeader {

	private static final long serialVersionUID = 1L;

	public RequireBundleHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
	
	public void addBundle(IPluginImport iimport) {
		addBundle(iimport.getId(), iimport.getVersion(), iimport.isReexported(), iimport.isOptional());
	}
	
	public void addBundle(String id) {
		addBundle(id, null, false, false);
	}
	
	public void addBundle(String id, String version, boolean exported, boolean optional) {
		PDEManifestElement element = new PDEManifestElement(this, id);

		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
		if (optional)
			if (bundleManifestVersion > 1)
				element.setDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL); 
			else
				element.setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true");  //$NON-NLS-1$
		
		if (exported)
			if (bundleManifestVersion > 1)
				element.setDirective(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT); 
			else
				element.setAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, "true"); //$NON-NLS-1$

		if (version != null && version.trim().length() > 0)
			element.setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version.trim()); 		

		addManifestElement(element);
	}
	
	public void removeBundle(String id) {
		removeManifestElement(id);		
	}
	
	public void updateBundle(int index, IPluginImport iimport) {
		if (index == -1)
			return;
		
		PDEManifestElement element = getElementAt(index);
		if (element != null) {
			element.setValue(iimport.getId());

			int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
			if (iimport.isOptional()) {
				if (bundleManifestVersion > 1)
					element.setDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL); 
				else
					element.setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true"); //$NON-NLS-1$
			} else {
				if (bundleManifestVersion > 1)
					element.setDirective(Constants.RESOLUTION_DIRECTIVE, null); 
				else
					element.setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, null);				
			}
			
			if (iimport.isReexported()) {
				if (bundleManifestVersion > 1)
					element.setDirective(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT); 
				else
					element.setAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, "true"); //$NON-NLS-1$
			} else {
				if (bundleManifestVersion > 1)
					element.setDirective(Constants.VISIBILITY_DIRECTIVE, null); 
				else
					element.setAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, null);				
			}
			element.setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, iimport.getVersion());
		}
		update(true);
	}
	
}
