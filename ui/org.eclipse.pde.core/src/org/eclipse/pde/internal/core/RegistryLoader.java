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

import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.core.plugin.*;


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
