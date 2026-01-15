package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
		assumeTrue(testProject.exists(), "Test project does not exist!");

		IFile dsFile = testProject.getFile(IPath.fromOSString(getComponentDescriptorPath()));
		assertTrue(dsFile.exists(),"Missing component descriptor!");

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try (InputStream src = dsFile.getContents()) {
			byte[] bytes = new byte[4096];
			int c;
			while ((c = src.read(bytes)) != -1) {
				buf.write(bytes, 0, c);
			}
		}

		dsModel = new DSModel(new Document(buf.toString(dsFile.getCharset())), false);
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
