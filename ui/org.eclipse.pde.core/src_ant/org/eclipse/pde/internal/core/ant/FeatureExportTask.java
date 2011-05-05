/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureExportTask extends BaseExportTask {
	private IFeatureModel[] fFeatures = new IFeatureModel[0];

	protected Job getExportJob(String jobName) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fToDirectory;
		info.useJarFormat = fUseJarFormat;
		info.exportSource = fExportSource;
		info.zipFileName = fZipFilename;
		info.items = fFeatures;
		info.qualifier = fQualifier;
		info.allowBinaryCycles = fAllowBinaryCycles;
		info.useWorkspaceCompiledClasses = fUseWorkspaceCompiledClasses;
		// if destination is relative, then make it absolute
		if (!new File(fDestination).isAbsolute()) {
			File home = new File(getLocation().getFileName()).getParentFile();
			info.destinationDirectory = new File(home, fDestination).toString();
		} else
			info.destinationDirectory = fDestination;
		return new FeatureExportOperation(info, jobName);
	}

	public void setFeatures(String features) {
		StringTokenizer tok = new StringTokenizer(features, ","); //$NON-NLS-1$
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		ArrayList list = new ArrayList();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IFeatureModel model = manager.findFeatureModel(id);
			if (model != null)
				list.add(model);
		}

		fFeatures = (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}
}
