/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public abstract class FeatureBasedExportOperation extends FeatureExportOperation {

	protected String fFeatureLocation;

	public FeatureBasedExportOperation(FeatureExportInfo info, String name) {
		super(info, name);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Exporting...", 33); //$NON-NLS-1$
		try {
			createDestination();

			// create a feature to contain all plug-ins
			String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator + featureID;
			String[][] config = new String[][] {{TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL()}};
			createFeature(featureID, fFeatureLocation, config, false);
			createBuildPropertiesFile(fFeatureLocation);
			if (fInfo.useJarFormat) {
				createPostProcessingFiles();
			}
			IStatus status = testBuildWorkspaceBeforeExport(subMonitor.split(10));
			doExport(featureID, null, fFeatureLocation, config, subMonitor.split(20));
			if (subMonitor.isCanceled()) {
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
			for (Object item : fInfo.items) {
				if (item instanceof IModel) {
					try {
						deleteBuildFiles(item);
					} catch (CoreException e) {
						PDECore.log(e);
					}
				}
			}
			cleanup(subMonitor.split(3));
		}
	}

	protected abstract void createPostProcessingFiles();

	@Override
	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + ICoreConstants.FEATURE_FILENAME_DESCRIPTOR;
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory()) {
			file.mkdirs();
		}
		Properties prop = new Properties();
		prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$

		if (fInfo.exportSource && fInfo.exportSourceBundle) {
			prop.put("individualSourceBundles", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			Dictionary<String, String> environment = new Hashtable<>(4);
			environment.put("osgi.os", TargetPlatform.getOS()); //$NON-NLS-1$
			environment.put("osgi.ws", TargetPlatform.getWS()); //$NON-NLS-1$
			environment.put("osgi.arch", TargetPlatform.getOSArch()); //$NON-NLS-1$
			environment.put("osgi.nl", TargetPlatform.getNL()); //$NON-NLS-1$

			for (Object item : fInfo.items) {
				if (item instanceof IFeatureModel) {
					IFeature feature = ((IFeatureModel) item).getFeature();
					prop.put("generate.feature@" + feature.getId() + ".source", feature.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					BundleDescription bundle = null;
					if (item instanceof IPluginModelBase) {
						bundle = ((IPluginModelBase) item).getBundleDescription();
					}
					if (bundle == null) {
						if (item instanceof BundleDescription) {
							bundle = (BundleDescription) item;
						}
					}
					if (bundle == null) {
						continue;
					}
					if (shouldAddPlugin(bundle, environment)) {
						prop.put("generate.plugin@" + bundle.getSymbolicName() + ".source", bundle.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		save(new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR), prop, "Marker File"); //$NON-NLS-1$
	}
}
