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

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;

/**
 *
 */
public class TargetPlatformRegistryLoader {
	
	private static String getFilesAndMode(URL[] urls, ArrayList fileList) {
		String targetMode = "2.1";
		for (int i = 0; i < urls.length; i++) {
			File directory = new File(urls[i].getFile());
			if (directory.exists() && directory.isDirectory()) {
				File[] files = directory.listFiles();
				if (files != null) {
					for (int j = 0; j < files.length; j++) {
						if (files[j].isDirectory()) {
							fileList.add(files[j]);
							if (files[j].getName().indexOf("org.eclipse.osgi") != -1)
								targetMode = null;
						}
					}
				}
			}
		}			
		return targetMode;
	}

	public static void load(URL[] urls, PDEState state, IProgressMonitor monitor) {
		ArrayList list = new ArrayList();
		String targetMode = getFilesAndMode(urls, list);
		state.setTargetMode(targetMode);
		for (int i = 0; i < list.size(); i++) {
			state.addBundle((File)list.get(i));
		}
	}
	
	public static IPluginModelBase[] loadModels(URL[] urls, boolean resolve, PDEState state, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask("Parsing Plugins...", 3);
		
		load(urls, state, monitor);
		monitor.worked(1);
		
		state.resolveState();	
		monitor.worked(1);
		
		BundleDescription[] bundleDescriptions = resolve ? state.getState().getResolvedBundles() : state.getState().getBundles();
		IPluginModelBase[] models = new IPluginModelBase[bundleDescriptions.length];
		for (int i = 0; i < bundleDescriptions.length; i++) {
			monitor.subTask(bundleDescriptions[i].getUniqueId());
			models[i] = createModelFromDescription(bundleDescriptions[i], state);
		}	
		monitor.done();
		return models;
	}
	
	public static IPluginModelBase[] loadModels(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		PDEState state = new PDEState();
		return loadModels(urls, resolve, state, monitor);
	}
	
	public static IPluginModelBase[] loadModels(String[] paths, boolean resolve, PDEState state, IProgressMonitor monitor) {
		URL[] urls = new URL[paths.length];
		try {
			for (int i = 0; i < paths.length; i++) {
				urls[i] = new URL("file:" + paths[i].replace('\\', '/') + "/");
			}
		} catch (MalformedURLException e) {
		}
		return loadModels(urls, resolve, state, monitor);
	}
	
	public static IPluginModelBase[] loadModels(String[] paths, boolean resolve, IProgressMonitor monitor) {
		PDEState state = new PDEState();
		return loadModels(paths, resolve, state, monitor);
	}

	/**
	 * @param description
	 * @return
	 */
	private static IPluginModelBase createModelFromDescription(BundleDescription description, PDEState state) {
		ExternalPluginModelBase model = null;
		if (description.getHosts().length == 0)
			model = new ExternalPluginModel();
		else
			model = new ExternalFragmentModel();
		model.load(description, state);
		return model;
	}

}
