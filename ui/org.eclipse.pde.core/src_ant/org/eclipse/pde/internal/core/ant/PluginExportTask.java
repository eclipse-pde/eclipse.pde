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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;

public class PluginExportTask extends BaseExportTask {
	protected IPluginModelBase[] fPlugins = new IPluginModelBase[0];

	@Override
	protected Job getExportJob(String jobName) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fToDirectory;
		info.useJarFormat = fUseJarFormat;
		info.exportSource = fExportSource;
		info.exportSourceBundle = fExportSourceBundle;
		info.zipFileName = fZipFilename;
		info.items = fPlugins;
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
		return new PluginExportOperation(info, jobName);
	}

	public void setPlugins(String plugins) {
		StringTokenizer tok = new StringTokenizer(plugins, ","); //$NON-NLS-1$
		ArrayList<IPluginModelBase> models = new ArrayList<>();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				models.add(model);
			}
		}
		fPlugins = models.toArray(new IPluginModelBase[models.size()]);
	}

}
