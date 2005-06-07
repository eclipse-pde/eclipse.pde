/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.ant;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.build.FeatureExportInfo;
import org.eclipse.pde.internal.ui.build.FeatureExportJob;

public class FeatureExportTask extends BaseExportTask {
	private IFeatureModel[] fFeatures = new IFeatureModel[0];
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.ant.BaseExportTask#getExportJob()
	 */
	protected Job getExportJob() {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fToDirectory;
		info.useJarFormat = fUseJarFormat;
		info.exportSource = fExportSource;
		info.destinationDirectory = fDestination;
		info.zipFileName = fZipFilename;
		info.items = fFeatures;
		info.javacSource = fJavacSource;
		info.javacTarget = fJavacTarget;
		return new FeatureExportJob(info);
	}
	
	public void setFeatures(String features) {
		StringTokenizer tok = new StringTokenizer(features, ","); //$NON-NLS-1$
		ArrayList list = new ArrayList();
		while (tok.hasMoreTokens()) {
			list.add(tok.nextToken().trim());
		}
		
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		ArrayList featureList = new ArrayList();
		IFeatureModel[] models = manager.getFeatureModels();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getFeature().getId();
			if (list.contains(id))
				featureList.add(models[i]);			
		}
		fFeatures = (IFeatureModel[])featureList.toArray(new IFeatureModel[featureList.size()]);
	}
}
