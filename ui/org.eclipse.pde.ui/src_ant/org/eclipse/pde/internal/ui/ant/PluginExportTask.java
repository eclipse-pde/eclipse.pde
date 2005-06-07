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
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.build.FeatureExportInfo;
import org.eclipse.pde.internal.ui.build.PluginExportJob;

public class PluginExportTask extends BaseExportTask {
	protected IPluginModelBase[] fPlugins = new IPluginModelBase[0];

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
		info.items = fPlugins;
		info.javacSource = fJavacSource;
		info.javacTarget = fJavacTarget;
		return new PluginExportJob(info);
	}
	
	public void setPlugins(String plugins) {
		StringTokenizer tok = new StringTokenizer(plugins, ","); //$NON-NLS-1$
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ArrayList models = new ArrayList();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IPluginModelBase model = manager.findPlugin(id, null, IMatchRules.NONE);
			if (model != null && model.getUnderlyingResource() != null)
				models.add(model);
		}
		fPlugins = (IPluginModelBase[])models.toArray(new IPluginModelBase[models.size()]);
	}
	
}
