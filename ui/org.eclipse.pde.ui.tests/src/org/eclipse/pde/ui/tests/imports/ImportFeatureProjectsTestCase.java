package org.eclipse.pde.ui.tests.imports;

import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.FeatureImportWizard;
import org.eclipse.pde.ui.tests.NewProjectTestCase;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class ImportFeatureProjectsTestCase extends NewProjectTestCase {

	private String fProjectName;
	
	public static Test suite() {
		return new TestSuite(ImportFeatureProjectsTestCase.class);
	}
	
	protected void tearDown() {
		fProjectName = null;
		super.tearDown();
	}
	
	protected String getProjectName() {
		return fProjectName;
	}
	
	private void lookingAtProject(IFeatureModel model) {
		String name = model.getFeature().getId();

		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			if (name.equals(plugins[i].getId())) {
				name += "-feature"; //$NON-NLS-1$
				break;
			}

		}
		fProjectName = name;
	}
	
	private void importFeature(IFeatureModel[] models, boolean binary) {
		IRunnableWithProgress op = FeatureImportWizard.getImportOperation(getShell(), binary, models, null);
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		try {
			progressService.runInUI(progressService, op, null);
			if (models.length > 0)
				lookingAtProject(models[0]);
		} catch (InvocationTargetException e) {
			fail("Feature import failed...");
		} catch (InterruptedException e) {
			fail("Feature import failed...");
		}
	}
	
	private void verifyNatures() {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (int i = 0; i < imported.length; i++) {
			lookingAtProject(imported[i]);
			assertTrue("Verifying feature nature...", hasNature(PDE.FEATURE_NATURE));
			IFeatureInstallHandler installHandler = imported[i].getFeature().getInstallHandler();
			boolean shouldHaveJavaNature = 
				installHandler != null ? installHandler.getLibrary() != null : false;
			assertTrue("Verifying java nature...", hasNature(JavaCore.NATURE_ID) == shouldHaveJavaNature);
		}
	}
	
	private void verifyFeature(boolean isBinary) {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (int i = 0; i < imported.length; i++) {
			lookingAtProject(imported[i]);
			try {
				assertTrue("Verifing feature is binary...", 
						isBinary == PDECore.BINARY_PROJECT_VALUE.equals(getProject().getPersistentProperty(
								PDECore.EXTERNAL_PROJECT_PROPERTY)));
			} catch (CoreException e) {
			}
		}
	}
	
	protected void verifyProjectExistence() {
		IFeatureModel[] imported = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (int i = 0; i < imported.length; i++) {
			lookingAtProject(imported[i]);
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
		for (int i = 0; i < imported.length; i++)
			if (imported[i].getFeature().getId().equals(id))
				return imported[0].getFeature().getPlugins();
		return null;
	}
	
}
