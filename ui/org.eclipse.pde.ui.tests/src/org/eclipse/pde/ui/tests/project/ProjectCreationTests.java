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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 525701
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.Document;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.core.project.IPackageImportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.BundleModelFactory;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Test project creation API.
 *
 * @since 3.6
 */
public class ProjectCreationTests {

	@BeforeEach
	void setUp(TestInfo testInfo) {
		testName = testInfo.getTestMethod().orElseThrow().getName();
	}

	protected static final IBundleClasspathEntry DEFAULT_BUNDLE_CLASSPATH_ENTRY = getBundleProjectService()
			.newBundleClasspathEntry(null, null, IPath.fromOSString("."));
	private static final VersionRange NO_VERSION = null;

	private String testName;

	public static IBundleProjectService getBundleProjectService() {
		return PDECore.getDefault().acquireService(IBundleProjectService.class);
	}

	/**
	 * Wait for builds to complete
	 */
	public static void waitForBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}

	/**
	 * Provides a project for the test case.
	 *
	 * @return project which does not yet exist
	 * @exception CoreException
	 *                on failure
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
	 * Minimal bundle project creation - set a symbolic name, and go.
	 */
	@Test
	public void testBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertTrue(d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE), "Nature should be present"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertTrue(d2.hasNature(JavaCore.NATURE_ID), "Nature should be present"); //$NON-NLS-1$
		assertFalse(d2.hasNature("BOGUS_NATURE"), "Should not have bogus nature"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Tests that a header can be written with an empty value.
	 */
	@Test
	public void testEmptyHeader() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setHeader("Test-Empty-Value", "");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		String value = d2.getHeader("Test-Empty-Value");
		assertNotNull(value, "Missing header 'Test-Empty-Value:'"); //$NON-NLS-1$
		assertEquals("", value, "Should be an blank header"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertTrue(d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE), "Nature should be present"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertTrue(d2.hasNature(JavaCore.NATURE_ID), "Nature should be present"); //$NON-NLS-1$
		assertFalse(d2.hasNature("BOGUS_NATURE"), "Should not have bogus nature"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Tests that an empty package import header can be tolerated (see bug
	 * 312291)
	 */
	@Test
	public void testEmptyPackageImportHeader() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setHeader(Constants.IMPORT_PACKAGE, "");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		String value = d2.getHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(value, "Missing header 'Import-Package:'"); //$NON-NLS-1$
		assertEquals("", value, "Should be a blank header"); //$NON-NLS-1$ //$NON-NLS-2$

		d2.setBundleName("EmptyTest");
		d2.apply(null);

		IBundleProjectDescription d3 = getBundleProjectService().getDescription(project);
		value = d3.getHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(value, "Missing header 'Import-Package:'"); //$NON-NLS-1$
		assertEquals("", value, "Should be a blank header"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("EmptyTest", d3.getBundleName(), "Wrong bundle name"); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Minimal fragment project creation - set a symbolic name and host, and go.
	 */
	@Test
	public void testFragment() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IHostDescription host = service.newHost("some.host", NO_VERSION);
		description.setHost(host);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(host, d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
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
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Fragment project creation with source folder and host range.
	 */
	@Test
	public void testFragmentSrc() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setBundleVersion(new Version("1.2.2"));
		IBundleProjectService service = getBundleProjectService();
		IHostDescription host = service.newHost("some.host",
				new VersionRange(VersionRange.LEFT_CLOSED, new Version("1.0.0"), new Version("2.0.0"), VersionRange.RIGHT_OPEN));
		description.setHost(host);
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(IPath.fromOSString("frag"), IPath.fromOSString("bin"),
				IPath.fromOSString("frag.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { e1 });
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(e1, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.2.2", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(host, d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
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
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Two source folders mapped to the same jar.
	 */
	@Test
	public void testTwoSourceFoldersOneJar() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(IPath.fromOSString("src1"), null, IPath.fromOSString("the.jar"));
		IBundleClasspathEntry e2 = service.newBundleClasspathEntry(IPath.fromOSString("src2"), null, IPath.fromOSString("the.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { e1, e2 });
		description.setBundleVersion(new Version("1.2.3"));
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(2, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(e1, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(e2, classpath[1], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.2.3", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$

		// validate there's only one output.the.jar entry in build.properties
		WorkspaceBuildModel properties = new WorkspaceBuildModel(PDEProject.getBuildProperties(project));
		IBuildEntry entry = properties.getBuild().getEntry("output.the.jar");
		assertNotNull(entry, "Missing output entry"); //$NON-NLS-1$
		String[] tokens = entry.getTokens();
		assertEquals(1, tokens.length, "Wrong number of output folders"); //$NON-NLS-1$
	}

	/**
	 * Test two source folders to different jars
	 */
	@Test
	public void testTwoSourceFoldersTwoJars() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(IPath.fromOSString("src1"), null, IPath.fromOSString("."));
		IBundleClasspathEntry e2 = service.newBundleClasspathEntry(IPath.fromOSString("src2"), IPath.fromOSString("bin2"),
				IPath.fromOSString("two.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { e1, e2 });
		description.setBundleVersion(new Version("1.2.3"));
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(2, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(e1, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(e2, classpath[1], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.2.3", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
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
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Set a symbolic name and singleton property, and go.
	 */
	@Test
	public void testSingleton() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		description.apply(null);
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * A simple project with a single source folder, default output folder, and
	 * bundle classpath (.).
	 */
	@Test
	public void testBundleSrc() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, List.of());
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, List.of());
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, List.of("x.y.z"));
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false,
				List.of("d.e.f", "g.h.i"));
		description.setPackageExports(new IPackageExportDescription[] { ex0, ex1, ex2, ex3 });
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertEquals(Constants.ACTIVATION_LAZY, d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull(exports, "Missing package exports"); //$NON-NLS-1$
		assertEquals(4, exports.length, "Wrong number of exports"); //$NON-NLS-1$
		assertEquals(ex0, exports[0], "Wrong package exprot"); //$NON-NLS-1$
		assertEquals(ex1, exports[1], "Wrong package exprot"); //$NON-NLS-1$
		assertEquals(ex2, exports[2], "Wrong package exprot"); //$NON-NLS-1$
		assertEquals(ex3, exports[3], "Wrong package exprot"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Convert a bundle to a fragment
	 */
	@Test
	public void testBundleToFrag() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.apply(null);

		// modify
		IBundleProjectDescription modify = service.getDescription(project);
		IHostDescription host = service.newHost("host." + project.getName(), new VersionRange("[1.0.0,2.0.0)"));
		modify.setHost(host);
		modify.apply(null);

		// validate
		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Should be no activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(host, d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * A project with a source folder, plugin.xml, activator, execution
	 * environment, required bundles, and package import.
	 */
	@Test
	public void testPlugin() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		description.setBinIncludes(new IPath[] { IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) });
		description.setActivator("org.eclipse.foo.Activator");
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.setExecutionEnvironments(new String[] { "J2SE-1.4" });
		IRequiredBundleDescription rb1 = service.newRequiredBundle("org.eclipse.core.resources",
				new VersionRange(VersionRange.LEFT_CLOSED, new Version(3, 5, 0), new Version(4, 0, 0), VersionRange.RIGHT_OPEN), true, false);
		IRequiredBundleDescription rb2 = service.newRequiredBundle("org.eclipse.core.variables", NO_VERSION, false, false);
		description.setRequiredBundles(new IRequiredBundleDescription[] { rb1, rb2 });
		IPackageImportDescription pi1 = service.newPackageImport("com.ibm.icu.text", NO_VERSION, false);
		description.setPackageImports(new IPackageImportDescription[] { pi1 });
		description.setHeader("SomeHeader", "something");
		// test version override with explicit header setting
		description.setBundleVersion(new Version("2.0.0"));
		description.setHeader(Constants.BUNDLE_VERSION, "3.2.1");
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("org.eclipse.foo.Activator", d2.getActivator(), "Wrong activator"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Constants.ACTIVATION_LAZY, d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals(1, binIncludes.length, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		assertEquals(IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0], "Wrong bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getHeader(Constants.BUNDLE_NAME), "Wrong header"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("3.2.1", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull(ees, "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(1, ees.length, "Wrong number of execution environments"); //$NON-NLS-1$
		assertEquals("J2SE-1.4", ees[0], "Wrong execution environment"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertNotNull(imports, "Wrong imports"); //$NON-NLS-1$
		assertEquals(1, imports.length, "Wrong number of package imports"); //$NON-NLS-1$
		assertEquals(pi1, imports[0], "Wrong package import"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNotNull(bundles, "Wrong required bundles"); //$NON-NLS-1$
		assertEquals(2, bundles.length, "Wrong number of required bundles"); //$NON-NLS-1$
		assertEquals(rb1, bundles[0], "Wrong required bundle"); //$NON-NLS-1$
		assertEquals(rb2, bundles[1], "Wrong required bundle"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertTrue(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertTrue(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
		assertEquals("something", d2.getHeader("SomeHeader"), "Wrong header"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull(d2.getHeader("AnotherHeader"), "Header should be missing"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Modify a simple project - change class path, add activator and
	 * plugin.xml.
	 */
	@Test
	public void testModifyBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IPath src = IPath.fromOSString("srcA");
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("a.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, List.of());
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, List.of());
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, List.of("x.y.z"));
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false,
				List.of("d.e.f", "g.h.i"));
		description.setPackageExports(new IPackageExportDescription[] { ex0, ex1, ex2, ex3 });
		description.apply(null);

		// modify the project
		IBundleProjectDescription modify = service.getDescription(project);
		IPath srcB = IPath.fromOSString("srcB");
		IBundleClasspathEntry specB = service.newBundleClasspathEntry(srcB, null, IPath.fromOSString("b.jar"));
		modify.setBundleClasspath(new IBundleClasspathEntry[] { specB });
		IPackageExportDescription ex4 = service.newPackageExport("x.y.z.interal", null, false, List.of("zz.top"));
		modify.setPackageExports(new IPackageExportDescription[] { ex0, ex2, ex4, ex3 }); // remove,
		// add,
		// re-order
		modify.setBinIncludes(new IPath[] { IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) });
		modify.setActivator("org.eclipse.foo.Activator");
		modify.setActivationPolicy(Constants.ACTIVATION_LAZY);
		modify.apply(null);

		// verify attributes
		IBundleProjectDescription d2 = service.getDescription(project);

		assertEquals("org.eclipse.foo.Activator", d2.getActivator(), "Wrong activator"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Constants.ACTIVATION_LAZY, d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals(1, binIncludes.length, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		assertEquals(IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0], "Wrong bin.includes entry"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(specB, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull(exports, "Missing exports"); //$NON-NLS-1$
		assertEquals(4, exports.length, "Wrong number of exports"); //$NON-NLS-1$
		assertEquals(ex0, exports[0], "Wrong export"); //$NON-NLS-1$
		assertEquals(ex2, exports[1], "Wrong export"); //$NON-NLS-1$
		assertEquals(ex3, exports[2], "Wrong export"); // the manifest ends up //$NON-NLS-1$
		// sorted, so order
		// changes
		assertEquals(ex4, exports[3], "Wrong export"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Modify a simple project to add/remove/clear some entries. See bug 380444
	 * where previous settings weren't being cleared
	 */
	@Test
	public void testModifyRequiredBundles() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();

		IRequiredBundleDescription requireDesc = service.newRequiredBundle("requiredBundleOne", NO_VERSION, false, false);
		IRequiredBundleDescription requireDesc2 = service.newRequiredBundle("requiredBundleTwo",
				new VersionRange("[1.0.0,2.0.0)"), false, false);
		IRequiredBundleDescription requireDesc3 = service.newRequiredBundle("requiredBundleThree", NO_VERSION, true, false);
		IRequiredBundleDescription requireDesc4 = service.newRequiredBundle("requiredBundleFour", NO_VERSION, false, true);
		description.setRequiredBundles(
				new IRequiredBundleDescription[] { requireDesc, requireDesc2, requireDesc3, requireDesc4 });

		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, List.of());
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, List.of());
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, List.of("x.y.z"));
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false,
				List.of("d.e.f", "g.h.i"));
		description.setPackageExports(new IPackageExportDescription[] { ex0, ex1, ex2, ex3 });

		IPackageImportDescription importDesc = service.newPackageImport("importPkgOne", NO_VERSION, false);
		IPackageImportDescription importDesc2 = service.newPackageImport("importPkgTwo",
				new VersionRange("[1.0.0,2.0.0)"), false);
		IPackageImportDescription importDesc3 = service.newPackageImport("importPkgThree", NO_VERSION, true);
		IPackageImportDescription importDesc4 = service.newPackageImport("importPkgFour", NO_VERSION, false);
		description.setPackageImports(
				new IPackageImportDescription[] { importDesc, importDesc2, importDesc3, importDesc4 });

		description.apply(null);

		// verify attributes
		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals(4, d2.getRequiredBundles().length, "Wrong number of required bundles"); //$NON-NLS-1$
		assertEquals(4, d2.getPackageExports().length, "Wrong number of package exports"); //$NON-NLS-1$
		assertEquals(4, d2.getPackageImports().length, "Wrong number of package imports"); //$NON-NLS-1$

		// add entries
		IRequiredBundleDescription requireDesc5 = service.newRequiredBundle("requiredBundleFive", NO_VERSION, false, false);
		IRequiredBundleDescription requireDesc6 = service.newRequiredBundle("requiredBundleSix", NO_VERSION, false, false);
		description.setRequiredBundles(new IRequiredBundleDescription[] { requireDesc, requireDesc2, requireDesc3,
				requireDesc4, requireDesc5, requireDesc6 });

		IPackageExportDescription ex4 = service.newPackageExport("a.b.c.interal.x2", null, false, List.of("x.y.z"));
		IPackageExportDescription ex5 = service.newPackageExport("a.b.c.interal.y2", new Version("1.2.3"), false,
				List.of("d.e.f", "g.h.i"));
		description.setPackageExports(new IPackageExportDescription[] { ex0, ex1, ex2, ex3, ex4, ex5 });

		IPackageImportDescription importDesc5 = service.newPackageImport("importPkgFive", NO_VERSION, true);
		IPackageImportDescription importDesc6 = service.newPackageImport("importPkgSix", NO_VERSION, false);
		description.setPackageImports(new IPackageImportDescription[] { importDesc, importDesc2, importDesc3,
				importDesc4, importDesc5, importDesc6 });

		description.apply(null);

		// verify attributes
		IBundleProjectDescription d3 = service.getDescription(project);
		assertEquals(6, d3.getRequiredBundles().length, "Wrong number of required bundles after additions"); //$NON-NLS-1$
		assertEquals(6, d3.getPackageExports().length, "Wrong number of package exports after addtions"); //$NON-NLS-1$
		assertEquals(6, d3.getPackageImports().length, "Wrong number of package imports after additions"); //$NON-NLS-1$

		// remove most entries
		description.setRequiredBundles(new IRequiredBundleDescription[] { requireDesc2, requireDesc5 });
		description.setPackageExports(new IPackageExportDescription[] { ex1, ex4 });
		description.setPackageImports(new IPackageImportDescription[] { importDesc2, importDesc5 });
		description.apply(null);

		// verify attributes
		IBundleProjectDescription d4 = service.getDescription(project);
		assertEquals(2, d4.getRequiredBundles().length, "Wrong number of required bundles after removals"); //$NON-NLS-1$
		assertEquals(2, d4.getPackageExports().length, "Wrong number of package exports after removals"); //$NON-NLS-1$
		assertEquals(2, d4.getPackageImports().length, "Wrong number of package imports after removals"); //$NON-NLS-1$

		// clear entries
		description.setRequiredBundles(null);
		description.setPackageExports(null);
		description.setPackageImports(null);
		description.apply(null);

		// verify attributes
		IBundleProjectDescription d5 = service.getDescription(project);
		assertNull(d5.getRequiredBundles(), "Wrong number of required bundles after removals"); //$NON-NLS-1$
		assertNull(d5.getPackageExports(), "Wrong number of package exports after removals"); //$NON-NLS-1$
		assertNull(d5.getPackageImports(), "Wrong number of package imports after removals"); //$NON-NLS-1$
	}

	/**
	 * Convert a fragment into a bundle
	 */
	@Test
	public void testFragToBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IHostDescription host = service.newHost("some.host", NO_VERSION);
		description.setHeader("HeaderOne", "one"); // arbitrary header
		description.setHost(host);
		description.apply(null);

		// modify to a bundle and remove a header
		IBundleProjectDescription modify = service.getDescription(project);
		assertEquals("one", modify.getHeader("HeaderOne"), "Wrong header value"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		modify.setHeader("HeaderOne", null);
		modify.setHost(null);
		modify.apply(null);

		// validate
		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getHeader("HeaderOne"), "Header should be removed"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
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
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Tests creating a project that simply wraps jars into a bundle.
	 */
	@Test
	public void testJarsAsBundle() throws CoreException, IOException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry one = service.newBundleClasspathEntry(null, null, IPath.fromOSString("one.jar"));
		IBundleClasspathEntry two = service.newBundleClasspathEntry(null, null, IPath.fromOSString("lib/two.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { one, two });
		IPackageExportDescription exp1 = service.newPackageExport("org.eclipse.one", new Version("1.0.0"), true, List.of());
		IPackageExportDescription exp2 = service.newPackageExport("org.eclipse.two", new Version("1.0.0"), true, List.of());
		description.setPackageExports(new IPackageExportDescription[] { exp1, exp2 });
		description.setBundleVersion(new Version("1.0.0"));
		description.setExecutionEnvironments(new String[] { "J2SE-1.5" });
		description.apply(null);
		// create bogus jar files
		createBogusJar(project.getFile("one.jar"));
		createBogusJar(project.getFile(IPath.fromOSString("lib/two.jar")));

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(2, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(one, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(two, classpath[1], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals(new Version("1.0.0"), d2.getBundleVersion(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull(ees, "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(1, ees.length, "Wrong number of execution environments"); //$NON-NLS-1$
		assertEquals("J2SE-1.5", ees[0], "Wrong execution environments"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull(exports, "Wrong exports"); //$NON-NLS-1$
		assertEquals(2, exports.length, "Wrong number of exports"); //$NON-NLS-1$
		assertEquals(exp1, exports[0], "Wrong exports"); //$NON-NLS-1$
		assertEquals(exp2, exports[1], "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Creates a file with some content at the given location.
	 */
	protected void createBogusJar(IFile file) throws CoreException, IOException {
		IContainer parent = file.getParent();
		while (parent instanceof IFolder) {
			if (!parent.exists()) {
				((IFolder) parent).create(false, true, null);
			}
			parent = parent.getParent();
		}
		URL zipURL = FrameworkUtil.getBundle(ProjectCreationTests.class).getEntry("tests/A.jar");
		try (InputStream stream = zipURL.openStream()) {
			file.create(stream, false, null);
		}
	}

	/**
	 * Tests creating a project that simply wraps jars into a bundle.
	 */
	@Test
	public void testClassFolders() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry one = service.newBundleClasspathEntry(null, IPath.fromOSString("bin1"), IPath.fromOSString("one.jar"));
		IBundleClasspathEntry two = service.newBundleClasspathEntry(null, IPath.fromOSString("bin2"), IPath.fromOSString("two.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { one, two });
		IPackageExportDescription exp1 = service.newPackageExport("org.eclipse.one", new Version("1.0.0"), true, List.of());
		IPackageExportDescription exp2 = service.newPackageExport("org.eclipse.two", new Version("1.0.0"), true, List.of());
		description.setPackageExports(new IPackageExportDescription[] { exp1, exp2 });
		description.setBundleVersion(new Version("1.0.0"));
		description.setExecutionEnvironments(new String[] { "J2SE-1.5" });
		description.apply(null);
		// create folders
		project.getFolder("bin1").create(false, true, null);
		project.getFolder("bin2").create(false, true, null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(2, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(one, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(two, classpath[1], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals(new Version("1.0.0"), d2.getBundleVersion(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull(ees, "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(1, ees.length, "Wrong number of execution environments"); //$NON-NLS-1$
		assertEquals("J2SE-1.5", ees[0], "Wrong execution environments"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull(exports, "Wrong exports"); //$NON-NLS-1$
		assertEquals(2, exports.length, "Wrong number of exports"); //$NON-NLS-1$
		assertEquals(exp1, exports[0], "Wrong exports"); //$NON-NLS-1$
		assertEquals(exp2, exports[1], "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$
	}

	/**
	 * Test custom export wizard and launch shortcuts.
	 */
	@Test
	public void testExportWizardLaunchShortcuts() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setLaunchShortcuts(new String[] { "org.eclipse.jdt.debug.ui.javaAppletShortcut" });
		description.setExportWizardId("org.eclipse.debug.internal.ui.importexport.breakpoints.WizardExportBreakpoints");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
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
		assertEquals("org.eclipse.debug.internal.ui.importexport.breakpoints.WizardExportBreakpoints", d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] ids = d2.getLaunchShortcuts();
		assertNotNull(ids, "Wrong launch shortctus"); //$NON-NLS-1$
		assertEquals(1, ids.length, "Wrong number of shortcuts"); //$NON-NLS-1$
		assertEquals(ids[0], "org.eclipse.jdt.debug.ui.javaAppletShortcut"); //$NON-NLS-1$
	}

	/**
	 * Targeting 3.1, should get result it Eclipse-AutoStart: true
	 */
	@Test
	public void testLazyAutostart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		description.setBinIncludes(new IPath[] { IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) });
		description.setActivator("org.eclipse.foo.Activator");
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_1);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.setExecutionEnvironments(new String[] { "J2SE-1.4" });
		IRequiredBundleDescription rb1 = service.newRequiredBundle("org.eclipse.core.resources",
				new VersionRange(VersionRange.LEFT_CLOSED, new Version(3, 5, 0), new Version(4, 0, 0), VersionRange.RIGHT_OPEN), true, false);
		IRequiredBundleDescription rb2 = service.newRequiredBundle("org.eclipse.core.variables", NO_VERSION, false, false);
		description.setRequiredBundles(new IRequiredBundleDescription[] { rb1, rb2 });
		IPackageImportDescription pi1 = service.newPackageImport("com.ibm.icu.text", NO_VERSION, false);
		description.setPackageImports(new IPackageImportDescription[] { pi1 });
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("org.eclipse.foo.Activator", d2.getActivator(), "Wrong activator"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Constants.ACTIVATION_LAZY, d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals(1, binIncludes.length, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		assertEquals(IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0], "Wrong bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull(ees, "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(1, ees.length, "Wrong number of execution environments"); //$NON-NLS-1$
		assertEquals("J2SE-1.4", ees[0], "Wrong execution environment"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertNotNull(imports, "Wrong imports"); //$NON-NLS-1$
		assertEquals(1, imports.length, "Wrong number of package imports"); //$NON-NLS-1$
		assertEquals(pi1, imports[0], "Wrong package import"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNotNull(bundles, "Wrong required bundles"); //$NON-NLS-1$
		assertEquals(2, bundles.length, "Wrong number of required bundles"); //$NON-NLS-1$
		assertEquals(rb1, bundles[0], "Wrong required bundle"); //$NON-NLS-1$
		assertEquals(rb2, bundles[1], "Wrong required bundle"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertTrue(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertTrue(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$

		// ensure proper header was generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull(model, "Missing plugin model"); //$NON-NLS-1$
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_AUTOSTART);
		assertNotNull(header, "Missing header"); //$NON-NLS-1$
	}

	/**
	 * Returns a structured header from a bundle model
	 *
	 * @param bundle
	 *            the bundle
	 * @param header
	 *            header name/key
	 * @return header or <code>null</code>
	 */
	private IManifestHeader createHeader(IBundle bundle, String header) {
		BundleModelFactory factory = new BundleModelFactory(bundle.getModel());
		String headerValue = bundle.getHeader(header);
		if (headerValue == null) {
			return null;
		}
		return factory.createHeader(header, headerValue);
	}

	/**
	 * Targeting 3.1, eager bundle should omit Eclipse-AutoStart: header
	 */
	@Test
	public void testEagerAutostart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		description.setBinIncludes(new IPath[] { IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) });
		description.setActivator("org.eclipse.foo.Activator");
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_1);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("org.eclipse.foo.Activator", d2.getActivator(), "Wrong activator"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals(1, binIncludes.length, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		assertEquals(IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0], "Wrong bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertNull(imports, "Wrong imports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNull(bundles, "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertTrue(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertTrue(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$

		// ensure header was *not* generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull(model, "Missing plugin model"); //$NON-NLS-1$
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_AUTOSTART);
		assertNull(header, "Header should not be present"); //$NON-NLS-1$
	}

	/**
	 * Targeting 3.2, lazy bundle should have Eclipse-LazyStart: header
	 */
	@Test
	public void testLazyEclipseLazyStart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		description.setBinIncludes(new IPath[] { IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) });
		description.setActivator("org.eclipse.foo.Activator");
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_2);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("org.eclipse.foo.Activator", d2.getActivator(), "Wrong activator"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Constants.ACTIVATION_LAZY, d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals(1, binIncludes.length, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		assertEquals(IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0], "Wrong bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertNull(imports, "Wrong imports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNull(bundles, "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertTrue(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertTrue(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$

		// ensure header was generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull(model, "Missing plugin model"); //$NON-NLS-1$
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_LAZYSTART);
		assertNotNull(header, "Header should be present"); //$NON-NLS-1$
	}

	/**
	 * Targeting 3.2, eager bundle should not have Eclipse-LazyStart: header
	 */
	@Test
	public void testEagerEclipseLazyStart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = IPath.fromOSString("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { spec });
		description.setBinIncludes(new IPath[] { IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) });
		description.setActivator("org.eclipse.foo.Activator");
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_2);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("org.eclipse.foo.Activator", d2.getActivator(), "Wrong activator"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals(1, binIncludes.length, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		assertEquals(IPath.fromOSString(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0], "Wrong bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Bundle-Classpath should be specified"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of bundle classpath entries"); //$NON-NLS-1$
		assertEquals(classpath[0], spec, "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getExecutionEnvironments(), "Wrong execution environments"); //$NON-NLS-1$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull(d2.getPackageExports(), "Wrong exports"); //$NON-NLS-1$
		assertNull(imports, "Wrong imports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNull(bundles, "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertTrue(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertTrue(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertTrue(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$

		// ensure header was generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull(model, "Missing plugin model"); //$NON-NLS-1$
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_LAZYSTART);
		assertNull(header, "Header should not be present"); //$NON-NLS-1$
	}

	/**
	 * Returns the given input stream's contents as a character array. If a
	 * length is specified (i.e. if length != -1), this represents the number of
	 * bytes in the stream. Note the specified stream is not closed in this
	 * method
	 *
	 * @param stream
	 *            the stream to get convert to the char array
	 * @return the given input stream's contents as a character array.
	 * @throws IOException
	 *             if a problem occurred reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream) throws IOException {
		Charset charset = StandardCharsets.UTF_8;
		CharsetDecoder charsetDecoder = charset.newDecoder();
		charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		byte[] contents = getInputStreamAsByteArray(stream);
		ByteBuffer byteBuffer = ByteBuffer.allocate(contents.length);
		byteBuffer.put(contents);
		byteBuffer.flip();
		return charsetDecoder.decode(byteBuffer).array();
	}

	/**
	 * Returns the given input stream as a byte array
	 *
	 * @param stream
	 *            the stream to get as a byte array
	 * @return the given input stream as a byte array
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream) throws IOException {
		try (stream) {
			return stream.readAllBytes();
		}
	}

	/**
	 * Tests that package import/export headers don't get flattened when doing
	 * an unrelated edit.
	 */
	@Test
	public void testHeaderFormatting() throws CoreException, IOException {
		IBundleProjectDescription description = newProject();
		IPackageImportDescription imp1 = getBundleProjectService().newPackageImport("org.eclipse.osgi", NO_VERSION, false);
		IPackageImportDescription imp2 = getBundleProjectService().newPackageImport("org.eclipse.core.runtime", NO_VERSION, false);
		IPackageImportDescription imp3 = getBundleProjectService().newPackageImport("org.eclipse.core.resources", NO_VERSION, false);
		description.setPackageImports(new IPackageImportDescription[] { imp1, imp2, imp3 });
		IPackageExportDescription ex1 = getBundleProjectService().newPackageExport("a.b.c", null, true, List.of());
		IPackageExportDescription ex2 = getBundleProjectService().newPackageExport("a.b.c.d", null, true, List.of());
		IPackageExportDescription ex3 = getBundleProjectService().newPackageExport("a.b.c.e", null, true, List.of());
		description.setPackageExports(new IPackageExportDescription[] { ex1, ex2, ex3 });
		IProject project = description.getProject();
		description.apply(null);

		// should be 12 lines
		IFile manifest = PDEProject.getManifest(project);
		char[] chars = getInputStreamAsCharArray(manifest.getContents());
		Document document = new Document(new String(chars));
		int lines = document.getNumberOfLines();
		assertEquals(12, lines, "Wrong number of lines"); //$NON-NLS-1$

		// modify version attribute
		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);
		d2.setBundleVersion(new Version("2.0.0"));
		d2.apply(null);

		// should be 12 lines
		manifest = PDEProject.getManifest(project);
		chars = getInputStreamAsCharArray(manifest.getContents());
		document = new Document(new String(chars));
		lines = document.getNumberOfLines();
		assertEquals(12, lines, "Wrong number of lines"); //$NON-NLS-1$
	}

	/**
	 * Changes a non-plug-in project into a a plug-in.
	 */
	@Test
	public void testNonBundleToBundle() throws CoreException {
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject("test.non.bundle.to.bundle");
		assertFalse(proj.exists(), "Project should not exist"); //$NON-NLS-1$
		proj.create(null);
		proj.open(null);
		IProjectDescription pd = proj.getDescription();
		pd.setNatureIds(new String[] { JavaCore.NATURE_ID });
		proj.setDescription(pd, null);

		IBundleProjectDescription description = getBundleProjectService().getDescription(proj);
		assertTrue(description.hasNature(JavaCore.NATURE_ID), "Missing Java Nature"); //$NON-NLS-1$
		description.setSymbolicName("test.non.bundle.to.bundle");
		description.setNatureIds(new String[] { IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID });
		description.apply(null);

		// validate
		IBundleProjectDescription d2 = getBundleProjectService().getDescription(proj);
		assertEquals(proj.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertTrue(d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE), "Nature should be present"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$

	}

	/**
	 * Convert an existing Java project into a bundle project. Ensure it's build
	 * path doesn't get toasted in the process.
	 */
	@Test
	public void testJavaToBundle() throws CoreException {
		// create a Java project
		String name = testName.toLowerCase().substring(4);
		name = "test." + name;
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertFalse(proj.exists(), "Project should not exist"); //$NON-NLS-1$
		proj.create(null);
		proj.open(null);
		IProjectDescription pd = proj.getDescription();
		pd.setNatureIds(new String[] { JavaCore.NATURE_ID });
		proj.setDescription(pd, null);
		IFolder src = proj.getFolder("someSrc");
		src.create(false, true, null);
		IFolder output = proj.getFolder("someBin");
		output.create(false, true, null);
		IJavaProject javaProject = JavaCore.create(proj);
		javaProject.setOutputLocation(output.getFullPath(), null);
		IClasspathEntry entry1 = JavaCore.newSourceEntry(src.getFullPath());
		IClasspathEntry entry2 = JavaCore.newContainerEntry(JavaRuntime
				.newJREContainerPath(JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4")));
		IClasspathEntry entry3 = JavaCore.newContainerEntry(ClasspathContainerInitializer.PATH);
		javaProject.setRawClasspath(new IClasspathEntry[] { entry1, entry2, entry3 }, null);

		// convert to a bundle
		IBundleProjectDescription description = getBundleProjectService().getDescription(proj);
		assertTrue(description.hasNature(JavaCore.NATURE_ID), "Missing Java Nature"); //$NON-NLS-1$
		description.setSymbolicName(proj.getName());
		description.setNatureIds(new String[] { IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID });
		IBundleClasspathEntry entry = getBundleProjectService().newBundleClasspathEntry(src.getProjectRelativePath(),
				null, null);
		description.setBundleClasspath(new IBundleClasspathEntry[] { entry });
		description.apply(null);

		// validate
		IBundleProjectDescription d2 = getBundleProjectService().getDescription(proj);
		assertEquals(proj.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertTrue(d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE), "Nature should be present"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		// execution environment should be that on the Java build path
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull(ees, "Missing EEs"); //$NON-NLS-1$
		assertEquals(1, ees.length, "Wrong number of EEs"); //$NON-NLS-1$
		assertEquals("J2SE-1.4", ees[0], "Wrong EE"); //$NON-NLS-1$ //$NON-NLS-2$
		// version
		assertEquals("1.0.0.qualifier", d2.getBundleVersion().toString(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$

		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(getBundleProjectService().newBundleClasspathEntry(src.getProjectRelativePath(), null, IPath.fromOSString(".")), classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$ //$NON-NLS-2$

		// raw class path should still be intact
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		assertEquals(4, rawClasspath.length, "Wrong number of entries"); //$NON-NLS-1$
		assertEquals(entry1, rawClasspath[0], "Wrong entry"); //$NON-NLS-1$
		assertEquals(entry2, rawClasspath[1], "Wrong entry"); //$NON-NLS-1$
		assertEquals(entry3, rawClasspath[2], "Wrong entry"); //$NON-NLS-1$
		assertEquals(ClasspathComputer.createContainerEntry(), rawClasspath[3], "Missing Required Plug-ins Container"); //$NON-NLS-1$
	}

	/**
	 * Tests creating a project that has a nested class file folders instead of
	 * a jar
	 */
	@Test
	public void testClassFoldersNoJars() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry one = service.newBundleClasspathEntry(IPath.fromOSString("src"),
				IPath.fromOSString("WebContent/WEB-INF/classes"), IPath.fromOSString("WebContent/WEB-INF/classes"));
		description.setBundleClasspath(new IBundleClasspathEntry[] { one });
		IPackageExportDescription exp1 = service.newPackageExport("org.eclipse.one", new Version("1.0.0"), true, List.of());
		IPackageExportDescription exp2 = service.newPackageExport("org.eclipse.two", new Version("1.0.0"), true, List.of());
		description.setPackageExports(new IPackageExportDescription[] { exp1, exp2 });
		description.setBundleVersion(new Version("1.0.0"));
		description.setExecutionEnvironments(new String[] { "J2SE-1.5" });
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull(d2.getActivator(), "Should be no activator"); //$NON-NLS-1$
		assertNull(d2.getActivationPolicy(), "Wrong activation policy"); //$NON-NLS-1$
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull(binIncludes, "Wrong number of entries on bin.includes"); //$NON-NLS-1$
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull(classpath, "Wrong Bundle-Classpath"); //$NON-NLS-1$
		assertEquals(1, classpath.length, "Wrong number of Bundle-Classpath entries"); //$NON-NLS-1$
		assertEquals(one, classpath[0], "Wrong Bundle-Classpath entry"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getBundleName(), "Wrong Bundle-Name"); //$NON-NLS-1$
		assertNull(d2.getBundleVendor(), "Wrong Bundle-Vendor"); //$NON-NLS-1$
		assertEquals(new Version("1.0.0"), d2.getBundleVersion(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IPath.fromOSString("bin"), d2.getDefaultOutputFolder(), "Wrong default output folder"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull(ees, "Wrong execution environments"); //$NON-NLS-1$
		assertEquals(1, ees.length, "Wrong number of execution environments"); //$NON-NLS-1$
		assertEquals("J2SE-1.5", ees[0], "Wrong execution environments"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(d2.getHost(), "Wrong host"); //$NON-NLS-1$
		assertNull(d2.getLocalization(), "Wrong localization"); //$NON-NLS-1$
		assertNull(d2.getLocationURI(), "Wrong project location URI"); //$NON-NLS-1$
		String[] natureIds = d2.getNatureIds();
		assertEquals(2, natureIds.length, "Wrong number of natures"); //$NON-NLS-1$
		assertEquals(IBundleProjectDescription.PLUGIN_NATURE, natureIds[0], "Wrong nature"); //$NON-NLS-1$
		assertEquals(JavaCore.NATURE_ID, natureIds[1], "Wrong nature"); //$NON-NLS-1$
		assertNull(d2.getPackageImports(), "Wrong imports"); //$NON-NLS-1$
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull(exports, "Wrong exports"); //$NON-NLS-1$
		assertEquals(2, exports.length, "Wrong number of exports"); //$NON-NLS-1$
		assertEquals(exp1, exports[0], "Wrong exports"); //$NON-NLS-1$
		assertEquals(exp2, exports[1], "Wrong exports"); //$NON-NLS-1$
		assertEquals(project, d2.getProject(), "Wrong project"); //$NON-NLS-1$
		assertNull(d2.getRequiredBundles(), "Wrong required bundles"); //$NON-NLS-1$
		assertNull(d2.getTargetVersion(), "Wrong target version"); //$NON-NLS-1$
		assertEquals(project.getName(), d2.getSymbolicName(), "Wrong symbolic name"); //$NON-NLS-1$
		assertFalse(d2.isExtensionRegistry(), "Wrong extension registry support"); //$NON-NLS-1$
		assertFalse(d2.isEquinox(), "Wrong Equinox headers"); //$NON-NLS-1$
		assertFalse(d2.isSingleton(), "Wrong singleton"); //$NON-NLS-1$
		assertNull(d2.getExportWizardId(), "Wrong export wizard"); //$NON-NLS-1$
		assertNull(d2.getLaunchShortcuts(), "Wrong launch shortctus"); //$NON-NLS-1$

		// should be no warnings on build.properties
		IFile file = PDEProject.getBuildProperties(project);
		IMarker[] markers = file.findMarkers(PDEMarkerFactory.MARKER_ID, true, 0);
		assertEquals(0, markers.length, "Should be no errors"); //$NON-NLS-1$
	}

	/**
	 * Tests that adding package exports incrementally works
	 */
	@Test
	public void testExportUpdateSequence() throws CoreException {
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IPackageExportDescription e1 = service.newPackageExport("a.b.c", null, true, List.of());
		description.setPackageExports(new IPackageExportDescription[] { e1 });
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		IPackageExportDescription e2 = service.newPackageExport("a.b.c.internal", null, false, List.of());
		d2.setPackageExports(new IPackageExportDescription[] { e1, e2 });
		d2.apply(null);

		IBundleProjectDescription d3 = service.getDescription(project);
		IPackageExportDescription[] exports = d3.getPackageExports();
		assertNotNull(exports, "Wrong exports"); //$NON-NLS-1$
		assertEquals(2, exports.length, "Wrong number of exports"); //$NON-NLS-1$
		assertEquals(e1, exports[0], "Wrong package export"); //$NON-NLS-1$
		assertEquals(e2, exports[1], "Wrong package export"); //$NON-NLS-1$
	}
}
