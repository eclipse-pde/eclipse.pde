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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.ui.tests.Catalog;
import org.eclipse.pde.ui.tests.wizards.plugin.NewProjectTest;

public class FeatureWithPluginTestCase extends NewProjectTest {

	private static final String PROJECT_NAME = "com.example.test.feature";

	public static Test suite() {
		return new TestSuite(FeatureWithPluginTestCase.class);
	}

	public void testFeatureWithExternalPlugin() {
		playScript(Catalog.FEATURE_WITH_PLUGIN_1);
		verifyProjectExistence();
		verifyFeatureModel("org.eclipse.pde.ui");
	}

	public void testFeatureWithWorkspacePlugin() {
		playScript(Catalog.FEATURE_WITH_PLUGIN_2);
		verifyProjectExistence();
		verifyFeatureModel("com.example.test.plugin");
	}

	protected String getProjectName() {
		return PROJECT_NAME;
	}

	private void verifyFeatureModel(String refPlugin) {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager()
				.findFeatureModels(PROJECT_NAME);
		assertTrue("Model is not found.", models.length > 0);
		IFeature feature = models[0].getFeature();
		assertNotNull("Model's feature is null.", feature);
		IFeaturePlugin[] plugins = feature.getPlugins();
		assertTrue("Feature does not contain plug-in " + refPlugin + ".",
				plugins.length == 1);
		String pluginId = plugins[0].getId();
		assertNotNull("Feature plug-in ID is null.", pluginId);
		assertTrue(refPlugin.equals(pluginId));
		IPluginModel pluginModel = PDECore.getDefault().getModelManager()
				.findPluginModel(pluginId);
		assertNotNull("Model for feature plug-in " + pluginId + " not found ", pluginModel);
	}

}
