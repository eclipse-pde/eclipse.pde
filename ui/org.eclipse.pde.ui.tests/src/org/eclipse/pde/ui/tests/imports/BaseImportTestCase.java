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

import org.osgi.framework.Version;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
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
	
	public void testImportJUnit3() {
		doSingleImport("org.junit", 3, true);
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
	
	/**
	 * Imports a bundle with the given symbolic name and a version with a major version matching
	 * the given int.  The result is checked for flags and natures.  This method was added to
	 * test org.junit which will have two versions of the same bundle in the SDK.
	 *     
	 * @param bundleSymbolicName name of the plug-in to import
	 * @param majorVersion the major version that the imported plug-in must have
	 * @param isJava whether the imported plug-in should have a java nature
	 */
	protected void doSingleImport(String bundleSymbolicName, int majorVersion, boolean isJava) {
		ModelEntry entry = PluginRegistry.findEntry(bundleSymbolicName);
		IPluginModelBase models[] = entry.getExternalModels();
		assertTrue("No models for " + bundleSymbolicName + " could be found", models.length > 0);
		IPluginModelBase modelToImport = null;
		
		for (int i = 0; i < models.length; i++) {
			Version version = new Version(models[i].getPluginBase().getVersion());
			if (version.getMajor() == majorVersion){
				modelToImport = models[i];
				break;
			}
		}
		
		assertNull("Model with correct major version " + majorVersion + " could not be found",  modelToImport.getUnderlyingResource());
		runOperation(new IPluginModelBase[] {modelToImport}, getType());
		verifyProject(modelToImport, isJava);
	}
	
	protected void runOperation(IPluginModelBase[] models, int type) {
		PluginImportOperation job = new PluginImportOperation(models, type, false);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setSystem(true);
		job.schedule();
		try{
			job.join();
		} catch (InterruptedException e){
			fail("Job interupted: " + e.getMessage());
		}
		IStatus status = job.getResult();
		if (!status.isOK()){
			fail("Import Operation failed: " + status.toString());
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
