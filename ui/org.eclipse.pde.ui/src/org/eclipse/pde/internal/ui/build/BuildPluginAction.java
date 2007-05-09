/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;

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
		generator.setPDEState(TargetPlatformHelper.getState());
		generator.setNextId(TargetPlatformHelper.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatformHelper.getBundleClasspaths(TargetPlatformHelper.getPDEState()), TargetPlatformHelper.getPatchMap(TargetPlatformHelper.getPDEState()));
		generator.setBuildingOSGi(true);
		IPluginModelBase model = PluginRegistry.findModel(project);
		if(model != null && model.getPluginBase().getId() != null)
		{	generator.setElements(new String[] { "plugin@" + model.getPluginBase().getId() }); //$NON-NLS-1$
			generator.generate();
		}
		else
		{	MessageDialog.openError(null,
					PDEUIMessages.BuildPluginAction_ErrorDialog_Title, 
					PDEUIMessages.BuildPluginAction_ErrorDialog_Message); 
		}
	}

}
