/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.builder.*;
import org.eclipse.pde.internal.core.*;

public class BuildPluginAction extends BaseBuildAction {

	protected void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
	
		ModelBuildScriptGenerator generator = new ModelBuildScriptGenerator();
		ModelBuildScriptGenerator.setOutputFormat(AbstractScriptGenerator.getDefaultOutputFormat());
		ModelBuildScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());
		ModelBuildScriptGenerator.setForceUpdateJar(AbstractScriptGenerator.getForceUpdateJarFormat());
		ModelBuildScriptGenerator.setConfigInfo(AbstractScriptGenerator.getDefaultConfigInfos());
		
		IProject project = fManifestFile.getProject();
		generator.setWorkingDirectory(project.getLocation().toOSString());
		String url = ClasspathHelper.getDevEntriesProperties(project.getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(new DevClassPathHelper(url));
		generator.setPluginPath(TargetPlatform.createPluginPath());
		generator.setBuildingOSGi(PDECore.getDefault().getModelManager().isOSGiRuntime());
		try {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
			if (model != null) {
				generator.setModelId(model.getPluginBase().getId());
				generator.generate();
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

}
