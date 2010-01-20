/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import org.eclipse.pde.core.project.IBundleProjectService;

import org.eclipse.pde.internal.core.PDECore;

import org.eclipse.pde.internal.core.ICoreConstants;

import java.io.*;
import java.net.URL;
import junit.framework.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.project.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
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
		return (IBundleProjectService) PDECore.getDefault().acquireService(IBundleProjectService.class.getName());
	}

	public static Test suite() {
		return new TestSuite(ProjectCreationTests.class);
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		IBundleClasspathEntry e1 = service.newBundleClasspathEntry(new Path("frag"), new Path("bin"), new Path("frag.jar"));
		description.setBundleClassath(new IBundleClasspathEntry[]{e1});
		description.apply(null);
		
		IBundleProjectDescription d2 = service.getDescription(project);
		
		assertNull("Should be no activator", d2.getActivator());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		description.setBundleClassath(new IBundleClasspathEntry[]{e1, e2});
		description.setBundleVersion(new Version("1.2.3"));
		description.apply(null);
		
		IBundleProjectDescription d2 = service.getDescription(project);
		
		assertNull("Should be no activator", d2.getActivator());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		description.setBundleClassath(new IBundleClasspathEntry[]{e1, e2});
		description.setBundleVersion(new Version("1.2.3"));
		description.apply(null);
		
		IBundleProjectDescription d2 = service.getDescription(project);
		
		assertNull("Should be no activator", d2.getActivator());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertTrue("Wrong singleton", d2.isSingleton());
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
		description.setBundleClassath(new IBundleClasspathEntry[] {spec});
		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, null);
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, null);
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, new String[]{"x.y.z"});
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false, new String[]{"d.e.f", "g.h.i"});
		description.setPackageExports(new IPackageExportDescription[]{ex0, ex1, ex2, ex3});
		description.apply(null);
		
		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull("Should be no activator", d2.getActivator());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
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
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertTrue("Wrong singleton", d2.isSingleton());
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
		description.setBundleClassath(new IBundleClasspathEntry[] {spec});
		description.apply(null);
		
		// modify
		IBundleProjectDescription modify = service.getDescription(project);
		IHostDescription host = service.newHost("host." + project.getName(), new VersionRange("[1.0.0,2.0.0)"));
		modify.setHost(host);
		modify.apply(null);
		
		// validate
		IBundleProjectDescription d2 = service.getDescription(project);
		assertNull("Should be no activator", d2.getActivator());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertTrue("Wrong singleton", d2.isSingleton());
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
		description.setBundleClassath(new IBundleClasspathEntry[] {spec});
		description.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		description.setActivator("org.eclipse.foo.Activator");
		description.setEqunioxHeaders(true);
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
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
		assertTrue("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertTrue("Wrong singleton", d2.isSingleton());
	}		
	
	/**
	 * Modify a simple project - change classpath, add activator and plugin.xml.
	 * 
	 * @throws CoreException
	 */
	public void testModifyBundle() throws CoreException {
		IBundleProjectDescription description = newProject();
		IProject project = description.getProject();
		IBundleProjectService service = getBundleProjectService();
		IPackageExportDescription ex0 = service.newPackageExport("a.b.c", new Version("2.0.0"), true, null);
		IPackageExportDescription ex1 = service.newPackageExport("a.b.c.interal", null, false, null);
		IPackageExportDescription ex2 = service.newPackageExport("a.b.c.interal.x", null, false, new String[]{"x.y.z"});
		IPackageExportDescription ex3 = service.newPackageExport("a.b.c.interal.y", new Version("1.2.3"), false, new String[]{"d.e.f", "g.h.i"});
		description.setPackageExports(new IPackageExportDescription[]{ex0, ex1, ex2, ex3});
		description.apply(null);
		
		// modify the project
		IBundleProjectDescription modify = service.getDescription(project);
		IPath src = new Path("src");
		IBundleClasspathEntry spec = service.newBundleClasspathEntry(src, null, new Path("a.jar"));
		modify.setBundleClassath(new IBundleClasspathEntry[] {spec});
		IPackageExportDescription ex4 = service.newPackageExport("x.y.z.interal", null, false, new String[]{"zz.top"});
		modify.setPackageExports(new IPackageExportDescription[]{ex0, ex2, ex4, ex3}); // remove, add, re-order
		modify.setBinIncludes(new IPath[]{new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)});
		modify.setActivator("org.eclipse.foo.Activator");
		modify.apply(null);
		
		// verify attributes
		IBundleProjectDescription d2 = service.getDescription(project);
		
		assertEquals("Should be no activator", "org.eclipse.foo.Activator", d2.getActivator());
		IPath[] binIncludes = d2.getBinIncludes();
		assertEquals("Wrong number of entries on bin.includes", 1, binIncludes.length);
		assertEquals("Wrong bin.includes entry", new Path(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), binIncludes[0]);
		IBundleClasspathEntry[] classpath = d2.getBundleClasspath();
		assertNotNull("Wrong Bundle-Classpath", classpath);
		assertEquals("Wrong number of Bundle-Classpath entries", 1, classpath.length);
		assertEquals("Wrong Bundle-Classpath entry", spec, classpath[0]);
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
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
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		description.setHost(host);
		description.apply(null);
		
		//modify to a bundle
		IBundleProjectDescription modify = service.getDescription(project);
		modify.setHost(null);
		modify.apply(null);
		
		// validate
		IBundleProjectDescription d2 = service.getDescription(project);
		
		assertNull("Should be no activator", d2.getActivator());
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
		assertEquals("Wrong nature", JavaCore.NATURE_ID, natureIds[1]);
		assertNull("Wrong imports", d2.getPackageImports());
		assertNull("Wrong exports", d2.getPackageExports());
		assertEquals("Wrong project", project, d2.getProject());
		assertNull("Wrong required bundles", d2.getRequiredBundles());
		assertNull("Wrong target version", d2.getTargetVersion());
		assertEquals("Wrong symbolic name", project.getName(), d2.getSymbolicName());
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		description.setBundleClassath(new IBundleClasspathEntry[]{one, two});
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
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
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
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
		URL zipURL = MacroPlugin.getBundleContext().getBundle().getEntry("tests/A.jar");
		File ioFile = new File(FileLocator.toFileURL(zipURL).getFile());
		FileInputStream stream = new FileInputStream(ioFile);
		file.create(stream, false, null);
		stream.close();
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
		description.setBundleClassath(new IBundleClasspathEntry[]{one, two});
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
		assertEquals("Wrong nature", PDE.PLUGIN_NATURE, natureIds[0]);
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
		assertFalse("Wrong Equinox headers", d2.isEquinoxHeaders());
		assertFalse("Wrong singleton", d2.isSingleton());
	}	
}
