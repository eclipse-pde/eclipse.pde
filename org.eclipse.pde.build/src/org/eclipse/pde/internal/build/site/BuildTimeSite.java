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

import java.net.URL;
import java.util.Properties;
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
public class BuildTimeSite extends Site implements ISite, IPDEBuildConstants, IXMLConstants {
	private PluginRegistryModel pluginRegistry;

	public PluginRegistryModel getPluginRegistry() throws CoreException {
		if (pluginRegistry == null) {
			// create the registry according to the site where the code to compile is, and a existing installation of eclipse 
			BuildTimeSiteContentProvider contentProvider = (BuildTimeSiteContentProvider) getSiteContentProvider();
			MultiStatus problems = new MultiStatus(PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null); //$NON-NLS-1$
			Factory factory = new Factory(problems);
			pluginRegistry = Platform.parsePlugins(contentProvider.getPluginPaths(), factory);
			setFragments();
			setExtraPrerequisites();
			IStatus status = factory.getStatus();
			if (!status.isOK())
				throw new CoreException(status);
		}
		return pluginRegistry;
	}

	/**
	 * This methods allows to set extra prerequisite for a given plugin
	 */
	private void setExtraPrerequisites() {
		PluginModel[] plugins = pluginRegistry.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			addPrerequisites(plugins[i]);
		}
		PluginModel[] fragments = pluginRegistry.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			addPrerequisites(fragments[i]);
		}
	}

	private void addPrerequisites(PluginModel model) {
		//Read the build.properties
		Properties buildProperties = new Properties();
		try {
			buildProperties.load(new URL(model.getLocation() + "/" + PROPERTIES_FILE).openStream()); //$NON-NLS-1$
		} catch (Exception e) {
			return;
		}

		String extraPrereqs = (String) buildProperties.get(PROPERTY_EXTRA_PREREQUISITES);
		if (extraPrereqs==null)
			return;

		//Create the new prerequisite from the list
		PluginPrerequisiteModel[] oldRequires = model.getRequires();
		String[] extraPrereqsList = Utils.getArrayFromString(extraPrereqs);
		int oldRequiresLength = oldRequires==null ? 0 : oldRequires.length; 
		PluginPrerequisiteModel[] newRequires = new PluginPrerequisiteModel[oldRequiresLength + extraPrereqsList.length];
		if (oldRequires!=null)
			System.arraycopy(oldRequires, 0, newRequires, 0, oldRequires.length);
		for (int i = 0; i < extraPrereqsList.length; i++) {
			PluginPrerequisiteModel prereq = new PluginPrerequisiteModel();
			prereq.setPlugin(extraPrereqsList[i]);
			newRequires[oldRequiresLength + i] = prereq; 
		}
		model.setRequires(newRequires);
	}

	//Associate the fragments to their corresponding plugin
	//because this is not done when parsing, and because
	//we can not use the registry resolver
	private void setFragments() {
		PluginFragmentModel[] fragments = pluginRegistry.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			String pluginId = fragments[i].getPluginId();
			PluginDescriptorModel plugin = pluginRegistry.getPlugin(pluginId);
			if (plugin == null) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", pluginId), null); //$NON-NLS-1$
				Platform.getPlugin(PI_PDEBUILD).getLog().log(status);
				continue;
			}
			
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
