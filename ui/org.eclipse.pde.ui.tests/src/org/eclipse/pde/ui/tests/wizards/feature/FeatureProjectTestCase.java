package org.eclipse.pde.ui.tests.wizards.feature;

import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.feature.CreateFeaturePatchOperation;
import org.eclipse.pde.internal.ui.wizards.feature.CreateFeatureProjectOperation;
import org.eclipse.pde.internal.ui.wizards.feature.FeatureData;
import org.eclipse.pde.ui.tests.NewProjectTest;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class FeatureProjectTestCase extends NewProjectTest {

	private static final String PROJECT_NAME = "com.junitTest.feature";
	private static final FeatureData DEFAULT_FEATURE_DATA = new FeatureData();
	static {
		DEFAULT_FEATURE_DATA.id = PROJECT_NAME;
		DEFAULT_FEATURE_DATA.name = PROJECT_NAME;
		DEFAULT_FEATURE_DATA.version = "1.0.0";
	}
	private static FeatureData createDefaultFeatureData() {
		FeatureData fd = new FeatureData();
		fd.id = DEFAULT_FEATURE_DATA.id;
		fd.name = DEFAULT_FEATURE_DATA.name;
		fd.version = DEFAULT_FEATURE_DATA.version;
		return fd;
	}
	public static Test suite() {
		return new TestSuite(FeatureProjectTestCase.class);
	}
	
	protected String getProjectName() {
		return PROJECT_NAME;
	}
	
	private void createFeature(FeatureData fd, boolean patch, Object modelObject) {
		if (fd == null)
			fd = DEFAULT_FEATURE_DATA;
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		IPath path = Platform.getLocation();
		IRunnableWithProgress op;
		if ((patch && !(modelObject instanceof IFeatureModel))
				|| (!patch && modelObject != null && !(modelObject instanceof IPluginBase[])))
			fail("Unaccepted model object passed...");
		
		if (patch)
			op = new CreateFeaturePatchOperation(
					project, path, fd, (IFeatureModel) modelObject,
					getWorkbench().getDisplay().getActiveShell());
		else
			op = new CreateFeatureProjectOperation(
					project, path, fd, (IPluginBase[]) modelObject,
					getWorkbench().getDisplay().getActiveShell());
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		try {
			progressService.runInUI(progressService, op, null);
		} catch (InvocationTargetException e) {
			fail("Feature creation failed...");
		} catch (InterruptedException e) {
			fail("Feature creation failed...");
		}
	}
	
	private void verifyFeatureNature() {
		assertTrue("Verifying feature nature...", hasNature(PDE.FEATURE_NATURE));
	}
	
	public void testCreationFeatureProject() {
		createFeature(DEFAULT_FEATURE_DATA, false, null);
		verifyProjectExistence();
		verifyFeatureNature();
	}

	public void testCreationFeaturePatch() {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		if (models.length == 0)
			// cant test patches if no feature models exist
			return;
		createFeature(DEFAULT_FEATURE_DATA, true, models[0]);
		verifyProjectExistence();
		verifyFeatureNature();
	}
	
	public void testFeatureProjectData() {
		FeatureData fd = createDefaultFeatureData();
		String library = "testLibrary";
		fd.library = library;
		String provider = "testProvider";
		fd.provider = provider;
		createFeature(fd, false, null);
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModel(DEFAULT_FEATURE_DATA.id);
		IFeature feature = model.getFeature();
		assertTrue(feature.getVersion().equals(DEFAULT_FEATURE_DATA.version));
		assertTrue(feature.getLabel().equals(DEFAULT_FEATURE_DATA.name));
		assertTrue(feature.getId().equals(DEFAULT_FEATURE_DATA.id));
		assertTrue(feature.getProviderName().equals(provider));
		assertTrue(feature.getInstallHandler().getLibrary().equals(library));
	}
	
	public void testSimpleFeature() {
		createFeature(DEFAULT_FEATURE_DATA, false, null);
		verifyProjectExistence();
		assertFalse("Testing simple project for no java nature...", hasNature(JavaCore.NATURE_ID));
	}
	
	public void testJavaFeature() {
		FeatureData fd = createDefaultFeatureData();
		String library = "testLibrary";
		fd.library = library;
		createFeature(fd, false, null);
		verifyProjectExistence();
		assertTrue("Testing for existing java nature...", hasNature(JavaCore.NATURE_ID));
	}
	
	public void testModelCount() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		int numModels = manager.getModels().length;
		createFeature(DEFAULT_FEATURE_DATA, false, null);
		int numModelsNew = manager.getModels().length;
		assertTrue(numModels + 1 == numModelsNew);
	}
	
	public void testMaskingFeature() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		int numModels = manager.getModels().length;
		FeatureData fd = createDefaultFeatureData();
		IFeature pdeFeature = manager.findFeatureModel("org.eclipse.pde").getFeature();
		fd.id = pdeFeature.getId();
		fd.version = pdeFeature.getVersion();
		createFeature(fd, false, null);
		int numNewModels = manager.getModels().length;
		int numInWorkspace = manager.getWorkspaceModels().length;
		assertTrue(numModels == numNewModels);
		assertTrue(numInWorkspace == 1);
	}
}
