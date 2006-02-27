/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.imports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizard;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public abstract class BaseImportTestCase extends PDETestCase {

	protected void runOperation(String[] symbolicNames, int type) {
		IRunnableWithProgress op =
			PluginImportWizard.getImportOperation(
				getShell(),
				type,
				getModels(symbolicNames),
				false);
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		try {
			progressService.runInUI(progressService, op, null);
		} catch (InvocationTargetException e) {
			fail("Import Operation failed: " + e);
		} catch (InterruptedException e) {
			fail("Import Operation failed: " + e);
		}
	}
	
	protected IPluginModelBase[] getModels(String[] symbolicNames) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase[] models = new IPluginModelBase[symbolicNames.length];
		
		for (int i = 0; i < symbolicNames.length; i++) {
			ModelEntry entry = manager.findEntry(symbolicNames[i]);
			models[i] = entry.getExternalModel();
		}
		return models;
	}
	
	protected IProject verifyProject(String projectName) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		assertTrue("Project " + projectName + " does not exist", project.exists());
		return project;
	}
	
	protected boolean checkSourceAttached(IJavaProject jProject) throws CoreException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			IClasspathEntry entry = roots[i].getRawClasspathEntry();
			if (entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY 
					|| entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER 
					|| !entry.getPath().equals(new Path(PDECore.CLASSPATH_CONTAINER_ID)))
				continue;
			if (roots[i].getSourceAttachmentPath() == null)
				return false;
		}
		return true;
	}
	

	
}
