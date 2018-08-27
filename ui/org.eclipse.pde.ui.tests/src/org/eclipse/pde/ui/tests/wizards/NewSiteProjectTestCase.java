/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.wizards;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.wizards.site.NewSiteProjectCreationOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class NewSiteProjectTestCase extends TestCase {
	private static final String EXISTING_PROJECT_NAME = "ExistingSiteProject"; //$NON-NLS-1$

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if ("testExistingSiteProject".equalsIgnoreCase(getName())) { //$NON-NLS-1$
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(EXISTING_PROJECT_NAME);
			project.create(new NullProgressMonitor());
			project.open(new NullProgressMonitor());
			IFile file = project.getFile(new Path("site.xml")); //$NON-NLS-1$
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
					+ "<site>" //$NON-NLS-1$
					+ "<category-def name=\"new_category_1\" label=\"New Category 1\"/>" //$NON-NLS-1$
					+ "</site>"; //$NON-NLS-1$
			ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes(StandardCharsets.US_ASCII));
			if (file.exists())
				file.setContents(source, true, false, new NullProgressMonitor());
			else
				file.create(source, true, new NullProgressMonitor());
			project.delete(false, true, new NullProgressMonitor());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		try {
			for (IProject project : projects) {
				project.delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			// do nothing if deletion fails. No need to fail the test.
		}
		super.tearDown();
	}

	private void createSite(IProject project, IPath path, String webLocation) throws InvocationTargetException, InterruptedException {
		NewSiteProjectCreationOperation createOperation = new NewSiteProjectCreationOperation(Display.getDefault(), project, path, webLocation);
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		progressService.runInUI(progressService, createOperation, null);
	}

	/**
	 * @param project
	 */
	private void ensureCreated(IProject project) {
		assertTrue("Project not created.", project.exists()); //$NON-NLS-1$
		assertTrue("Project not open.", project.isOpen()); //$NON-NLS-1$
		try {
			assertTrue("Site nature not added.", project //$NON-NLS-1$
					.hasNature(PDE.SITE_NATURE));
		} catch (Exception e) {
		}
		assertTrue("site.xml not created.", project //$NON-NLS-1$
				.exists(new Path("site.xml"))); //$NON-NLS-1$
		WorkspaceSiteModel model = new WorkspaceSiteModel(project.getFile(new Path("site.xml"))); //$NON-NLS-1$
		model.load();
		assertTrue("Model cannot be loaded.", model.isLoaded()); //$NON-NLS-1$
		assertTrue("Model is not valid.", model.isValid()); //$NON-NLS-1$
		assertFalse("ISite is null.", model.getSite() == null); //$NON-NLS-1$
		model.dispose();
	}

	public void testExistingSiteProject() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(EXISTING_PROJECT_NAME);
		IPath path = Platform.getLocation();
		try {
			createSite(project, path, null);
		} catch (Exception e) {
			e.printStackTrace();
			fail("testExistingSiteProject: " + e); //$NON-NLS-1$
		}
		ensureCreated(project);
		WorkspaceSiteModel model = new WorkspaceSiteModel(project.getFile(new Path("site.xml"))); //$NON-NLS-1$
		model.load();
		assertTrue("Existig site overwritten.", model.getSite() //$NON-NLS-1$
				.getCategoryDefinitions().length > 0);
		model.dispose();

	}

	public void testSiteProject() {
		String projectName = "SiteProject"; //$NON-NLS-1$
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IPath path = Platform.getLocation();
		try {
			createSite(project, path, null);
		} catch (Exception e) {
			e.printStackTrace();
			fail("testSiteProject: " + e); //$NON-NLS-1$
		}
		ensureCreated(project);
		assertFalse("index.html should have not been generated.", project //$NON-NLS-1$
				.exists(new Path("index.html"))); //$NON-NLS-1$
	}

	public void testSiteProjectWithWeb() {
		String projectName = "SiteProjectWithWeb"; //$NON-NLS-1$
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IPath path = Platform.getLocation();
		try {
			createSite(project, path, "testWeb"); //$NON-NLS-1$
		} catch (Exception e) {
			e.printStackTrace();
			fail("testSiteProjectWithWeb: " + e); //$NON-NLS-1$
		}
		ensureCreated(project);
		assertTrue("index.html not generated.", project.exists(new Path( //$NON-NLS-1$
				"index.html"))); //$NON-NLS-1$
		IFolder webFolder = project.getFolder(new Path("testWeb")); //$NON-NLS-1$
		assertTrue("Web folder not generated.", webFolder.exists()); //$NON-NLS-1$
		assertTrue("site.xsl not generated.", webFolder.exists(new Path( //$NON-NLS-1$
				"site.xsl"))); //$NON-NLS-1$
		assertTrue("site.css not generated.", webFolder.exists(new Path( //$NON-NLS-1$
				"site.css"))); //$NON-NLS-1$
	}
}
