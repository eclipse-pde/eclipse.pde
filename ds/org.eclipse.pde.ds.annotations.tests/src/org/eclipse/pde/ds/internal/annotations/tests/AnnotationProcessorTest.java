package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.Document;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@SuppressWarnings("restriction")
public abstract class AnnotationProcessorTest extends TestBase {

	protected IProject testProject;

	protected IDSModel dsModel;

	protected abstract String getTestProjectName();

	protected abstract String getComponentDescriptorPath();

	@BeforeEach
	public void setUp() throws Exception {
		testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(getTestProjectName());
		assertTrue(testProject.exists(), "Test project does not exist!");

		IFile dsFile = testProject.getFile(IPath.fromOSString(getComponentDescriptorPath()));
		dsFile.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue(dsFile.exists(), "Missing component descriptor:" + dsFile);
		String dsFileContent = dsFile.readString();
		dsModel = new DSModel(new Document(dsFileContent), false);
		dsModel.setUnderlyingResource(dsFile);
		dsModel.load();

		assertNotNull(dsModel.getDSComponent());
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (dsModel != null) {
			dsModel.dispose();
		}
	}
}
