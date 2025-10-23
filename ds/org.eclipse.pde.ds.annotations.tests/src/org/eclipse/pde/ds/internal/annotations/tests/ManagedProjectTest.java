package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.ds.internal.annotations.DSAnnotationCompilationParticipant;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class ManagedProjectTest extends CompilationParticipantTest {

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test1";
	}

	@Test
	public void managedProject() throws Exception {
		assertTrue(DSAnnotationCompilationParticipant.isManaged(testProject));
	}

	@Test
	public void dsBuilderConfigured() throws Exception {
		ICommand[] commands = testProject.getDescription().getBuildSpec();
		boolean hasDSBuilder = Arrays.stream(commands).anyMatch(command -> "org.eclipse.pde.ds.core.builder".equals(command.getBuilderName()));
		assertTrue(hasDSBuilder, "DS builder not configured!");
	}

	@Test
	public void folderOSGIInfCreated() throws Exception {
		assertTrue(testProject.getFolder("OSGI-INF").exists(), "Folder OSGI-INF does not exist!");
	}

	@Test
	public void manifestHeaderServiceComponentAdded() throws Exception {
		IPluginModelBase pluginModel = PluginRegistry.findModel(testProject);
		assumeTrue("Test project not a bundle project!", pluginModel instanceof IBundlePluginModelBase);
		IBundleModel bundleModel = ((IBundlePluginModelBase) pluginModel).getBundleModel();
		assumeNotNull("Missing bundle manifest!", bundleModel);
		String serviceComponentHeader = bundleModel.getBundle().getHeader("Service-Component");
		assertNotNull("Missing Service-Component header!", serviceComponentHeader);
		String[] entries = serviceComponentHeader.split("\\s*,\\s*");
		List<String> entryList = Arrays.asList(entries);
		assertEquals(5, entryList.size());
		assertTrue(entryList.contains("OSGI-INF/ds.annotations.test1.DefaultComponent.xml"),
				"Missing Service-Component entry for DefaultComponent!");
		assertTrue(entryList.contains("OSGI-INF/test.fullComponent-v1_2.xml"),
				"Missing Service-Component entry for FullComponentV1_2!");
		assertTrue(entryList.contains("OSGI-INF/test.fullComponent.xml"),
				"Missing Service-Component entry for FullComponent!");
	}

	@Test
	public void noErrorsOrWarnings() throws Exception {
		IMarker[] markers = testProject.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		assertEquals(0, markers.length);
	}
}
