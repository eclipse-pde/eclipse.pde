/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.model.PluginDescriptorModel;
import org.eclipse.core.runtime.model.PluginFragmentModel;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.core.runtime.model.PluginRegistryModel;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;


public class RegistryLoader {

	public static MultiStatus loadFromDirectories(
		Vector result,
		Vector fresult,
		String[] pluginPaths,
		boolean resolve,
		boolean useCache,
		IProgressMonitor monitor) {
		try {
			URL[] urls = new URL[pluginPaths.length];
			for (int i = 0; i < pluginPaths.length; i++) {
				urls[i] = new URL("file:" + pluginPaths[i].replace('\\', '/') + "/");
			}
			TargetPlatformRegistryLoader loader = new TargetPlatformRegistryLoader();
			monitor.beginTask("",6);
			MultiStatus errors = loader.load(urls, resolve, useCache, new SubProgressMonitor(monitor,4));
			PluginRegistryModel registryModel = loader.getRegistry();
			processPluginModels(result, registryModel.getPlugins(), false, new SubProgressMonitor(monitor,1));
			processPluginModels(fresult, registryModel.getFragments(), true, new SubProgressMonitor(monitor,1));
			return errors;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	private static void processPluginModels(
		Vector result,
		PluginModel[] models,
		boolean isFragment,
		IProgressMonitor monitor) {
		monitor.beginTask("", models.length);
		for (int i = 0; i < models.length; i++) {
			ExternalPluginModelBase model = processPluginModel(models[i], isFragment);
			if (model.isLoaded()) {
				result.add(model);
			}
			monitor.worked(1);
		}
	}
	
	public static ExternalPluginModelBase processPluginModel(PluginModel registryModel, boolean isFragment) {
		ExternalPluginModelBase model = null;
		if (isFragment) {
			model = new ExternalFragmentModel();
		} else {
			model = new ExternalPluginModel();
		}
		String location = registryModel.getLocation();
		try {
			String localLocation = new URL(location).getFile();
			IPath path = new Path(localLocation).removeTrailingSeparator();
			if (path.getDevice() != null)
				path = path.setDevice(path.getDevice().toUpperCase());
			model.setInstallLocation(path.toOSString());
			model.getPluginBase();
		} catch (MalformedURLException e) {
			model.setInstallLocation(location);
		}
		model.load(registryModel);
		if (model.isLoaded())
			model.getPluginBase();
		return model;
	}

	public static void reloadFromLive(
		Vector result,
		Vector fresult,
		IProgressMonitor monitor) {
		PluginRegistryModel registryModel =
			(PluginRegistryModel) Platform.getPluginRegistry();
		PluginDescriptorModel[] plugins = registryModel.getPlugins();
		PluginFragmentModel[] fragments = registryModel.getFragments();
		monitor.beginTask("", plugins.length + fragments.length);
		processPluginModels(result, plugins, false, monitor);
		processPluginModels(fresult, fragments, true, monitor);
	}

	public static void reload(
		String[] pluginPaths,
		Vector result,
		Vector fresult,
		IProgressMonitor monitor) {
		MultiStatus errors =
			loadFromDirectories(
				result,
				fresult,
				pluginPaths,
				true,
				true,
				monitor);
		if (errors != null && errors.getChildren().length > 0) {
			ResourcesPlugin.getPlugin().getLog().log(errors);
		}
	}


}
