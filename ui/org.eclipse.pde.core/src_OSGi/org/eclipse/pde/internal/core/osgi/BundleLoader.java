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
package org.eclipse.pde.internal.core.osgi;

import java.io.File;
import java.net.*;
import java.util.Vector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.*;


public class BundleLoader {
	public static MultiStatus loadFromDirectories(
		Vector result,
		Vector fresult,
		String[] pluginPaths,
		boolean resolve,
		boolean useCache,
		IProgressMonitor monitor) {
		
		PlatformAdmin admin = PDECore.getDefault().acquirePlatform();
		StateObjectFactory factory = admin.getFactory();
		State state = factory.createState();
		long id[]= {0};
		for (int i = 0; i < pluginPaths.length; i++) {
			String pluginPath = pluginPaths[i];
			parseDirectory(pluginPath, id);
		}
		/*
		monitor.beginTask("",6);
		MultiStatus errors = loader.load(urls, resolve, useCache, new SubProgressMonitor(monitor,4));
		PluginRegistryModel registryModel = loader.getRegistry();
		processPluginModels(result, registryModel.getPlugins(), false, new SubProgressMonitor(monitor,1));
		processPluginModels(fresult, registryModel.getFragments(), true, new SubProgressMonitor(monitor,1));
		*/
		return null;
	}

	private static void parseDirectory(String path, long []id) {
		File dir = new File(path);
		File [] pdirs = dir.listFiles();
		for (int i=0; i<pdirs.length; i++) {
			File pdir = pdirs[i];
			if (pdir.isDirectory()) {
				// test for manifest first
				File file = new File(pdir, "META-INF/MANIFEST.MF");
				if (file.exists()) {
					// manifest present
					parseBundleManifest(file, id[0]++);
				} else {
					// try plugin.xml
					file = new File(dir, "plugin.xml");
					if (file.exists()) {
						parsePluginManifest(file, false);
					}
					else {
						// try fragment.xml
						file = new File(dir, "fragment.xml");
						if (file.exists()) {
							parsePluginManifest(file, true);
						}
					}
				}
			}
		}
	}

	private static void parseBundleManifest(File file, long id) {
		System.out.println("Bundle: "+file.getPath());
	}
	
	private static void parsePluginManifest(File file, boolean fragment) {
		System.out.println((fragment?"Plugin: ":"Fragment: ")+file.getPath());
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
