/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

	@Override
	protected Job getExportJob(String jobName) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fToDirectory;
		info.useJarFormat = fUseJarFormat;
		info.exportSource = fExportSource;
		info.exportSourceBundle = fExportSourceBundle;
		info.zipFileName = fZipFilename;
		info.items = fFeatures;
		info.qualifier = fQualifier;
		info.allowBinaryCycles = fAllowBinaryCycles;
		info.useWorkspaceCompiledClasses = fUseWorkspaceCompiledClasses;
		// if destination is relative, then make it absolute
		if (!new File(fDestination).isAbsolute()) {
			File home = new File(getLocation().getFileName()).getParentFile();
			info.destinationDirectory = new File(home, fDestination).toString();
		} else {
			info.destinationDirectory = fDestination;
		}
		return new FeatureExportOperation(info, jobName);
	}

	public void setFeatures(String features) {
		StringTokenizer tok = new StringTokenizer(features, ","); //$NON-NLS-1$
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		ArrayList<IFeatureModel> list = new ArrayList<>();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IFeatureModel model = manager.findFeatureModel(id);
			if (model != null) {
				list.add(model);
			}
		}

		fFeatures = list.toArray(new IFeatureModel[list.size()]);
	}
}
