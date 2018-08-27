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

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.Document;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.project.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.BundleModelFactory;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Test project creation API.
 *
 * @since 3.6
 */
public class ProjectCreationTests extends TestCase {

	protected static final IBundleClasspathEntry DEFAULT_BUNDLE_CLASSPATH_ENTRY;

	static {
		DEFAULT_BUNDLE_CLASSPATH_ENTRY = getBundleProjectService().newBundleClasspathEntry(null, null, new Path("."));
	}

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
	 * Minimal bundle project creation - set a symbolic name, and go.
	 *
	 * @throws CoreException
	 */
	public void testBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertTrue("Nature should be present", d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE));
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertTrue("Nature should be present", d2.hasNature(JavaCore.NATURE_ID));
		assertFalse("Should not have bogus nature", d2.hasNature("BOGUS_NATURE"));
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Tests that a header can be written with an empty value.
	 *
	 * @throws CoreException
	 */
	public void testEmptyHeader() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setHeader("Test-Empty-Value", "");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		String value = d2.getHeader("Test-Empty-Value");
		assertNotNull("Missing header 'Test-Empty-Value:'", value);
		assertEquals("Should be an blank header", "", value);
		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertTrue("Nature should be present", d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE));
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertTrue("Nature should be present", d2.hasNature(JavaCore.NATURE_ID));
		assertFalse("Should not have bogus nature", d2.hasNature("BOGUS_NATURE"));
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Tests that an empty package import header can be tolerated (see bug 312291)
	 *
	 * @throws CoreException
	 */
	public void testEmptyPackageImportHeader() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setHeader(Constants.IMPORT_PACKAGE, "");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		String value = d2.getHeader(Constants.IMPORT_PACKAGE);
		assertNotNull("Missing header 'Import-Package:'", value);
		assertEquals("Should be a blank header", "", value);

		d2.setBundleName("EmptyTest");
		d2.apply(null);

		IBundleProjectDescription d3 = getBundleProjectService().getDescription(project);
		value = d3.getHeader(Constants.IMPORT_PACKAGE);
		assertNotNull("Missing header 'Import-Package:'", value);
		assertEquals("Should be a blank header", "", value);
		assertEquals("Wrong bundle name", "EmptyTest", d3.getBundleName());

	}

	/**
	 * Minimal fragment project creation - set a symbolic name and host, and go.
	 *
	 * @throws CoreException
	 */
	public void testFragment() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IHostDescription host = service.newHost("some.host", null);
		description.setHost(host);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertEquals("Wrong host", host, d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
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
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Fragment project creation with source folder and host range.
	 *
	 * @throws CoreException
	 */
	public void testFragmentSrc() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setBundleVersion(new Version("1.2.2"));
		IBundleProjectService service = getBundleProjectService();
		IHostDescription host = service.newHost("some.host", new VersionRange(new Version("1.0.0"), true, new Version("2.0.0"), false));
		description.setHost(host);
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(new Path("frag"), new Path("bin"), new Path("frag.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{e1});
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", e1, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.2.2", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertEquals("Wrong host", host, d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
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
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Two source folders mapped to the same jar.
	 *
	 * @throws CoreException
	 */
	public void testTwoSourceFoldersOneJar() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(new Path("src1"), null, new Path("the.jar"));
		IBundleClasspathEntry e2 = service.newBundleClasspathEntry(new Path("src2"), null, new Path("the.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{e1, e2});
		description.setBundleVersion(new Version("1.2.3"));
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 2, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", e1, classpath[0]);
		assertEquals("Wrong Bundle-Classpath entry", e2, classpath[1]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.2.3", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());

		// validate there's only one output.the.jar entry in build.properties
		WorkspaceBuildModel properties = new WorkspaceBuildModel(PDEProject.getBuildProperties(project));
		IBuildEntry entry = properties.getBuild().getEntry("output.the.jar");
		assertNotNull("Missing output entry", entry);
		String[] tokens = entry.getTokens();
		assertEquals("Wrong number of output folders", 1, tokens.length);
	}

	/**
	 * Test two source folders to different jars
	 *
	 * @throws CoreException
	 */
	public void testTwoSourceFoldersTwoJars() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(new Path("src1"), null, new Path("."));
		IBundleClasspathEntry e2 = service.newBundleClasspathEntry(new Path("src2"), new Path("bin2"), new Path("two.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{e1, e2});
		description.setBundleVersion(new Version("1.2.3"));
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 2, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", e1, classpath[0]);
		assertEquals("Wrong Bundle-Classpath entry", e2, classpath[1]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.2.3", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
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
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}
	/**
	 * Set a symbolic name and singleton property, and go.
	 *
	 * @throws CoreException
	 */
	public void testSingleton() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		description.apply(null);
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * A simple project with a single source folder, default output folder, and bundle classpath (.).
	 *
	 * @throws CoreException
	 */
	public void testBundleSrc() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, null);
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, null);
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, new String[]{"x.y.z"});
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false, new String[]{"d.e.f", "g.h.i"});
		description.setPackageExports(new IPackageExportDescription[]{ex0, ex1, ex2, ex3});
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull("Should be no activator", d2.getActivator());
		assertEquals("Wrong activation policy", Constants.ACTIVATION_LAZY, d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull("Missing package exports", exports);
		assertEquals("Wrong number of exports", 4, exports.length);
		assertEquals("Wrong package exprot", ex0, exports[0]);
		assertEquals("Wrong package exprot", ex1, exports[1]);
		assertEquals("Wrong package exprot", ex2, exports[2]);
		assertEquals("Wrong package exprot", ex3, exports[3]);
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Convert a bundle to a fragment
	 *
	 * @throws CoreException
	 */
	public void testBundleToFrag() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.apply(null);

		// modify
		IBundleProjectDescription modify = service.getDescription(project);
		IHostDescription host = service.newHost("host." + project.getName(), new VersionRange("[1.0.0,2.0.0)"));
		modify.setHost(host);
		modify.apply(null);

		// validate
		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull("Should be no activator", d2.getActivator());
		assertNull("Should be no activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertEquals("Wrong host", host, d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * A project with a source folder, plugin.xml, activator, execution environment,
	 * required bundles, and package import.
	 *
	 * @throws CoreException
	 */
	public void testPlugin() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		description.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		description.setActivator("org.eclipse.foo.Activator");
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.setExecutionEnvironments(new String[]{"J2SE-1.4"});
		IRequiredBundleDescription rb1 = service.newRequiredBundle(
				"org.eclipse.core.resources",
				new VersionRange(new Version(3,5,0), true, new Version(4,0,0), false),
				true, false);
		IRequiredBundleDescription rb2 = service.newRequiredBundle("org.eclipse.core.variables", null, false, false);
		description.setRequiredBundles(new IRequiredBundleDescription[]{rb1, rb2});
		IPackageImportDescription pi1 = service.newPackageImport("com.ibm.icu.text", null, false);
		description.setPackageImports(new IPackageImportDescription[]{pi1});
		description.setHeader("SomeHeader", "something");
		// test version override with explicit header setting
		description.setBundleVersion(new Version("2.0.0"));
		description.setHeader(Constants.BUNDLE_VERSION, "3.2.1");
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("Wrong activator", "org.eclipse.foo.Activator", d2.getActivator());
		assertEquals("Wrong activation policy", Constants.ACTIVATION_LAZY, d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertEquals("Wrong header", project.getName(), d2.getHeader(Constants.BUNDLE_NAME));
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "3.2.1", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull("Wrong execution environments", ees);
		assertEquals("Wrong number of execution environments", 1, ees.length);
		assertEquals("Wrong execution environment", "J2SE-1.4", ees[0]);
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull("Wrong exports", d2.getPackageExports());
		assertNotNull("Wrong imports", imports);
		assertEquals("Wrong number of package imports", 1, imports.length);
		assertEquals("Wrong package import", pi1, imports[0]);
		assertEquals("Wrong project", project, d2.getProject());
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNotNull("Wrong required bundles", bundles);
		assertEquals("Wrong number of required bundles", 2, bundles.length);
		assertEquals("Wrong required bundle", rb1, bundles[0]);
		assertEquals("Wrong required bundle", rb2, bundles[1]);
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertTrue("Wrong extension registry support", d2.isExtensionRegistry());
		assertTrue("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
		assertEquals("Wrong header", "something", d2.getHeader("SomeHeader"));
		assertNull("Header should be missing", d2.getHeader("AnotherHeader"));
	}

	/**
	 * Modify a simple project - change class path, add activator and plugin.xml.
	 *
	 * @throws CoreException
	 */
	public void testModifyBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IPath src = new Path("srcA");
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("a.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, null);
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, null);
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, new String[]{"x.y.z"});
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false, new String[]{"d.e.f", "g.h.i"});
		description.setPackageExports(new IPackageExportDescription[]{ex0, ex1, ex2, ex3});
		description.apply(null);

		// modify the project
		IBundleProjectDescription modify = service.getDescription(project);
		IPath srcB = new Path("srcB");
		IBundleClasspathEntry specB = service.newBundleClasspathEntry(srcB, null, new Path("b.jar"));
		modify.setBundleClasspath(new IBundleClasspathEntry[] {specB});
		IPackageExportDescription ex4 = service.newPackageExport("x.y.z.interal", null, false, new String[]{"zz.top"});
		modify.setPackageExports(new IPackageExportDescription[]{ex0, ex2, ex4, ex3}); // remove, add, re-order
		modify.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		modify.setActivator("org.eclipse.foo.Activator");
		modify.setActivationPolicy(Constants.ACTIVATION_LAZY);
		modify.apply(null);

		// verify attributes
		IBundleProjectDescription d2 = service.getDescription(project);

		assertEquals("Wrong activator", "org.eclipse.foo.Activator", d2.getActivator());
		assertEquals("Wrong activation policy", Constants.ACTIVATION_LAZY, d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes entry", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", specB, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull("Missing exports", exports);
		assertEquals("Wrong number of exports", 4, exports.length);
		assertEquals("Wrong export", ex0, exports[0]);
		assertEquals("Wrong export", ex2, exports[1]);
		assertEquals("Wrong export", ex3, exports[2]); // the manifest ends up sorted, so order changes
		assertEquals("Wrong export", ex4, exports[3]);
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Modify a simple project to add/remove/clear some entries.
	 * See bug 380444 where previous settings weren't being cleared
	 *
	 * @throws CoreException
	 */
	public void testModifyRequiredBundles() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();

		IRequiredBundleDescription requireDesc = service.newRequiredBundle("requiredBundleOne", null, false, false);
		IRequiredBundleDescription requireDesc2 = service.newRequiredBundle("requiredBundleTwo", new VersionRange("[1.0.0,2.0.0)"), false, false);
		IRequiredBundleDescription requireDesc3 = service.newRequiredBundle("requiredBundleThree", null, true, false);
		IRequiredBundleDescription requireDesc4 = service.newRequiredBundle("requiredBundleFour", null, false, true);
		description.setRequiredBundles(new IRequiredBundleDescription[]{requireDesc, requireDesc2, requireDesc3, requireDesc4});

		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, null);
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, null);
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, new String[]{"x.y.z"});
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false, new String[]{"d.e.f", "g.h.i"});
		description.setPackageExports(new IPackageExportDescription[]{ex0, ex1, ex2, ex3});

		IPackageImportDescription importDesc = service.newPackageImport("importPkgOne", null, false);
		IPackageImportDescription importDesc2 = service.newPackageImport("importPkgTwo", new VersionRange("[1.0.0,2.0.0)"), false);
		IPackageImportDescription importDesc3 = service.newPackageImport("importPkgThree", null, true);
		IPackageImportDescription importDesc4 = service.newPackageImport("importPkgFour", null, false);
		description.setPackageImports(new IPackageImportDescription[]{importDesc, importDesc2, importDesc3, importDesc4});

		description.apply(null);

		// verify attributes
		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("Wrong number of required bundles", 4, d2.getRequiredBundles().length);
		assertEquals("Wrong number of package exports", 4, d2.getPackageExports().length);
		assertEquals("Wrong number of package imports", 4, d2.getPackageImports().length);

		// add entries
		IRequiredBundleDescription requireDesc5 = service.newRequiredBundle("requiredBundleFive", null, false, false);
		IRequiredBundleDescription requireDesc6 = service.newRequiredBundle("requiredBundleSix", null, false, false);
		description.setRequiredBundles(new IRequiredBundleDescription[]{requireDesc, requireDesc2, requireDesc3, requireDesc4, requireDesc5, requireDesc6});

		IPackageExportDescription ex4 = service.newPackageExport("a.b.c.interal.x2", null, false, new String[]{"x.y.z"});
		IPackageExportDescription ex5 = service.newPackageExport("a.b.c.interal.y2", new Version("1.2.3"), false, new String[]{"d.e.f", "g.h.i"});
		description.setPackageExports(new IPackageExportDescription[]{ex0, ex1, ex2, ex3, ex4, ex5});

		IPackageImportDescription importDesc5 = service.newPackageImport("importPkgFive", null, true);
		IPackageImportDescription importDesc6 = service.newPackageImport("importPkgSix", null, false);
		description.setPackageImports(new IPackageImportDescription[]{importDesc, importDesc2, importDesc3, importDesc4, importDesc5, importDesc6});

		description.apply(null);

		// verify attributes
		IBundleProjectDescription d3 = service.getDescription(project);
		assertEquals("Wrong number of required bundles after additions", 6, d3.getRequiredBundles().length);
		assertEquals("Wrong number of package exports after addtions", 6, d3.getPackageExports().length);
		assertEquals("Wrong number of package imports after additions", 6, d3.getPackageImports().length);

		// remove most entries
		description.setRequiredBundles(new IRequiredBundleDescription[]{requireDesc2, requireDesc5});
		description.setPackageExports(new IPackageExportDescription[]{ex1, ex4});
		description.setPackageImports(new IPackageImportDescription[]{importDesc2, importDesc5});
		description.apply(null);

		// verify attributes
		IBundleProjectDescription d4 = service.getDescription(project);
		assertEquals("Wrong number of required bundles after removals", 2, d4.getRequiredBundles().length);
		assertEquals("Wrong number of package exports after removals", 2, d4.getPackageExports().length);
		assertEquals("Wrong number of package imports after removals", 2, d4.getPackageImports().length);

		// clear entries
		description.setRequiredBundles(null);
		description.setPackageExports(null);
		description.setPackageImports(null);
		description.apply(null);

		// verify attributes
		IBundleProjectDescription d5 = service.getDescription(project);
		assertNull("Wrong number of required bundles after removals", d5.getRequiredBundles());
		assertNull("Wrong number of package exports after removals", d5.getPackageExports());
		assertNull("Wrong number of package imports after removals", d5.getPackageImports());
	}

	/**
	 * Convert a fragment into a bundle
	 *
	 * @throws CoreException
	 */
	public void testFragToBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IHostDescription host = service.newHost("some.host", null);
		description.setHeader("HeaderOne", "one"); // arbitrary header
		description.setHost(host);
		description.apply(null);

		//modify to a bundle and remove a header
		IBundleProjectDescription modify = service.getDescription(project);
		assertEquals("Wrong header value", "one", modify.getHeader("HeaderOne"));
		modify.setHeader("HeaderOne", null);
		modify.setHost(null);
		modify.apply(null);

		// validate
		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Header should be removed", d2.getHeader("HeaderOne"));
		assertNull("Should be no activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
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
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Tests creating a project that simply wraps jars into a bundle.
	 *
	 * @throws CoreException
	 */
	public void testJarsAsBundle() throws CoreException, IOException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry one = service.newBundleClasspathEntry(null, null, new Path("one.jar"));
		IBundleClasspathEntry two = service.newBundleClasspathEntry(null, null, new Path("lib/two.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{one, two});
		IPackageExportDescription exp1 = service.newPackageExport("org.eclipse.one", new Version("1.0.0"), true, null);
		IPackageExportDescription exp2 = service.newPackageExport("org.eclipse.two", new Version("1.0.0"), true, null);
		description.setPackageExports(new IPackageExportDescription[]{exp1, exp2});
		description.setBundleVersion(new Version("1.0.0"));
		description.setExecutionEnvironments(new String[]{"J2SE-1.5"});
		description.apply(null);
		// create bogus jar files
		createBogusJar(project.getFile("one.jar"));
		createBogusJar(project.getFile(new Path("lib/two.jar")));

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 2, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", one, classpath[0]);
		assertEquals("Wrong Bundle-Classpath entry", two, classpath[1]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", new Version("1.0.0"), d2.getBundleVersion());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull("Wrong execution environments", ees);
		assertEquals("Wrong number of execution environments", 1, ees.length);
		assertEquals("Wrong execution environments", "J2SE-1.5", ees[0]);
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull("Wrong exports", exports);
		assertEquals("Wrong number of exports", 2, exports.length);
		assertEquals("Wrong exports", exp1, exports[0]);
		assertEquals("Wrong exports", exp2, exports[1]);
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Creates a file with some content at the given location.
	 *
	 * @param file
	 * @throws CoreException
	 */
	protected void createBogusJar(IFile file) throws CoreException, IOException {
		IContainer parent = file.getParent();
		while (parent instanceof IFolder) {
			if (!parent.exists()) {
				((IFolder)parent).create(false, true, null);
			}
			parent = parent.getParent();
		}
		URL zipURL = PDETestsPlugin.getBundleContext().getBundle().getEntry("tests/A.jar");
		File ioFile = new File(FileLocator.toFileURL(zipURL).getFile());
		try (FileInputStream stream = new FileInputStream(ioFile)) {
			file.create(stream, false, null);
		}
	}

	/**
	 * Tests creating a project that simply wraps jars into a bundle.
	 *
	 * @throws CoreException
	 */
	public void testClassFolders() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry one = service.newBundleClasspathEntry(null, new Path("bin1"), new Path("one.jar"));
		IBundleClasspathEntry two = service.newBundleClasspathEntry(null, new Path("bin2"), new Path("two.jar"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{one, two});
		IPackageExportDescription exp1 = service.newPackageExport("org.eclipse.one", new Version("1.0.0"), true, null);
		IPackageExportDescription exp2 = service.newPackageExport("org.eclipse.two", new Version("1.0.0"), true, null);
		description.setPackageExports(new IPackageExportDescription[]{exp1, exp2});
		description.setBundleVersion(new Version("1.0.0"));
		description.setExecutionEnvironments(new String[]{"J2SE-1.5"});
		description.apply(null);
		// create folders
		project.getFolder("bin1").create(false, true, null);
		project.getFolder("bin2").create(false, true, null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 2, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", one, classpath[0]);
		assertEquals("Wrong Bundle-Classpath entry", two, classpath[1]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", new Version("1.0.0"), d2.getBundleVersion());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull("Wrong execution environments", ees);
		assertEquals("Wrong number of execution environments", 1, ees.length);
		assertEquals("Wrong execution environments", "J2SE-1.5", ees[0]);
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull("Wrong exports", exports);
		assertEquals("Wrong number of exports", 2, exports.length);
		assertEquals("Wrong exports", exp1, exports[0]);
		assertEquals("Wrong exports", exp2, exports[1]);
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());
	}

	/**
	 * Test custom export wizard and launch shortcuts.
	 *
	 * @throws CoreException
	 */
	public void testExportWizardLaunchShortcuts() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setLaunchShortcuts(new String[]{"org.eclipse.jdt.debug.ui.javaAppletShortcut"});
		description.setExportWizardId("org.eclipse.debug.internal.ui.importexport.breakpoints.WizardExportBreakpoints");
		description.apply(null);

		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
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
		assertEquals("Wrong export wizard", "org.eclipse.debug.internal.ui.importexport.breakpoints.WizardExportBreakpoints", d2.getExportWizardId());
		String[] ids = d2.getLaunchShortcuts();
		assertNotNull("Wrong launch shortctus", ids);
		assertEquals("Wrong number of shortcuts", 1, ids.length);
		assertEquals("org.eclipse.jdt.debug.ui.javaAppletShortcut", ids[0]);
	}

	/**
	 * Targeting 3.1, should get result it Eclipse-AutoStart: true
	 *
	 * @throws CoreException
	 */
	public void testLazyAutostart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		description.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		description.setActivator("org.eclipse.foo.Activator");
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_1);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.setExecutionEnvironments(new String[]{"J2SE-1.4"});
		IRequiredBundleDescription rb1 = service.newRequiredBundle(
				"org.eclipse.core.resources",
				new VersionRange(new Version(3,5,0), true, new Version(4,0,0), false),
				true, false);
		IRequiredBundleDescription rb2 = service.newRequiredBundle("org.eclipse.core.variables", null, false, false);
		description.setRequiredBundles(new IRequiredBundleDescription[]{rb1, rb2});
		IPackageImportDescription pi1 = service.newPackageImport("com.ibm.icu.text", null, false);
		description.setPackageImports(new IPackageImportDescription[]{pi1});
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("Wrong activator", "org.eclipse.foo.Activator", d2.getActivator());
		assertEquals("Wrong activation policy", Constants.ACTIVATION_LAZY, d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull("Wrong execution environments", ees);
		assertEquals("Wrong number of execution environments", 1, ees.length);
		assertEquals("Wrong execution environment", "J2SE-1.4", ees[0]);
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull("Wrong exports", d2.getPackageExports());
		assertNotNull("Wrong imports", imports);
		assertEquals("Wrong number of package imports", 1, imports.length);
		assertEquals("Wrong package import", pi1, imports[0]);
		assertEquals("Wrong project", project, d2.getProject());
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNotNull("Wrong required bundles", bundles);
		assertEquals("Wrong number of required bundles", 2, bundles.length);
		assertEquals("Wrong required bundle", rb1, bundles[0]);
		assertEquals("Wrong required bundle", rb2, bundles[1]);
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertTrue("Wrong extension registry support", d2.isExtensionRegistry());
		assertTrue("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());

		// ensure proper header was generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Missing plugin model", model);
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_AUTOSTART);
		assertNotNull("Missing header", header);
	}

	/**
	 * Returns a structured header from a bundle model
	 *
	 * @param bundle the bundle
	 * @param header header name/key
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
	 *
	 * @throws CoreException
	 */
	public void testEagerAutostart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		description.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		description.setActivator("org.eclipse.foo.Activator");
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_1);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("Wrong activator", "org.eclipse.foo.Activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull("Wrong exports", d2.getPackageExports());
		assertNull("Wrong imports", imports);
		assertEquals("Wrong project", project, d2.getProject());
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNull("Wrong required bundles", bundles);
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertTrue("Wrong extension registry support", d2.isExtensionRegistry());
		assertTrue("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());

		// ensure header was *not* generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Missing plugin model", model);
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_AUTOSTART);
		assertNull("Header should not be present", header);
	}

	/**
	 * Targeting 3.2, lazy bundle should have Eclipse-LazyStart: header
	 *
	 * @throws CoreException
	 */
	public void testLazyEclipseLazyStart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		description.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		description.setActivator("org.eclipse.foo.Activator");
		description.setActivationPolicy(Constants.ACTIVATION_LAZY);
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_2);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("Wrong activator", "org.eclipse.foo.Activator", d2.getActivator());
		assertEquals("Wrong activation policy", Constants.ACTIVATION_LAZY, d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull("Wrong exports", d2.getPackageExports());
		assertNull("Wrong imports", imports);
		assertEquals("Wrong project", project, d2.getProject());
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNull("Wrong required bundles", bundles);
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertTrue("Wrong extension registry support", d2.isExtensionRegistry());
		assertTrue("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());

		// ensure header was generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Missing plugin model", model);
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_LAZYSTART);
		assertNotNull("Header should be present", header);
	}

	/**
	 * Targeting 3.2, eager bundle should not have Eclipse-LazyStart: header
	 *
	 * @throws CoreException
	 */
	public void testEagerEclipseLazyStart() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		description.setSingleton(true);
		IPath src = new Path("src");
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] {spec});
		description.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		description.setActivator("org.eclipse.foo.Activator");
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_2);
		description.setEquinox(true);
		description.setExtensionRegistry(true);
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		assertEquals("Wrong activator", "org.eclipse.foo.Activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Bundle-Classpath should be specified", classpath);
		assertEquals("Wrong number of bundle classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", classpath[0], spec);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		assertNull("Wrong execution environments", d2.getExecutionEnvironments());
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		IPackageImportDescription[] imports = d2.getPackageImports();
		assertNull("Wrong exports", d2.getPackageExports());
		assertNull("Wrong imports", imports);
		assertEquals("Wrong project", project, d2.getProject());
		IRequiredBundleDescription[] bundles = d2.getRequiredBundles();
		assertNull("Wrong required bundles", bundles);
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertTrue("Wrong extension registry support", d2.isExtensionRegistry());
		assertTrue("Wrong Equinox headers", d2.isEquinox());
		assertTrue("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());

		// ensure header was generated
		waitForBuild();
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Missing plugin model", model);
		IPluginBase base = model.getPluginBase();
		IBundle bundle = ((BundlePluginBase) base).getBundle();
		IManifestHeader header = createHeader(bundle, ICoreConstants.ECLIPSE_LAZYSTART);
		assertNull("Header should not be present", header);
	}

	/**
	 * Returns the given input stream's contents as a character array.
	 * If a length is specified (i.e. if length != -1), this represents the number of bytes in the stream.
	 * Note the specified stream is not closed in this method
	 * @param stream the stream to get convert to the char array
	 * @return the given input stream's contents as a character array.
	 * @throws IOException if a problem occurred reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream) throws IOException {
		Charset charset = StandardCharsets.UTF_8;
		CharsetDecoder charsetDecoder = charset.newDecoder();
		charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		byte[] contents = getInputStreamAsByteArray(stream, -1);
		ByteBuffer byteBuffer = ByteBuffer.allocate(contents.length);
		byteBuffer.put(contents);
		byteBuffer.flip();
		return charsetDecoder.decode(byteBuffer).array();
	}

	/**
	 * Returns the given input stream as a byte array
	 * @param stream the stream to get as a byte array
	 * @param length the length to read from the stream or -1 for unknown
	 * @return the given input stream as a byte array
	 * @throws IOException
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				// read at least 8K
				int amountRequested = Math.max(stream.available(), 8192);
				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(contents,
							0,
							contents = new byte[contentsLength + amountRequested],
							0,
							contentsLength);
				}
				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);
				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);
			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case length is the actual
				// read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}
		return contents;
	}

	/**
	 * Tests that package import/export headers don't get flattened when doing an unrelated edit.
	 *
	 * @throws CoreException
	 * @throws IOException
	 */
	public void testHeaderFormatting() throws CoreException, IOException {
		IBundleProjectDescription description = newProject();
		IPackageImportDescription imp1 = getBundleProjectService().newPackageImport("org.eclipse.osgi", null, false);
		IPackageImportDescription imp2 = getBundleProjectService().newPackageImport("org.eclipse.core.runtime", null, false);
		IPackageImportDescription imp3 = getBundleProjectService().newPackageImport("org.eclipse.core.resources", null, false);
		description.setPackageImports(new IPackageImportDescription[]{imp1, imp2, imp3});
		IPackageExportDescription ex1 = getBundleProjectService().newPackageExport("a.b.c", null, true, null);
		IPackageExportDescription ex2 = getBundleProjectService().newPackageExport("a.b.c.d", null, true, null);
		IPackageExportDescription ex3 = getBundleProjectService().newPackageExport("a.b.c.e", null, true, null);
		description.setPackageExports(new IPackageExportDescription[]{ex1, ex2, ex3});
		IProject project = description.getProject();
		description.apply(null);

		// should be 12 lines
		IFile manifest = PDEProject.getManifest(project);
		char[] chars = getInputStreamAsCharArray(manifest.getContents());
		Document document = new Document(new String(chars));
		int lines = document.getNumberOfLines();
		assertEquals("Wrong number of lines", 12, lines);

		// modify version attribute
		IBundleProjectDescription d2 = getBundleProjectService().getDescription(project);
		d2.setBundleVersion(new Version("2.0.0"));
		d2.apply(null);

		// should be 12 lines
		manifest = PDEProject.getManifest(project);
		chars = getInputStreamAsCharArray(manifest.getContents());
		document = new Document(new String(chars));
		lines = document.getNumberOfLines();
		assertEquals("Wrong number of lines", 12, lines);
	}

	/**
	 * Changes a non-plug-in project into a a plug-in.
	 *
	 * @throws CoreException
	 */
	public void testNonBundleToBundle() throws CoreException {
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject("test.non.bundle.to.bundle");
		assertFalse("Project should not exist", proj.exists());
		proj.create(null);
		proj.open(null);
		IProjectDescription pd = proj.getDescription();
		pd.setNatureIds(new String[]{JavaCore.NATURE_ID});
		proj.setDescription(pd, null);

		IBundleProjectDescription description = getBundleProjectService().getDescription(proj);
		assertTrue("Missing Java Nature", description.hasNature(JavaCore.NATURE_ID));
		description.setSymbolicName("test.non.bundle.to.bundle");
		description.setNatureIds(new String[]{IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID});
		description.apply(null);

		// validate
		IBundleProjectDescription d2 = getBundleProjectService().getDescription(proj);
		assertEquals("Wrong symbolic name", proj.getName(), d2.getSymbolicName());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertTrue("Nature should be present", d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE));
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", DEFAULT_BUNDLE_CLASSPATH_ENTRY, classpath[0]);

	}

	/**
	 * Convert an existing Java project into a bundle project. Ensure it's build path
	 * doesn't get toasted in the process.
	 *
	 * @throws CoreException
	 */
	public void testJavaToBundle() throws CoreException {
		// create a Java project
		String name = getName().toLowerCase().substring(4);
		name = "test." + name;
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		assertFalse("Project should not exist", proj.exists());
		proj.create(null);
		proj.open(null);
		IProjectDescription pd = proj.getDescription();
		pd.setNatureIds(new String[]{JavaCore.NATURE_ID});
		proj.setDescription(pd, null);
		IFolder src = proj.getFolder("someSrc");
		src.create(false, true, null);
		IFolder output = proj.getFolder("someBin");
		output.create(false, true, null);
		IJavaProject javaProject = JavaCore.create(proj);
		javaProject.setOutputLocation(output.getFullPath(), null);
		IClasspathEntry entry1 = JavaCore.newSourceEntry(src.getFullPath());
		IClasspathEntry entry2 = JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4")));
		IClasspathEntry entry3 = JavaCore.newContainerEntry(ClasspathContainerInitializer.PATH);
		javaProject.setRawClasspath(new IClasspathEntry[]{entry1, entry2, entry3}, null);

		// convert to a bundle
		IBundleProjectDescription description = getBundleProjectService().getDescription(proj);
		assertTrue("Missing Java Nature", description.hasNature(JavaCore.NATURE_ID));
		description.setSymbolicName(proj.getName());
		description.setNatureIds(new String[]{IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID});
		IBundleClasspathEntry entry = getBundleProjectService().newBundleClasspathEntry(src.getProjectRelativePath(), null, null);
		description.setBundleClasspath(new IBundleClasspathEntry[]{entry});
		description.apply(null);

		// validate
		IBundleProjectDescription d2 = getBundleProjectService().getDescription(proj);
		assertEquals("Wrong symbolic name", proj.getName(), d2.getSymbolicName());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertTrue("Nature should be present", d2.hasNature(IBundleProjectDescription.PLUGIN_NATURE));
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		// execution environment should be that on the Java build path
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull("Missing EEs", ees);
		assertEquals("Wrong number of EEs", 1, ees.length);
		assertEquals("Wrong EE", "J2SE-1.4", ees[0]);
		// version
		assertEquals("Wrong version", "1.0.0.qualifier", d2.getBundleVersion().toString());

		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", getBundleProjectService().newBundleClasspathEntry(src.getProjectRelativePath(), null, new Path(".")), classpath[0]);

		// raw class path should still be intact
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		assertEquals("Wrong number of entries", 4, rawClasspath.length);
		assertEquals("Wrong entry", entry1, rawClasspath[0]);
		assertEquals("Wrong entry", entry2, rawClasspath[1]);
		assertEquals("Wrong entry", entry3, rawClasspath[2]);
		assertEquals("Missing Required Plug-ins Container", ClasspathComputer.createContainerEntry(), rawClasspath[3]);
	}

	/**
	 * Tests creating a project that has a nested class file folders instead of a jar
	 *
	 * @throws CoreException
	 */
	public void testClassFoldersNoJars() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IBundleClasspathEntry one = service.newBundleClasspathEntry(new Path("src"), new Path("WebContent/WEB-INF/classes"), new Path("WebContent/WEB-INF/classes"));
		description.setBundleClasspath(new IBundleClasspathEntry[]{one});
		IPackageExportDescription exp1 = service.newPackageExport("org.eclipse.one", new Version("1.0.0"), true, null);
		IPackageExportDescription exp2 = service.newPackageExport("org.eclipse.two", new Version("1.0.0"), true, null);
		description.setPackageExports(new IPackageExportDescription[]{exp1, exp2});
		description.setBundleVersion(new Version("1.0.0"));
		description.setExecutionEnvironments(new String[]{"J2SE-1.5"});
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);

		assertNull("Should be no activator", d2.getActivator());
		assertNull("Wrong activation policy", d2.getActivationPolicy());
		IPath[] binIncludes = d2.getBinIncludes();
		assertNull("Wrong number of entries on bin.includes", binIncludes);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", one, classpath[0]);
		assertEquals("Wrong Bundle-Name", project.getName(), d2.getBundleName());
		assertNull("Wrong Bundle-Vendor", d2.getBundleVendor());
		assertEquals("Wrong version", new Version("1.0.0"), d2.getBundleVersion());
		assertEquals("Wrong default output folder", new Path("bin"), d2.getDefaultOutputFolder());
		String[] ees = d2.getExecutionEnvironments();
		assertNotNull("Wrong execution environments", ees);
		assertEquals("Wrong number of execution environments", 1, ees.length);
		assertEquals("Wrong execution environments", "J2SE-1.5", ees[0]);
		assertNull("Wrong host", d2.getHost());
		assertNull("Wrong localization", d2.getLocalization());
		assertNull("Wrong project location URI", d2.getLocationURI());
		String[] natureIds = d2.getNatureIds();
		assertEquals("Wrong number of natures", 2, natureIds.length);
		assertEquals("Wrong nature", IBundleProjectDescription.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		IPackageExportDescription[] exports = d2.getPackageExports();
		assertNotNull("Wrong exports", exports);
		assertEquals("Wrong number of exports", 2, exports.length);
		assertEquals("Wrong exports", exp1, exports[0]);
		assertEquals("Wrong exports", exp2, exports[1]);
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong extension registry support", d2.isExtensionRegistry());
		assertFalse("Wrong Equinox headers", d2.isEquinox());
		assertFalse("Wrong singleton", d2.isSingleton());
		assertNull("Wrong export wizard", d2.getExportWizardId());
		assertNull("Wrong launch shortctus", d2.getLaunchShortcuts());

		// should be no warnings on build.properties
		IFile file = PDEProject.getBuildProperties(project);
		IMarker[] markers = file.findMarkers(PDEMarkerFactory.MARKER_ID, true, 0);
		assertEquals("Should be no errors", 0, markers.length);
	}


	/**
	 * Tests that adding package exports incrementally works
	 *
	 * @throws CoreException
	 */
	public void testExportUpdateSequence() throws CoreException {
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IPackageExportDescription e1 = service.newPackageExport("a.b.c", null, true, null);
		description.setPackageExports(new IPackageExportDescription[]{e1});
		description.apply(null);

		IBundleProjectDescription d2 = service.getDescription(project);
		IPackageExportDescription e2 = service.newPackageExport("a.b.c.internal", null, false, null);
		d2.setPackageExports(new IPackageExportDescription[]{e1, e2});
		d2.apply(null);

		IBundleProjectDescription d3 = service.getDescription(project);
		IPackageExportDescription[] exports = d3.getPackageExports();
		assertNotNull("Wrong exports", exports);
		assertEquals("Wrong number of exports", 2, exports.length);
		assertEquals("Wrong package export", e1, exports[0]);
		assertEquals("Wrong package export", e2, exports[1]);
	}
}
