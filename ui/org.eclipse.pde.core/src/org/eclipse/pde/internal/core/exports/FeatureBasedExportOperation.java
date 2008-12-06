/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;

public abstract class FeatureBasedExportOperation extends FeatureExportOperation {

	protected String fFeatureLocation;

	public FeatureBasedExportOperation(FeatureExportInfo info, String name) {
		super(info, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		try {
			createDestination();
			monitor.beginTask("Exporting...", 33); //$NON-NLS-1$
			// create a feature to contain all plug-ins
			String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator + featureID;
			String[] config = new String[] {TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL()};
			createFeature(featureID, fFeatureLocation, config, false);
			createBuildPropertiesFile(fFeatureLocation);
			if (fInfo.useJarFormat)
				createPostProcessingFiles();
			IStatus status = testBuildWorkspaceBeforeExport(new SubProgressMonitor(monitor, 10));
			doExport(featureID, null, fFeatureLocation, TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), new SubProgressMonitor(monitor, 20));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			return status;
		} catch (IOException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e);
		} catch (CoreException e) {
			return e.getStatus();
		} catch (InvocationTargetException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e.getTargetException());
		} finally {
			for (int i = 0; i < fInfo.items.length; i++) {
				if (fInfo.items[i] instanceof IModel)
					try {
						deleteBuildFiles(fInfo.items[i]);
					} catch (CoreException e) {
						PDECore.log(e);
					}
			}
			cleanup(null, new SubProgressMonitor(monitor, 3));
			monitor.done();
		}
	}

	protected abstract void createPostProcessingFiles();

	protected String[] getPaths() {
		String[] paths = super.getPaths();
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
		save(new File(file, "build.properties"), prop, "Marker File"); //$NON-NLS-1$ //$NON-NLS-2$
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
