/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.imports;

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;

public class ImportFromRepoTestCase extends BaseImportTestCase {

	private static int TYPE = PluginImportOperation.IMPORT_FROM_REPOSITORY;

	public ImportFromRepoTestCase(String testName) {
		setName(testName);
	}

	public static Test suite() {
		TestSuite testSuite = new TestSuite("ImportFromRepoTestCase") ;
		testSuite.addTest(new ImportFromRepoTestCase("testImportOrgEclipsePdeUaUi"));
		testSuite.addTest(new ImportFromRepoTestCase("testImportOrgEclipseRcp"));
		return testSuite;
	}
	
	protected int getType() {
		return TYPE;
	}
	
	public void testImportOrgEclipsePdeUaUi(){
		doSingleImport("org.eclipse.pde.ua.ui", true);
	}
	
	public void testImportOrgEclipseRcp(){
		doSingleImport("org.eclipse.rcp", false);
	}
	
	protected void runOperation(IPluginModelBase[] models, int type) {
		PluginImportOperation job = new PluginImportOperation(models, type, false);
		try{
			Map descriptions = ((BundleProjectService)BundleProjectService.getDefault()).getImportDescriptions(models);
			job.setImportDescriptions(descriptions);
			job.setRule(ResourcesPlugin.getWorkspace().getRoot());
			job.setSystem(true);
			job.schedule();
			job.join();
		} catch (InterruptedException e){
			fail("Job interupted: " + e.getMessage());
		} catch (CoreException e) {
			fail("Error fetching import descriptions: " + e.getMessage());
		}
		IStatus status = job.getResult();
		if (!status.isOK()){
			fail("Import Operation failed: " + status.toString());
		}
	}


	protected void verifyProject(String projectName, boolean isJava) {
		try {
			IProject project = verifyProject(projectName);
			assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
			assertEquals(isJava, project.hasNature(JavaCore.NATURE_ID));
			if (isJava) {
				IJavaProject jProject = JavaCore.create(project);
				assertTrue(checkSourceAttached(jProject));
				assertTrue(checkSourceFolder(jProject));
			}
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}

	private boolean checkSourceFolder(IJavaProject jProject) throws JavaModelException {
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE)
				return true;
		}
		return false;
	}

}
