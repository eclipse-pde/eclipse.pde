/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.natures.FeatureProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.imports.FeatureImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.FeatureImportWizard.ReplaceQuery;
import org.eclipse.pde.ui.tests.wizards.NewProjectTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ImportFeatureProjectsTestCase extends NewProjectTestCase {

	private String fProjectName;

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		fProjectName = null;
		super.tearDown();
	}

	@Override
	protected String getProjectName() {
		return fProjectName;
	}

	private void lookingAtProject(IFeatureModel model) {
		String name = model.getFeature().getId();

		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		for (IFeaturePlugin plugin : plugins) {
			if (name.equals(plugin.getId())) {
				name += "-feature"; //$NON-NLS-1$
				break;
			}

		}
		fProjectName = name;
	}

	private void importFeature(IFeatureModel[] models, boolean binary) throws Exception {
		FeatureImportOperation op = new FeatureImportOperation(models, binary, null, new ReplaceQuery(PDEPlugin.getActiveWorkbenchShell()));
		PDEPlugin.getWorkspace().run(op, new NullProgressMonitor());
		if (models.length > 0) {
			lookingAtProject(models[0]);
		}
	}

	private void verifyNatures() {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (IFeatureModel element : imported) {
			lookingAtProject(element);
			assertTrue(hasNature(FeatureProject.NATURE), "Verifying feature nature..."); //$NON-NLS-1$
			IFeatureInstallHandler installHandler = element.getFeature().getInstallHandler();
			boolean shouldHaveJavaNature = installHandler != null ? installHandler.getLibrary() != null : false;
			assertEquals(hasNature(JavaCore.NATURE_ID), shouldHaveJavaNature, "Verifying java nature..."); //$NON-NLS-1$
		}
	}

	private void verifyFeature(boolean isBinary) throws Exception {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (IFeatureModel element : imported) {
			lookingAtProject(element);
			assertEquals(isBinary, PDECore.BINARY_PROJECT_VALUE
					.equals(getProject().getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY)), "Verifing feature is binary..."); //$NON-NLS-1$
		}
	}

	@Override
	protected void verifyProjectExistence() {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (IFeatureModel element : imported) {
			lookingAtProject(element);
			super.verifyProjectExistence();
		}
	}

	@Test
	public void testImportFeature() throws Exception {
		IFeatureModel[] model = PDECore.getDefault().getFeatureModelManager().getModels();
		if (model.length == 0) {
			return;
		}
		boolean binary = false;
		importFeature(new IFeatureModel[] {model[0]}, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
	}

	@Test
	public void testImportBinaryFeature() throws Exception {
		IFeatureModel[] model = PDECore.getDefault().getFeatureModelManager().getModels();
		if (model.length == 0) {
			return;
		}
		boolean binary = true;
		importFeature(new IFeatureModel[] {model[0]}, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
	}

	@Test
	public void testImportMulitpleFeatures() throws Exception {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		if (models.length == 0) {
			return;
		}
		boolean binary = false;
		importFeature(models, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		if (imported.length != models.length) {
			Set<String> expected = toFeatureIds(models);
			Set<String> actual = toFeatureIds(imported);
			assertEquals(expected.toString(), actual.toString(), "Imported models differ from expected"); //$NON-NLS-1$
		}
		assertEquals(models.length, imported.length, "Verifing number models imported..."); //$NON-NLS-1$
	}

	private static TreeSet<String> toFeatureIds(IFeatureModel[] models) {
		return new TreeSet<>(Arrays.stream(models).map(m -> m.getFeature())
				.filter(x -> x != null).map(f -> f.getId()).collect(Collectors.toSet()));
	}

	@Test
	public void testFeaturePlugins() throws Exception {
		IFeatureModel[] model = PDECore.getDefault().getFeatureModelManager().getModels();
		if (model.length == 0) {
			return;
		}
		boolean binary = false;
		importFeature(new IFeatureModel[] {model[0]}, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		assertTrue(imported.length == 1, "Verifing number models imported..."); //$NON-NLS-1$
		IFeaturePlugin[] plugins = model[0].getFeature().getPlugins();
		if (plugins != null) {
			IFeaturePlugin[] importedFeaturePlugins = getFeaturePluginsFrom(model[0].getFeature().getId(), imported);
			assertNotNull(importedFeaturePlugins, "Verifying feature plugins exist..."); //$NON-NLS-1$
			assertEquals(plugins.length, importedFeaturePlugins.length, "Verifying total equal feature plugins..."); //$NON-NLS-1$
		}
	}

	private IFeaturePlugin[] getFeaturePluginsFrom(String id, IFeatureModel[] imported) {
		for (IFeatureModel element : imported) {
			if (element.getFeature().getId().equals(id)) {
				return imported[0].getFeature().getPlugins();
			}
		}
		return null;
	}

}
