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
package org.eclipse.pde.ui.tests.wizards.feature;

import junit.framework.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.ui.tests.*;
import org.eclipse.pde.ui.tests.wizards.plugin.NewProjectTest;

public class SimpleFeatureTestCase extends NewProjectTest {

	private static final String PROJECT_NAME = "com.example.test.feature";

	public static Test suite() {
		return new TestSuite(SimpleFeatureTestCase.class);
	}

	public void testSimpleFeatureProject() {
		playScript(Catalog.SIMPLE_FEATURE_1);
		verifyProjectContent(false);
	}

	public void testSimpleFeatureWithInstallHandlerProject() {
		playScript(Catalog.SIMPLE_FEATURE_2);
		verifyProjectContent(true);
	}

	private void verifyProjectContent(boolean isJava) {
		verifyProjectExistence();
		assertNatures(isJava);
		verifyFeatureModel(isJava);
		verifyBuildProperties(isJava);
	}

	protected String getProjectName() {
		return PROJECT_NAME;
	}

	private void assertNatures(boolean isJava) {
		assertTrue("Feature project does not have a feature nature.",
				hasNature(PDE.FEATURE_NATURE));
		assertFalse("Feature project had plugin nature.",
				hasNature(PDE.PLUGIN_NATURE));
		if (isJava) {
			assertTrue("Java Project has no Java nature.",
					hasNature(JavaCore.NATURE_ID));
		} else {
			assertFalse("Simple Project has a Java nature.",
					hasNature(JavaCore.NATURE_ID));
		}
	}

	private void verifyFeatureModel(boolean isJava) {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager()
				.findFeatureModels(PROJECT_NAME);
		assertTrue("Model is not found.", models.length > 0);
		IFeature feature = models[0].getFeature();
		assertNotNull("Model's feature is null.", feature);
		assertEquals(PROJECT_NAME, feature.getId());
		assertEquals("1.0.1", feature.getVersion());
		assertEquals("Eclipse.org", feature.getProviderName());
		assertEquals("Test Feature", feature.getLabel());
		assertTrue(feature.isValid());
		assertTrue((feature.getInstallHandler() != null) == isJava);
	}

	private void verifyBuildProperties(boolean isJava) {
		IFile buildFile = getProject().getFile("build.properties"); //$NON-NLS-1$
		assertTrue("Build.properties does not exist.", buildFile.exists());

		IBuildModel model = new WorkspaceBuildModel(buildFile);
		try {
			model.load();
		} catch (CoreException e) {
			fail("Model cannot be loaded:" + e);
		}

		IBuild build = model.getBuild();
		assertEquals(isJava ? 3 : 1, build.getBuildEntries().length);
		IBuildEntry entry = build.getEntry("bin.includes");
		assertNotNull(entry);
		String[] tokens = entry.getTokens();
		assertEquals(isJava ? 2 : 1, tokens.length);
		assertEquals(tokens[0], "feature.xml");
		if (isJava) {
			assertEquals(tokens[1], "handler.jar");

			entry = build.getEntry("source.handler.jar");
			assertNotNull(entry);
			tokens = entry.getTokens();
			assertEquals(1, tokens.length);
			String sourceFolder = PreferenceConstants.getPreferenceStore()
					.getString(PreferenceConstants.SRCBIN_SRCNAME);
			assertEquals(tokens[0], sourceFolder + "/");

			entry = build.getEntry("output.handler.jar");
			assertNotNull(entry);
			tokens = entry.getTokens();
			assertEquals(1, tokens.length);
			String buildFolder = PreferenceConstants.getPreferenceStore()
					.getString(PreferenceConstants.SRCBIN_BINNAME);
			assertEquals(tokens[0], buildFolder + "/");
		}
	}

}
