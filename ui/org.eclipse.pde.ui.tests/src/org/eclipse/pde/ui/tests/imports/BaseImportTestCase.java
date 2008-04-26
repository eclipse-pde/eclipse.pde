/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.imports;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizard.ImportQuery;
import org.eclipse.pde.ui.tests.PDETestCase;

public abstract class BaseImportTestCase extends PDETestCase {
	
	protected abstract int getType();
	protected abstract void verifyProject(String projectName, boolean isJava);
	
	public void testImportJAR() {
		doSingleImport("org.eclipse.jsch.core", true);
	}

	public void testImportFlat() {
		doSingleImport("org.eclipse.jdt.debug", true);
	}

	public void testImportNotJavaFlat() {
		doSingleImport("org.junit.source", false);
	}

	public void testImportNotJavaJARd() {
		doSingleImport("org.eclipse.jdt.doc.user", false);
		doSingleImport("org.eclipse.pde.source", false);
	}
	
	public void testImportJUnit() {
		doSingleImport("org.junit", true);
		doSingleImport("org.junit4", true);
	}
	
	public void testImportICU(){
		doSingleImport("com.ibm.icu", true);
	}
	
	public void testImportLinksMultiple() {
		IPluginModelBase[] modelsToImport = getModels(new String[] {"org.eclipse.core.filebuffers", "org.eclipse.jdt.doc.user", "org.eclipse.pde.build"});
		runOperation(modelsToImport, getType());
		for (int i = 0; i < modelsToImport.length; i++) {
			verifyProject(modelsToImport[i], i != 1);
		}
	}
	
	protected void doSingleImport(String bundleSymbolicName, boolean isJava) {
		IPluginModelBase modelToImport = PluginRegistry.findModel(bundleSymbolicName);
		assertNull(modelToImport.getUnderlyingResource());
		runOperation(new IPluginModelBase[] {modelToImport}, getType());
		verifyProject(modelToImport, isJava);
	}
	
	protected void runOperation(IPluginModelBase[] models, int type) {
		PluginImportOperation.IImportQuery query = new ImportQuery(getShell());
		PluginImportOperation.IImportQuery executionQuery = new ImportQuery(getShell());
		final PluginImportOperation op = new PluginImportOperation(models, type, query, executionQuery, false);

		try {
			PDEPlugin.getWorkspace().run(op, new NullProgressMonitor());
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
	
	protected void verifyProject(IPluginModelBase modelImported, boolean isJava) {
		String id = modelImported.getPluginBase().getId();
		verifyProject(id, isJava);
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
			if (entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY || entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER || !entry.getPath().equals(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH))
				continue;
			if (roots[i].getSourceAttachmentPath() == null)
				return false;
		}
		return true;
	}

}
