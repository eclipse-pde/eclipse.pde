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
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class ExternalFeatureLoader {

	/**
	 * @param monitor
	 * @return IFeatureModel[]
	 */
	public static IFeatureModel[] loadFeatureModels(IProgressMonitor monitor,
			String platformHome) {
		URL[] featurePaths = PluginPathFinder.getFeaturePaths(platformHome);
		monitor.beginTask("", featurePaths.length); //$NON-NLS-1$
		Map uniqueFeatures = new HashMap();
		for (int i = 0; i < featurePaths.length; i++) {
			File manifest = new File(featurePaths[i].getFile(), "feature.xml"); //$NON-NLS-1$
			monitor.subTask(manifest.getAbsolutePath());
			IFeatureModel model = loadFeatureModel(manifest);
			if (model != null && model.isLoaded()) {
				IFeature feature = model.getFeature();
				uniqueFeatures.put(
						feature.getId() + "_" + feature.getVersion(), model); //$NON-NLS-1$
			}
			monitor.worked(1);
		}
		Collection models = uniqueFeatures.values();
		monitor.done();
		return (IFeatureModel[]) models
				.toArray(new IFeatureModel[models.size()]);
	}

	/**
	 * 
	 * @param manifest
	 * @return ExternalFeatureModel or null
	 */
	private static IFeatureModel loadFeatureModel(File manifest) {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(manifest.getParent());
		InputStream stream = null;
		try {
			stream = new FileInputStream(manifest);
			model.load(stream, false);
			return model;
		} catch (Exception e) {
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

}
