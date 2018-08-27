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

import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.Version;

/**
 * Tests flexible bundle root location within PDE projects.
 * @since 3.6
 */
public class BundleRootTests extends TestCase {

	protected IBundleProjectService getBundleProjectService() {
		return PDECore.getDefault().acquireService(IBundleProjectService.class);
	}

	/**
	 * Creates and returns a project for the test case.
	 *
	 * @param test test case
	 * @return project test project
	 * @exception CoreException on failure
	 */
	protected IProject createProject() throws CoreException {
		String name = getName().toLowerCase().substring(4);
		name = "test." + name;
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertFalse("Project should not exist", proj.exists());
		proj.create(null);
		proj.open(null);
		return proj;
	}

	/**
	 * Provides a project for the test case.
	 *
	 * @param test test case
	 * @return project which does not yet exist
	 * @exception CoreException on failure
	 */
	protected IBundleProjectDescription newProject() throws CoreException {
		String name = getName().toLowerCase().substring(4);
		name = "test." + name;
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertFalse("Project should not exist", proj.exists());
		IBundleProjectDescription description = getBundleProjectService().getDescription(proj);
		description.setSymbolicName(proj.getName());
		return description;
	}

	/**
	 * Tests setting/getting the bundle root property for a project.
	 */
	public void testSetGetLocation() throws CoreException {
		IProject project = createProject();
		assertEquals("Bundle root unspecified - should be project itself", project, PDEProject.getBundleRoot(project));
		// set to something
		IFolder folder = project.getFolder(new Path("bundle/root"));
		PDEProject.setBundleRoot(project, folder);
		assertEquals("Wrong bundle root", folder, PDEProject.getBundleRoot(project));
		// set to null
		PDEProject.setBundleRoot(project, null);
		assertEquals("Bundle root unspecified - should be project itself", project, PDEProject.getBundleRoot(project));
		// set to empty project itself
		PDEProject.setBundleRoot(project, project);
		assertEquals("Bundle root unspecified - should be project itself", project, PDEProject.getBundleRoot(project));
	}

	/**
	 * Tests setting/getting the bundle root property for a project using IBundleProjectService and IBundleProjectDescription
	 */
	public void testServiceSetGetLocation() throws CoreException {
		IProject project = createProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		assertNull("Bundle root unspecified - should be project itself (null)", description.getBundleRoot());
		// set to something
		IFolder folder = project.getFolder(new Path("bundle/root"));
		service.setBundleRoot(project, folder.getProjectRelativePath());
		description = service.getDescription(project);
		assertEquals("Wrong bundle root", folder.getProjectRelativePath(), description.getBundleRoot());
		// set to null
		service.setBundleRoot(project, null);
		description = service.getDescription(project);
		assertNull("Bundle root unspecified - should be project itself (null)", description.getBundleRoot());
	}

	/**
	 * Test getting a root location from a non-existent project
	 */
	public void testGetOnNonExistantProject() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getName());
		assertFalse("Project should not exist", project.exists());
		assertEquals("Root location should be project root", project, PDEProject.getBundleRoot(project));
	}

	/**
	 * Tests that IPluginModel.getInstallLocation() returns the bundle root location in a project.
	 * @throws CoreException
	 */
	public void testPluginModelInstallLocation() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IPath root = new Path("some/place");
		description.setBundleRoot(root);
		IBundleClasspathEntry cp1 = getBundleProjectService().newBundleClasspathEntry(new Path("src"), new Path("bin"), new Path("the.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{cp1});
		IPath nls = new Path("plugin.properties");
		description.setLocalization(nls);
		description.apply(null);

		ProjectCreationTests.waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertEquals("Wrong install location", project.getFolder(root).getLocation(), new Path(model.getInstallLocation()));
	}

	/**
	 * Minimal bundle project with a non-default root - set a symbolic name, and go.
	 *
	 * @throws CoreException
	 */
	public void testBundleRoot() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IPath root = new Path("bundle/root");
		description.setBundleRoot(root);
		IBundleClasspathEntry cp1 = getBundleProjectService().newBundleClasspathEntry(new Path("src"), new Path("bin"), new Path("the.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{cp1});
		IPath nls = new Path("plugin.properties");
		description.setLocalization(nls);
		description.setActivator("org.eclipse.foo.SomeActivator");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		assertEquals("Wrong bundle root", root, d2.getBundleRoot());
		assertEquals("Should be no activator", "org.eclipse.foo.SomeActivator", d2.getActivator());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", cp1, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertEquals("Wrong localization", nls, d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
	}

	/**
	 * Creates a bundle project at a root location, and then removes PDE/Java natures. Then attempts create
	 * a bundle project out of the existing data.
	 *
	 * @throws CoreException
	 */
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
		Path root = new Path("bundle/root");
		service.setBundleRoot(project, root);

		// Resurrect the bundle project, with a modified version
		IBundleProjectDescription bpd = service.getDescription(project);
		bpd.setBundleVendor("Some Vendor");
		bpd.setBundleVersion(new Version("2.0.0"));
		bpd.setNatureIds(new String[]{PDE.PLUGIN_NATURE, JavaCore.NATURE_ID});
		bpd.apply(null);

		// validate
		IBundleProjectDescription d2 = service.getDescription(project);
		IPath nls = new Path("plugin.properties");
		IBundleClasspathEntry cp1 = service.newBundleClasspathEntry(new Path("src"), new Path("bin"), new Path("the.jar"));

		assertEquals("Wrong bundle root", root, d2.getBundleRoot());
		assertEquals("Should be no activator", "org.eclipse.foo.SomeActivator", d2.getActivator());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", cp1, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertEquals("Wrong Bundle-Vendor", "Some Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "2.0.0", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertEquals("Wrong localization", nls, d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
	}
}
