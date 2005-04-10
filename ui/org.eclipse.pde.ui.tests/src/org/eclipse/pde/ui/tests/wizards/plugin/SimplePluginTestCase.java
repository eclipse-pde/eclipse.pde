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

import junit.framework.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.ui.tests.*;

public class SimplePluginTestCase extends NewProjectTest {
	
	private static final String PROJECT_NAME = "com.example.simple";

	public static Test suite() {
		return new TestSuite(SimplePluginTestCase.class);
	}

	public void testSimplePluginProject() {
		playScript(Catalog.SIMPLE_PLUGIN_1);
		verifyProjectContent(true);
	}
	
	public void testSimplePluginWithoutManifest() {
		playScript(Catalog.SIMPLE_PLUGIN_2);
		verifyProjectContent(false);
	}
	
	private void verifyProjectContent(boolean isBundle) {
		verifyProjectExistence();
		verifyNatures();
		verifyManifestFiles(isBundle);
		verifyPluginModel();
		verifyBuildProperties(isBundle);			
	}
	
	protected String getProjectName() {
		return PROJECT_NAME;
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
	
	private void verifyNatures() {
		assertTrue("Project does not have a PDE nature.", hasNature(PDE.PLUGIN_NATURE));
		assertFalse("Simple Project has a Java nature.", hasNature(JavaCore.NATURE_ID));
	}

	private void verifyPluginModel() {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(getProject());
		assertTrue("Model is not found.", model != null);
		IPlugin plugin = (IPlugin)model.getPluginBase();
		assertEquals("com.example.simple", plugin.getId());
		assertEquals("1.0.0", plugin.getVersion());
		assertEquals("EXAMPLE", plugin.getProviderName());
		assertEquals("Simple Plug-in", plugin.getName());
		assertNull(plugin.getClassName());
		assertEquals(0, plugin.getLibraries().length);
		assertEquals(0, plugin.getExtensionPoints().length);
		assertEquals(0, plugin.getExtensions().length);
	}

	private void verifyBuildProperties(boolean isBundle) {
		IFile buildFile = getProject().getFile("build.properties"); //$NON-NLS-1$
		assertTrue("Build.properties does not exist.", buildFile.exists());
		
		IBuildModel model =  new WorkspaceBuildModel(buildFile);
		try {
			model.load();
		} catch (CoreException e) {
			fail("Model cannot be loaded:" + e);
		}
		
		IBuild build = model.getBuild();
		assertEquals(1, build.getBuildEntries().length);
		IBuildEntry entry = build.getEntry("bin.includes");
		assertNotNull(entry);
		String[] tokens = entry.getTokens();
		assertEquals(1, tokens.length);
		assertEquals(isBundle ? "META-INF/" : "plugin.xml", tokens[0]);
	}
	
}
