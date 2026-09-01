/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

/**
 * Tests flexible bundle root location within PDE projects.
 * @since 3.6
 */
public class BundleRootTests {

	@BeforeEach
	void setUp(TestInfo testInfo) {
		testName = testInfo.getTestMethod().orElseThrow().getName();
	}

	private String testName;

	protected IBundleProjectService getBundleProjectService() {
		return PDECore.getDefault().acquireService(IBundleProjectService.class);
	}

	/**
	 * Creates and returns a project for the test case.
	 *
	 * @return project test project
	 * @exception CoreException on failure
	 */
	protected IProject createProject() throws CoreException {
		String name = testName.toLowerCase().substring(4);
		name = "test." + name;
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertFalse(proj.exists(), "Project should not exist"); //$NON-NLS-1$
		proj.create(null);
		proj.open(null);
		return proj;
	}

	/**
	 * Provides a project for the test case.
	 *
	 * @return project which does not yet exist
	 * @exception CoreException on failure
	 */
	protected IBundleProjectDescription newProject() throws CoreException {
		String name = testName.toLowerCase().substring(4);
		name = "test." + name;
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertFalse(proj.exists(), "Project should not exist"); //$NON-NLS-1$
		IBundleProjectDescription description = getBundleProjectService().getDescription(proj);
		description.setSymbolicName(proj.getName());
		return description;
	}

	/**
	 * Tests setting/getting the bundle root property for a project.
	 */
	@Test
	public void testSetGetLocation() throws CoreException {
		IProject project = createProject();
		assertEquals(project, PDEProject.getBundleRoot(project), "Bundle root unspecified - should be project itself"); //$NON-NLS-1$
		// set to something
		IFolder folder = project.getFolder(IPath.fromOSString("bundle/root"));
		PDEProject.setBundleRoot(project, folder);
		assertEquals(folder, PDEProject.getBundleRoot(project), "Wrong bundle root"); //$NON-NLS-1$
		// set to null
		PDEProject.setBundleRoot(project, null);
		assertEquals(project, PDEProject.getBundleRoot(project), "Bundle root unspecified - should be project itself"); //$NON-NLS-1$
		// set to empty project itself
		PDEProject.setBundleRoot(project, project);
		assertEquals(project, PDEProject.getBundleRoot(project), "Bundle root unspecified - should be project itself"); //$NON-NLS-1$
	}

	/**
	 * Tests setting/getting the bundle root property for a project using
	 * IBundleProjectService and IBundleProjectDescription
	 */
	@Test
	public void testServiceSetGetLocation() throws CoreException {
		IProject project = createProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		assertNull(description.getBundleRoot(), "Bundle root unspecified - should be project itself (null)"); //$NON-NLS-1$
		// set to something
		IFolder folder = project.getFolder(IPath.fromOSString("bundle/root"));
		service.setBundleRoot(project, folder.getProjectRelativePath());
		description = service.getDescription(project);
		assertEquals(folder.getProjectRelativePath(), description.getBundleRoot(), "Wrong bundle root"); //$NON-NLS-1$
		// set to null
		service.setBundleRoot(project, null);
		description = service.getDescription(project);
		assertNull(description.getBundleRoot(), "Bundle root unspecified - should be project itself (null)"); //$NON-NLS-1$
	}

	/**
	 * Test getting a root location from a non-existent project
	 */
	@Test
	public void testGetOnNonExistantProject() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testName);
		assertFalse(project.exists(), "Project should not exist"); //$NON-NLS-1$
		assertEquals(project, PDEProject.getBundleRoot(project), "Root location should be project root"); //$NON-NLS-1$
	}

	/**
	 * Tests that IPluginModel.getInstallLocation() returns the bundle root
	 * location in a project.
	 */
	@Test
	public void testPluginModelInstallLocation() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IPath root = IPath.fromOSString("some/place");
		description.setBundleRoot(root);
		IBundleClasspathEntry cp1 = getBundleProjectService().newBundleClasspathEntry(IPath.fromOSString("src"), IPath.fromOSString("bin"), IPath.fromOSString("the.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{cp1});
		IPath nls = IPath.fromOSString("plugin.properties");
		description.setLocalization(nls);
		description.apply(null);

		ProjectCreationTests.waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertEquals(project.getFolder(root).getLocation(), IPath.fromOSString(model.getInstallLocation()), "Wrong install location"); //$NON-NLS-1$
	}

	/**
	 * Minimal bundle project with a non-default root - set a symbolic name, and
	 * go.
	 */
	@Test
	public void testBundleRoot() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IPath root = IPath.fromOSString("bundle/root");
		description.setBundleRoot(root);
		IBundleClasspathEntry cp1 = getBundleProjectService().newBundleClasspathEntry(IPath.fromOSString("src"), IPath.fromOSString("bin"), IPath.fromOSString("the.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{cp1});
		IPath nls = IPath.fromOSString("plugin.properties");
		description.setLocalization(nls);
		description.setActivator("org.eclipse.foo.SomeActivator");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		assertEquals(root, d2.getBundleRoot(), "Wrong bundle root"); //$NON-NLS-1$
		assertEquals("org.eclipse.foo.SomeActivator", d2.getActivator(), "Should be no activator"); //$NON-NLS-1$ //$NON-NLS-2$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(cp1, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertEquals(nls, d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(PluginProject.NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
	}

	/**
	 * Creates a bundle project at a root location, and then removes PDE/Java
	 * natures. Then attempts create a bundle project out of the existing data.
	 */
	@Test
	public void testAssignRootToExistingProject() throws CoreException {
		testBundleRoot(); // create a simple bundle

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("test.assignroottoexistingproject");

		// remove PDE nature
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[]{JavaCore.NATURE_ID});
		project.setDescription(description, null);

		// remove existing root property
		PDEProject.setBundleRoot(project, null);

		IBundleProjectService service = getBundleProjectService();
		// reset the root
		IPath root = IPath.fromOSString("bundle/root");
		service.setBundleRoot(project, root);

		// Resurrect the bundle project, with a modified version
		IBundleProjectDescription bpd = service.getDescription(project);
		bpd.setBundleVendor("Some Vendor");
		bpd.setBundleVersion(new Version("2.0.0"));
		bpd.setNatureIds(new String[] { PluginProject.NATURE, JavaCore.NATURE_ID });
		bpd.apply(null);

		// validate
		IBundleProjectDescription d2 = service.getDescription(project);
		IPath nls = IPath.fromOSString("plugin.properties");
		IBundleClasspathEntry cp1 = service.newBundleClasspathEntry(IPath.fromOSString("src"), IPath.fromOSString("bin"), IPath.fromOSString("the.jar"));

		assertEquals(root, d2.getBundleRoot(), "Wrong bundle root"); //$NON-NLS-1$
		assertEquals("org.eclipse.foo.SomeActivator", d2.getActivator(), "Should be no activator"); //$NON-NLS-1$ //$NON-NLS-2$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(cp1, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertEquals("Some Vendor", d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("2.0.0", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertEquals(nls, d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(PluginProject.NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
	}
}
