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
package org.eclipse.pde.internal.ui.feature;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IWorkspaceModelManager;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.builder.*;
import org.eclipse.pde.internal.core.*;

public class BuildPluginAction extends BaseBuildAction {

	protected void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		ModelBuildScriptGenerator generator;
		
		if (file.getName().toLowerCase().equals("fragment.xml"))
			generator = new FragmentBuildScriptGenerator();
		else
			generator = new PluginBuildScriptGenerator();
		
		IProject project = file.getProject();
		generator.setWorkingDirectory(project.getLocation().toOSString());
		generator.setDevEntries(new String[] {"bin"}); // FIXME: look at bug #5747
		generator.setPluginPath(TargetPlatform.createPluginPath());

		try {
			IWorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
			IPluginModelBase model = (IPluginModelBase) manager.getWorkspaceModel(project);
			generator.setModelId(model.getPluginBase().getId());
			generator.generate();
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

}
