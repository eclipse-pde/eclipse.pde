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
package org.eclipse.pde.ui.tests.imports;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.ui.tests.Catalog;
import org.eclipse.pde.ui.tests.PDETestCase;

public class ImportFeatureTestCase extends PDETestCase {

	public static Test suite() {
		return new TestSuite(ImportFeatureTestCase.class);
	}

	public void testImportBinaryFeature1() {
		playScript(Catalog.IMPORT_BINARY_FEATURE_1);
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			verifyProject(root.getProject("org.eclipse.pde-feature"), true);
			verifyModel("org.eclipse.pde", "org.eclipse.pde-feature");
		} catch (CoreException e) {
			fail("testImportBinaryFeature1:" + e);
		}
	}

	public void testImportBinaryFeature2() {
		playScript(Catalog.IMPORT_BINARY_FEATURE_2);
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			verifyProject(root.getProject("org.eclipse.sdk-feature"), true);
			verifyModel("org.eclipse.sdk", "org.eclipse.sdk-feature");
			IFeature feature = PDECore.getDefault().getFeatureModelManager()
					.findFeatureModels("org.eclipse.sdk")[0].getFeature();
			IFeatureChild[] included = feature.getIncludedFeatures();
			assertTrue("SDK feature does not include other features",
					included.length > 0);
			boolean foundPDE = false;
			for (int i = 0; i < included.length; i++) {
				IFeature inclFeature = ((FeatureChild) included[i])
						.getReferencedFeature();
				if (inclFeature != null
						&& "org.eclipse.pde".equals(inclFeature.getId())) {
					foundPDE = true;
				}
			}
			assertTrue("Included feature not found.", foundPDE);
		} catch (CoreException e) {
			fail("testImportBinaryFeature2:" + e);
		}
	}

	public void testImportFeatureAsSource() {
		playScript(Catalog.IMPORT_SOURCE_FEATURE);
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			verifyProject(root.getProject("org.eclipse.pde-feature"), false);
			verifyModel("org.eclipse.pde", "org.eclipse.pde-feature");
		} catch (CoreException e) {
			fail("testImportFeatureAsSource:" + e);
		}
	}

	private void verifyProject(IProject project, boolean binary)
			throws CoreException {
		assertTrue("Project was not created.", project.exists());
		if (binary) {
			assertTrue(
					"Project not binary.",
					PDECore.BINARY_PROJECT_VALUE
							.equals(project
									.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY)));
		} else {
			assertFalse(
					"Project is binary.",
					PDECore.BINARY_PROJECT_VALUE
							.equals(project
									.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY)));

		}
	}

	private void verifyModel(String featureId, String projectName)
			throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager()
				.findFeatureModels(featureId);
		assertTrue("Model not found.", models.length > 0);
		IFeature feature = models[0].getFeature();
		assertTrue("Model has no feature.", feature != null);
		if (feature.getInstallHandler() == null) {
			assertFalse("Project has Java nature.", root
					.getProject(projectName).hasNature(JavaCore.NATURE_ID));
		} else {
			assertTrue("Project does not have Java nature.", root.getProject(
					projectName).hasNature(JavaCore.NATURE_ID));
		}
	}

}
