/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;

public class BuildPluginAction extends BaseBuildAction {

	protected void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
	
		BuildScriptGenerator generator = new BuildScriptGenerator();
		AbstractScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());
		AbstractScriptGenerator.setForceUpdateJar(AbstractScriptGenerator.getForceUpdateJarFormat());
		AbstractScriptGenerator.setConfigInfo(AbstractScriptGenerator.getDefaultConfigInfos());
		
		IProject project = fManifestFile.getProject();
		generator.setWorkingDirectory(project.getLocation().toOSString());
		String url = ClasspathHelper.getDevEntriesProperties(project.getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(url);
		generator.setPDEState(TargetPlatform.getState());
		generator.setNextId(TargetPlatform.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatform.getBundleClasspaths(TargetPlatform.getPDEState()), TargetPlatform.getPatchMap(TargetPlatform.getPDEState()));
		generator.setBuildingOSGi(true);
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
		generator.setElements(new String[] { "plugin@" +model.getPluginBase().getId() }); //$NON-NLS-1$
		
		generator.generate();
	}

}
