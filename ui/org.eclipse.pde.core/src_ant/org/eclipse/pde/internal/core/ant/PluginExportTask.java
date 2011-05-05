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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;

public class PluginExportTask extends BaseExportTask {
	protected IPluginModelBase[] fPlugins = new IPluginModelBase[0];

	protected Job getExportJob(String jobName) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fToDirectory;
		info.useJarFormat = fUseJarFormat;
		info.exportSource = fExportSource;
		info.zipFileName = fZipFilename;
		info.items = fPlugins;
		info.qualifier = fQualifier;
		info.allowBinaryCycles = fAllowBinaryCycles;
		info.useWorkspaceCompiledClasses = fUseWorkspaceCompiledClasses;
		// if destination is relative, then make it absolute
		if (!new File(fDestination).isAbsolute()) {
			File home = new File(getLocation().getFileName()).getParentFile();
			info.destinationDirectory = new File(home, fDestination).toString();
		} else
			info.destinationDirectory = fDestination;
		return new PluginExportOperation(info, jobName);
	}

	public void setPlugins(String plugins) {
		StringTokenizer tok = new StringTokenizer(plugins, ","); //$NON-NLS-1$
		ArrayList models = new ArrayList();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null)
				models.add(model);
		}
		fPlugins = (IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
	}

}
