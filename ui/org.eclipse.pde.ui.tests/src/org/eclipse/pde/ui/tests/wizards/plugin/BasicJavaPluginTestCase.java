/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.wizards.plugin;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.ui.tests.*;

import junit.framework.*;

public class BasicJavaPluginTestCase extends NewProjectTest {

	private static final String PROJECT_NAME = "com.example.xyz";

	public static Test suite() {
		return new TestSuite(BasicJavaPluginTestCase.class);
	}
	
	public void testMinimalJavaPlugin() {
		try {
			playScript(Catalog.BASIC_JAVA_PLUGIN_1);
			verifyProject(true);
			verifyPluginModel(null, ".", true);
			verifyBuildProperties(true, ".", "src", "bin");
		} catch (CoreException e) {
			fail("testMinimalJavaPlugin:" + e);
		}
	}
	
	public void testMinimalJavaPluginWithoutManifest() {
		try {
			playScript(Catalog.BASIC_JAVA_PLUGIN_2);
			verifyProject(false);
			verifyPluginModel(null, "xyz.jar", false);
			verifyBuildProperties(false, "xyz.jar", "src", "bin");
		} catch (CoreException e) {
			fail("testMinimalJavaPluginWithManifest:" + e);
		}
	}
	
	public void testMultiSegmentSourceFolder() {
		try {
			playScript(Catalog.BASIC_JAVA_PLUGIN_3);
			verifyProject(true, null, "src/abc", "bin");
			verifyPluginModel(null, ".", true);
			verifyBuildProperties(true, ".", "src/abc", "bin");
		} catch (CoreException e) {
			fail("testMultiSegmentSourceFolder:" + e);
		}
	}
	
	public void xtestMultiSegmentOutputFolder() {
		try {
			playScript(Catalog.BASIC_JAVA_PLUGIN_4);
			verifyProject(true, null, "src", "bin/abc");
			verifyPluginModel(null, ".", true);
			verifyBuildProperties(true, ".", "src", "bin/abc");
		} catch (CoreException e) {
			fail("testMultiSegmentOutputFolder:" + e);
		}
	}
	
	public void testUIPlugin() {
		try {
			playScript(Catalog.BASIC_JAVA_PLUGIN_6);
			verifyProject(true, "com.example.xyz.XyzPlugin", "src", "bin");
			verifyPluginModel("com.example.xyz.XyzPlugin", ".", true);
			verifyBuildProperties(true, ".", "src", "bin");
			verifyType(true, "com.example.xyz.XyzPlugin");
		} catch (CoreException e) {
			fail("testUIPlugin:" + e);
		}
	}
	
	public void testNonUIPlugin() {
		try {
			playScript(Catalog.BASIC_JAVA_PLUGIN_7);
			verifyProject(true, "com.example.xyz.XyzPlugin", "src", "bin");
			verifyPluginModel("com.example.xyz.XyzPlugin", ".", true);
			verifyBuildProperties(true, ".", "src", "bin");
			verifyType(false, "com.example.xyz.XyzPlugin");
		} catch (CoreException e) {
			fail("testNonUIPlugin:" + e);
		}
	}
	
	public void verifyType(boolean ui, String className) throws CoreException {
		if (className != null) {
			IJavaProject jProject = JavaCore.create(getProject());
			IType type = jProject.findType(className);
			assertNotNull(type);
			assertTrue(type.isClass());
			assertFalse(type.isBinary());
			if (ui)
				assertEquals("AbstractUIPlugin", type.getSuperclassName());
			else
				assertEquals("Plugin", type.getSuperclassName());
		}

	}

	protected String getProjectName() {
		return PROJECT_NAME;
	}
	
	private void verifyProject(boolean isBundle) throws CoreException {
		verifyProject(isBundle, null, "src", "bin");
	}
	
	private void verifyProject(boolean isBundle, String className, String srcFolder, String outputFolder) throws CoreException {
		verifyProjectExistence();
		verifyNatures();
		verifyManifestFiles(isBundle);
		verifyClasspath(className, srcFolder, outputFolder);
	}
	
	private void verifyNatures() {
		assertTrue("Project does not have a PDE nature.", hasNature(PDE.PLUGIN_NATURE));
		assertTrue("Simple Project has a Java nature.", hasNature(JavaCore.NATURE_ID));
	}
	
