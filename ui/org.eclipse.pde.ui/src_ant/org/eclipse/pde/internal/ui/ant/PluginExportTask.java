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
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

public class PluginExportTask extends BaseExportTask {
	protected IPluginModelBase[] fModels = new IPluginModelBase[0];

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.ant.BaseExportTask#getExportJob()
	 */
	protected Job getExportJob() {
		return new PluginExportJob(
				fToDirectory, fUseJarFormat, fExportSource,
				fDestination, fZipFilename, fModels);
	}
	
	public void setPlugins(String plugins) {
		StringTokenizer tok = new StringTokenizer(plugins, ",");
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ArrayList models = new ArrayList();
		while (tok.hasMoreTokens()) {
			String id = tok.nextToken().trim();
			IPluginModelBase model = manager.findPlugin(id, null, IMatchRules.NONE);
			if (model != null && model.getUnderlyingResource() != null)
				models.add(model);
		}
		fModels = (IPluginModelBase[])models.toArray(new IPluginModelBase[models.size()]);
	}
	
}
