/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;


public class PluginExportJob extends FeatureExportJob {

	private String fFeatureLocation;

	public PluginExportJob(
		int exportType,
		boolean exportSource,
		String destination,
		String zipFileName,
		Object[] items) {
		super(exportType, exportSource, destination, zipFileName, items);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#doExports(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void doExports(IProgressMonitor monitor)
			throws InvocationTargetException, CoreException {
		try {
			// create a feature to contain all plug-ins
			String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator + featureID;
			createFeature(featureID, fFeatureLocation);
			createBuildPropertiesFile(fFeatureLocation);
			doExport(featureID, null, fFeatureLocation, TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), monitor);
		} catch (IOException e) {
		} finally {
			for (int i = 0; i < fItems.length; i++) {
				if (fItems[i] instanceof IPluginModelBase)
					deleteBuildFiles((IPluginModelBase)fItems[i]);
			}
			cleanup(new SubProgressMonitor(monitor, 1));
			monitor.done();
		}
	}
	
	private void createFeature(String featureID, String featureLocation)
			throws IOException {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();
		File featureXML = new File(file, "feature.xml"); //$NON-NLS-1$
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(featureXML), "UTF-8"), true); //$NON-NLS-1$
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<feature id=\"" + featureID+ "\" version=\"1.0\">"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < fItems.length; i++) {
			if (fItems[i] instanceof IPluginModelBase) {
				IPluginBase plugin = ((IPluginModelBase) fItems[i])
						.getPluginBase();
				writer.println("<plugin id=\"" + plugin.getId()+ "\" version=\"0.0.0\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		writer.println("</feature>"); //$NON-NLS-1$
		writer.close();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getPaths()
	 */
	protected String[] getPaths() throws CoreException {
		String[] paths =  super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + "feature.xml"; //$NON-NLS-1$
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}
	
	private void createBuildPropertiesFile(String featureLocation) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();
		Properties prop = new Properties();
		prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
		save(new File(file, "build.properties"),prop, "Marker File");  //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void save(File file, Properties properties, String header) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, header); 
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	


}
