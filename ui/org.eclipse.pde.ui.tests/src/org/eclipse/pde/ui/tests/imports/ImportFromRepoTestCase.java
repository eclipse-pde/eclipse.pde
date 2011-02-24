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
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSCommunicationException;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;

public class ImportFromRepoTestCase extends BaseImportTestCase {

	private static final int TYPE = PluginImportOperation.IMPORT_FROM_REPOSITORY;
	private static final String REPOSITORY_LOCATION = ":pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse";
	public ImportFromRepoTestCase(String testName) {
		setName(testName);
	}

	public static Test suite() {
		TestSuite testSuite = new TestSuite("ImportFromRepoTestCase") ;
		testSuite.addTest(new ImportFromRepoTestCase("testImportOrgEclipsePdeUaUi"));
		testSuite.addTest(new ImportFromRepoTestCase("testImportOrgEclipseRcp"));
		return testSuite;
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	public void setUp() throws CoreException {
		// Validate that we can connect to the repository
		CVSRepositoryLocation repository = (CVSRepositoryLocation)KnownRepositories.getInstance().getRepository(REPOSITORY_LOCATION);
		KnownRepositories.getInstance().addRepository(repository, false);
		repository.setUserAuthenticator(new TestsUserAuthenticator());

		// Give some info about which repository the tests are running with
		System.out.println("Connecting to: " + repository.getHost() + ":" + repository.getMethod().getName());

		try {
			try {
				repository.validateConnection(new NullProgressMonitor());
			} catch (CVSCommunicationException e) {
				// Try once more, just in case it is a transient server problem
				repository.validateConnection(new NullProgressMonitor());
			} catch (OperationCanceledException e) {
				// This can occur if authentication fails
				throw new CVSException(new CVSStatus(IStatus.ERROR, "The connection was canceled, possibly due to an authentication failure."));
			}
		} catch (CVSException e) {
			System.out.println("Unable to connect to remote repository: " + repository.toString());
			System.out.println(e.getMessage());
			throw e;
		}
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
	
	/**
	 * A test authenticator that provide defaults for all methods.
	 */
	class TestsUserAuthenticator implements IUserAuthenticator {

		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.IUserAuthenticator#promptForUserInfo(org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, org.eclipse.team.internal.ccvs.core.IUserInfo, java.lang.String)
		 */
		public void promptForUserInfo(ICVSRepositoryLocation location, IUserInfo userInfo, String message) throws CVSException {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.IUserAuthenticator#prompt(org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, int, java.lang.String, java.lang.String, int[], int)
		 */
		public int prompt(ICVSRepositoryLocation location, int promptType, String title, String message, int[] promptResponses, int defaultResponseIndex) {
			return defaultResponseIndex;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.IUserAuthenticator#promptForKeyboradInteractive(org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, java.lang.String, java.lang.String, java.lang.String, java.lang.String[], boolean[])
		 */
		public String[] promptForKeyboradInteractive(ICVSRepositoryLocation location, String destination, String name, String instruction, String[] prompt, boolean[] echo) throws CVSException {
			return prompt;
		}

	    public boolean promptForHostKeyChange(ICVSRepositoryLocation location) {
	        return false;
	    }

		public Map promptToConfigureRepositoryLocations(Map alternativeMap) {
			return null;
		}
	}


}
