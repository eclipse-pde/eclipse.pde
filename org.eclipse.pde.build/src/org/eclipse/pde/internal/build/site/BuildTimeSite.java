/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.site;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.*;

/**
 * This site represent a site at build time. A build time site is made of code
 * to compile, and a potential installation of eclipse (or derived products)
 * against which the code must be compiled.
 * Moreover this site provide access to a pluginRegistry.
 */
public class BuildTimeSite extends Site implements ISite, IPDEBuildConstants {
	private PluginRegistryModel pluginRegistry;

	public PluginRegistryModel getPluginRegistry() throws CoreException {
		if (pluginRegistry == null) {
			// create the registry according to the site where the code to compile is, and a existing installation of eclipse 
			BuildTimeSiteContentProvider contentProvider = (BuildTimeSiteContentProvider) getSiteContentProvider();
			MultiStatus problems = new MultiStatus(PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null); //$NON-NLS-1$
			Factory factory = new Factory(problems);
			pluginRegistry = Platform.parsePlugins(contentProvider.getPluginPaths(), factory);
			setFragments();
			IStatus status = factory.getStatus();
			if (!status.isOK())
				throw new CoreException(status);
		}
		return pluginRegistry;
	}

	//Associate the fragments to their corresponding plugin
	//because this is not done when parsing, and because
	//we can not use the registry resolver
	private void setFragments() {
		PluginFragmentModel[] fragments = pluginRegistry.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			String pluginId = fragments[i].getPluginId();
			PluginDescriptorModel plugin = pluginRegistry.getPlugin(pluginId); //TODO Needs to check for the case where the plugin is not in the repository
			PluginFragmentModel[] existingFragments = plugin.getFragments();
			if (existingFragments == null)
				plugin.setFragments(new PluginFragmentModel[] { fragments[i] });
			else {
				PluginFragmentModel[] newFragments = new PluginFragmentModel[existingFragments.length + 1];
				System.arraycopy(existingFragments, 0, newFragments, 0, existingFragments.length);
				newFragments[newFragments.length - 1] = fragments[i];
				plugin.setFragments(newFragments);
			}
		}
	}

	public IFeature findFeature(String featureId) throws CoreException {
		ISiteFeatureReference[] features = getFeatureReferences();
		for (int i = 0; i < features.length; i++) {
			if (features[i].getVersionedIdentifier().getIdentifier().equals(featureId))
				return features[i].getFeature(null);
		}
		return null;
	}
}
