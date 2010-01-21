/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.converter;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.BundleException;

public class PDEPluginConverter {

	public static void convertToOSGIFormat(IProject project, String target, Dictionary dictionary, IProgressMonitor monitor) throws CoreException {
		convertToOSGIFormat(project, target, dictionary, null, monitor);
	}

	public static void convertToOSGIFormat(IProject project, String target, Dictionary dictionary, HashMap newProps, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(PDEProject.getManifest(project).getLocation().toOSString());
			File inputFile = new File(project.getLocation().toOSString());
			PluginConverter converter = PluginConverter.getDefault();
			converter.convertManifest(inputFile, outputFile, false, target, true, dictionary);

			if (newProps != null && newProps.size() > 0)
				converter.writeManifest(outputFile, getProperties(outputFile, newProps), false);

			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (PluginConversionException e) {
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
