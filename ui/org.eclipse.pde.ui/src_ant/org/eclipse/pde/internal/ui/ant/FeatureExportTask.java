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

import java.util.*;

import org.eclipse.core.runtime.jobs.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

public class FeatureExportTask extends BaseExportTask {
	private IFeatureModel[] fFeatures = new IFeatureModel[0];
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.ant.BaseExportTask#getExportJob()
	 */
	protected Job getExportJob() {
		return new FeatureExportJob(
				fToDirectory, fUseJarFormat, fExportSource,
				fDestination, fZipFilename, fFeatures);
	}
	
	public void setFeatures(String features) {
		StringTokenizer tok = new StringTokenizer(features, ",");
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
