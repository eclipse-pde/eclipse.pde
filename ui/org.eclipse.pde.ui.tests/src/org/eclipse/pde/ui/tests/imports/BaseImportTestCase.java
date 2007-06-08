/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.imports;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizard.ImportQuery;
import org.eclipse.pde.ui.tests.PDETestCase;

public abstract class BaseImportTestCase extends PDETestCase {

	protected void runOperation(String[] symbolicNames, int type) {
		PluginImportOperation.IImportQuery query = new ImportQuery(getShell());
		PluginImportOperation.IImportQuery executionQuery = new ImportQuery(getShell());
		final PluginImportOperation op =
			new PluginImportOperation(getModels(symbolicNames), type, query, executionQuery, false);

		try {
			op.run(new NullProgressMonitor());
		} catch (OperationCanceledException e) {
			fail("Import Operation failed: " + e);
		} catch (CoreException e) {
			fail("Import Operation failed: " + e);
		}
	}

	protected IPluginModelBase[] getModels(String[] symbolicNames) {
		IPluginModelBase[] models = new IPluginModelBase[symbolicNames.length];
		for (int i = 0; i < symbolicNames.length; i++) {
			IPluginModelBase model = PluginRegistry.findModel(symbolicNames[i]);
			assertNull(model.getUnderlyingResource());
			models[i] = model;
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
					|| !entry.getPath().equals(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH))
				continue;
			if (roots[i].getSourceAttachmentPath() == null)
				return false;
		}
		return true;
	}



}
