/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.*;
import org.osgi.util.tracker.*;

public class PDEPluginConverter {
	
	public static void convertToOSGIFormat(IProject project, String filename, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().append(filename).toOSString());
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			PluginConverter converter = (PluginConverter) tracker.getService();
			converter.convertManifest(inputFile, outputFile, false, null, true);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			tracker.close();
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
	}

}
