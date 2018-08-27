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

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.imports.FeatureImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.FeatureImportWizard.ReplaceQuery;
import org.eclipse.pde.ui.tests.wizards.NewProjectTestCase;

public class ImportFeatureProjectsTestCase extends NewProjectTestCase {

	private String fProjectName;

	@Override
	protected void tearDown() {
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

	private void importFeature(IFeatureModel[] models, boolean binary) {
		FeatureImportOperation op = new FeatureImportOperation(models, binary, null, new ReplaceQuery(PDEPlugin.getActiveWorkbenchShell()));
		try {
			PDEPlugin.getWorkspace().run(op, new NullProgressMonitor());
			if (models.length > 0)
				lookingAtProject(models[0]);
		} catch (OperationCanceledException e) {
			fail("Feature import failed...");
		} catch (CoreException e) {
			fail("Feature import failed...");
		}
	}

	private void verifyNatures() {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (IFeatureModel element : imported) {
			lookingAtProject(element);
			assertTrue("Verifying feature nature...", hasNature(PDE.FEATURE_NATURE));
			IFeatureInstallHandler installHandler = element.getFeature().getInstallHandler();
			boolean shouldHaveJavaNature = installHandler != null ? installHandler.getLibrary() != null : false;
			assertTrue("Verifying java nature...", hasNature(JavaCore.NATURE_ID) == shouldHaveJavaNature);
		}
	}

	private void verifyFeature(boolean isBinary) {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (IFeatureModel element : imported) {
			lookingAtProject(element);
			try {
				assertTrue("Verifing feature is binary...", isBinary == PDECore.BINARY_PROJECT_VALUE.equals(getProject().getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY)));
			} catch (CoreException e) {
			}
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

	public void testImportFeature() {
		IFeatureModel[] model = PDECore.getDefault().getFeatureModelManager().getModels();
		if (model.length == 0)
			return;
		boolean binary = false;
		importFeature(new IFeatureModel[] {model[0]}, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
	}

	public void testImportBinaryFeature() {
		IFeatureModel[] model = PDECore.getDefault().getFeatureModelManager().getModels();
		if (model.length == 0)
			return;
		boolean binary = true;
		importFeature(new IFeatureModel[] {model[0]}, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
	}

	public void testImportMulitpleFeatures() {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		if (models.length == 0)
			return;
		boolean binary = false;
		importFeature(models, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		assertTrue("Verifing number models imported...", imported.length == models.length);
	}

	public void testFeaturePlugins() {
		IFeatureModel[] model = PDECore.getDefault().getFeatureModelManager().getModels();
		if (model.length == 0)
			return;
		boolean binary = false;
		importFeature(new IFeatureModel[] {model[0]}, binary);
		verifyProjectExistence();
		verifyNatures();
		verifyFeature(binary);
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		assertTrue("Verifing number models imported...", imported.length == 1);
		IFeaturePlugin[] plugins = model[0].getFeature().getPlugins();
		if (plugins != null) {
			IFeaturePlugin[] importedFeaturePlugins = getFeaturePluginsFrom(model[0].getFeature().getId(), imported);
			assertNotNull("Verifying feature plugins exist...", importedFeaturePlugins);
			assertTrue("Verifying total equal feature plugins...", plugins.length == importedFeaturePlugins.length);
		}
	}

	private IFeaturePlugin[] getFeaturePluginsFrom(String id, IFeatureModel[] imported) {
		for (IFeatureModel element : imported)
			if (element.getFeature().getId().equals(id))
				return imported[0].getFeature().getPlugins();
		return null;
	}

}