	private void verifyManifestFiles(boolean isBundle) {
		if (isBundle) {
			assertTrue(getProject().getFile("META-INF/MANIFEST.MF").exists());
			assertFalse(getProject().getFile("plugin.xml").exists());
		} else {
			assertTrue(getProject().getFile("plugin.xml").exists());
			assertFalse(getProject().getFile("META-INF/MANIFEST.MF").exists());
		}
	}
	
	private void verifyClasspath(String className, String srcFolder, String outputFolder) throws CoreException {
		IJavaProject jProject = JavaCore.create(getProject());
		IPath expected = new Path(getProjectName()).append(outputFolder).makeAbsolute();
		assertEquals(expected, jProject.getOutputLocation());
		
		IClasspathEntry[] entries = jProject.getRawClasspath();
		assertEquals(3, entries.length);
		
		// verify source folder
		IClasspathEntry entry = entries[0];
		assertEquals(IClasspathEntry.CPE_SOURCE, entry.getEntryKind());
		assertEquals(new Path(getProjectName()).append(srcFolder).makeAbsolute(), entry.getPath());
		
		// verify PDE container 
		entry = entries[1];
		assertEquals(IClasspathEntry.CPE_CONTAINER, entry.getEntryKind());
		assertEquals(new Path(PDECore.CLASSPATH_CONTAINER_ID), entry.getPath());
		IPackageFragmentRoot[] roots = jProject.findPackageFragmentRoots(entry);
		assertEquals(className == null, roots.length == 0);
		
		// verify JRE container
		entry = entries[2];
		assertEquals(IClasspathEntry.CPE_CONTAINER, entry.getEntryKind());
		assertEquals(new Path(JavaRuntime.JRE_CONTAINER), entry.getPath());
		
		// verify no errors
		assertEquals(0, getProject().findMarkers(null, true, IResource.DEPTH_INFINITE).length);

	}
	
	private void verifyPluginModel(String className, String libraryName, boolean bundle) {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(getProject());
		assertTrue("Model is not found.", model != null);
		IPlugin plugin = (IPlugin)model.getPluginBase();
		assertEquals("com.example.xyz", plugin.getId());
		assertEquals("1.0.0", plugin.getVersion());
		assertEquals("EXAMPLE", plugin.getProviderName());
		assertEquals("Xyz Plug-in", plugin.getName());
		if (className == null)
			assertNull(plugin.getClassName());
		else
			assertEquals(className, plugin.getClassName());
		assertEquals(bundle ? 0 : 1, plugin.getLibraries().length);
		if (!bundle)
			assertEquals(libraryName, plugin.getLibraries()[0].getName());
		assertEquals(0, plugin.getExtensionPoints().length);
		assertEquals(0, plugin.getExtensions().length);
	}

	private void verifyBuildProperties(boolean isBundle, String libraryName, String srcFolder, String outputFolder) {
		IFile buildFile = getProject().getFile("build.properties"); //$NON-NLS-1$
		assertTrue("Build.properties does not exist.", buildFile.exists());
		
		IBuildModel model =  new WorkspaceBuildModel(buildFile);
		try {
			model.load();
		} catch (CoreException e) {
			fail("Model cannot be loaded:" + e);
		}
		
		IBuild build = model.getBuild();
		assertEquals(3, build.getBuildEntries().length);
		
		// verify bin.includes
		IBuildEntry entry = build.getEntry("bin.includes");
		assertNotNull(entry);		
		String[] tokens = entry.getTokens();
		assertEquals(2, tokens.length);
		assertEquals(isBundle ? "META-INF/" : "plugin.xml", tokens[0]);
		if (!libraryName.equals("."))
			assertEquals(libraryName, tokens[1]);
		
		// verify source.<libraryName> and output.<libraryName>
		entry = build.getEntry("source." + libraryName);
		assertNotNull(entry);		
		tokens = entry.getTokens();
		assertEquals(1, tokens.length);
		assertEquals(srcFolder + "/", tokens[0]);
		
		// verify output.<libraryName> and output.<libraryName>
		entry = build.getEntry("output." + libraryName);
		assertNotNull(entry);		
		tokens = entry.getTokens();
		assertEquals(1, tokens.length);
		assertEquals(outputFolder + "/", tokens[0]);
	}

}
