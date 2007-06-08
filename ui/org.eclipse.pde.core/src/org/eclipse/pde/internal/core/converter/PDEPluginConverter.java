/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.osgi.framework.BundleException;

public class PDEPluginConverter {
	
	public static void convertToOSGIFormat(IProject project, String target, Dictionary dictionary, IProgressMonitor monitor) throws CoreException {
		convertToOSGIFormat(project, target, dictionary, null, monitor);
	}
	
	public static void convertToOSGIFormat(IProject project, String target, Dictionary dictionary, HashMap newProps, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().toOSString());
			PluginConverter converter = PluginConverter.getDefault();
			converter.convertManifest(inputFile, outputFile, false, target, true, dictionary);

			if (newProps != null && newProps.size() > 0)
				converter.writeManifest(outputFile, getProperties(outputFile, newProps), false);
		
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (PluginConversionException e) {
		}  finally {
			monitor.done();
		}
	}
	
	public static void createBundleForFramework(IProject project, HashMap newProps, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().toOSString());
			PluginConverter converter = PluginConverter.getDefault();
			double version = TargetPlatformHelper.getTargetVersion();
			String versionString =  version <= 3.1 ? ICoreConstants.TARGET31 : TargetPlatformHelper.getTargetVersionString();
			converter.convertManifest(inputFile, outputFile, false, versionString, true, null);
			
			Map prop = getProperties(outputFile, newProps);
			prop.remove(ICoreConstants.ECLIPSE_AUTOSTART); 
			prop.remove(ICoreConstants.ECLIPSE_LAZYSTART);
			converter.writeManifest(outputFile, prop, false);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
	}
	
	private static Map getProperties(File file, HashMap newProps) {
		try {
			Map prop = ManifestElement.parseBundleManifest(new FileInputStream(file), null);
			if (newProps != null && newProps.size() > 0) {
				Iterator iter = newProps.keySet().iterator();
				while (iter.hasNext()) {
					String key = iter.next().toString();
					prop.put(key, newProps.get(key));
				}
			}
			return prop;
		} catch (FileNotFoundException e) {
		} catch (BundleException e) {
		} catch (IOException e) {
		}
		return new HashMap();
	}
	
}
