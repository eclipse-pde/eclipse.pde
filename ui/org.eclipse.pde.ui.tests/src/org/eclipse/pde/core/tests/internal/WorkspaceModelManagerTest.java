/*******************************************************************************
 *  Copyright (c) 2022, 2022 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.WorkspacePluginModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;

/**
 * Tests for the abstract {@link WorkspaceModelManager}, using Plug-in projects
 * and therefore the {@link WorkspacePluginModelManager} as
 * 'test'-implementation.
 */
public class WorkspaceModelManagerTest {
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	private static final Set<WorkspaceModelManager<?>> openManagers = ConcurrentHashMap.newKeySet();

	@After
	public void tearDown() {
		openManagers.forEach(WorkspaceModelManager::shutdown);
	}

	@Test
	public void testGetModel_projectCreated() throws CoreException {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = getWorkspaceProject("plugin.a");
		assertNull(mm.getModel(project));
		createModelProject("plugin.a", "1.0.0");
		IPluginModelBase model = mm.getModel(project);
		assertExistingModel("plugin.a", "1.0.0", model);
	}

	@Test
	public void testGetModel_workspaceStartUpWithExistingProject() throws CoreException {
		// simulate start-up with workspace with existing, open projects
		IProject existingProject = createModelProject("plugin.a", "1.0.0");
		TestWorkspaceModelManager mm = createWorkspaceModelManager(false);
		IPluginModelBase model = mm.getModel(existingProject);
		assertExistingModel("plugin.a", "1.0.0", model);
	}

	@Test
	public void testChangeEvents_singleModelCreated() throws CoreException {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		List<IModelProviderEvent> events = new ArrayList<>();
		mm.addModelProviderListener(events::add);

		IPluginModelBase model = mm.getModel(createModelProject("plugin.a", "1.0.0"));

		assertEquals(1, events.size());
		IModelProviderEvent event = events.get(0);
		assertEquals(IModelProviderEvent.MODELS_ADDED, event.getEventTypes());
		assertEquals(1, event.getAddedModels().length);
		assertEquals(0, event.getChangedModels().length);
		assertEquals(0, event.getRemovedModels().length);
		assertSame(model, event.getAddedModels()[0]);
		assertSame(mm, event.getEventSource());

		mm.shutdown();
	}

	// MODELS_CHANGED events are not properly handled if multiple
	// WorkspaceModelManager exist because the first one re-loads the model into
	// the same model object so all subsequently notified managers do not detect
	// a difference -> This case is skipped for now

	@Test
	public void testChangeEvents_singleModelRemoved() throws CoreException {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = createModelProject("plugin.a", "1.0.0");
		IPluginModelBase model = mm.getModel(project);
		List<IModelProviderEvent> events = new ArrayList<>();
		mm.addModelProviderListener(events::add);

		project.delete(true, true, null);

		assertEquals(1, events.size());
		IModelProviderEvent event = events.get(0);
		assertEquals(IModelProviderEvent.MODELS_REMOVED, event.getEventTypes());
		assertEquals(0, event.getAddedModels().length);
		assertEquals(0, event.getChangedModels().length);
		assertEquals(1, event.getRemovedModels().length);
		assertSame(model, event.getRemovedModels()[0]);
		assertSame(mm, event.getEventSource());
	}

	@Test
	public void testBundleRootHandling_projectCreatedWithNonDefaultBundleRoot() throws CoreException {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IPath bundleRootPath = Path.forPosix("other/root");

		IProject project = ProjectUtils.createPluginProject("plugin.a", "plugin.a", "1.0.0", (description, service) -> {
			description.setBundleRoot(bundleRootPath);
		});

		IPluginModelBase model = mm.getModel(project);
		assertExistingModel("plugin.a", "1.0.0", model);
		assertFalse(manifest(project).exists());
		assertTrue(project.getFile("other/root/META-INF/MANIFEST.MF").exists());
		assertEquals(project.getFile("other/root/META-INF/MANIFEST.MF"), model.getUnderlyingResource());
	}

	@Test
	public void testOpenAndClose_projectWithChangedBundleRoot() throws Exception {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = createModelProject("plugin.a", "1.0.0");

		copyFile(manifest(project), manifestIn(project, "otherRoot"), replaceVersionTo("2.0.0"));
		setBundleRoot(project, "otherRoot");
		manifest(project).delete(true, null);

		project.close(null);

		assertNull(mm.getModel(project));

		project.open(null);

		IPluginModelBase model = mm.getModel(project);
		assertExistingModel("plugin.a", "2.0.0", model);
		assertEquals(manifestIn(project, "otherRoot"), model.getUnderlyingResource());
	}

	@Test
	public void testDelete_projectWithChangedBundleRoot() throws Exception {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = createModelProject("plugin.a", "1.0.0");

		copyFile(manifest(project), manifestIn(project, "otherRoot"), replaceVersionTo("2.0.0"));
		setBundleRoot(project, "otherRoot");
		manifest(project).delete(true, null);

		project.delete(true, null);
		assertNull(mm.getModel(project));
	}

	@Test
	public void testBundleRootHandling_bundleRootChangedFromDefaultToOthersAndReverse() throws Exception {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = createModelProject("plugin.a", "1.0.0");
		copyFile(manifest(project), manifestIn(project, "otherRoot"), replaceVersionTo("2.0.0"));
		copyFile(manifest(project), manifestIn(project, "root2"), replaceVersionTo("3.0.0"));

		setBundleRoot(project, "otherRoot");

		IPluginModelBase model1 = mm.getModel(project);
		assertExistingModel("plugin.a", "2.0.0", model1);
		assertEquals(manifestIn(project, "otherRoot"), model1.getUnderlyingResource());

		setBundleRoot(project, "root2");

		IPluginModelBase model2 = mm.getModel(project);
		assertExistingModel("plugin.a", "3.0.0", model2);
		assertEquals(manifestIn(project, "root2"), model2.getUnderlyingResource());

		setBundleRoot(project, null);

		IPluginModelBase model0 = mm.getModel(project);
		assertExistingModel("plugin.a", "1.0.0", model0);
		assertEquals(manifest(project), model0.getUnderlyingResource());
	}

	@Test
	public void testBundleRootHandling_bundleRootChangedFromNoneToOther() throws Exception {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = createModelProject("plugin.a", "1.0.0");

		copyFile(manifest(project), manifestIn(project, "otherRoot"), replaceVersionTo("2.0.0"));

		manifest(project).delete(true, null);
		assertNull(mm.getModel(project));

		setBundleRoot(project, "otherRoot");

		IPluginModelBase model = mm.getModel(project);
		assertExistingModel("plugin.a", "2.0.0", model);
		assertEquals(manifestIn(project, "otherRoot"), model.getUnderlyingResource());
	}

	@Test
	public void testBundleRootHandling_bundleRootChangedFromNoneToDefault() throws Exception {
		TestWorkspaceModelManager mm = createWorkspaceModelManager();
		IProject project = createModelProject("plugin.a", "1.0.0");

		copyFile(manifest(project), manifestIn(project, "otherRoot"), replaceVersionTo("2.0.0"));
		setBundleRoot(project, "otherRoot");

		manifestIn(project, "otherRoot").delete(true, null);
		assertNull(mm.getModel(project));

		setBundleRoot(project, null);

		IPluginModelBase model = mm.getModel(project);
		assertExistingModel("plugin.a", "1.0.0", model);
		assertEquals(manifest(project), model.getUnderlyingResource());
	}

	// --- utilities ---

	// This class tests tests the abstract WorkspaceModelManager using the
	// specific WorkspacePluginModelManager as 'example'.
	private static class TestWorkspaceModelManager extends WorkspacePluginModelManager {
		// Make protected methods visible to tests
		@Override
		public IPluginModelBase getModel(IProject project) {
			return super.getModel(project);
		}

		@Override
		protected IPluginModelBase[] getPluginModels() {
			return super.getPluginModels();
		}
	}

	private TestWorkspaceModelManager createWorkspaceModelManager() {
		return createWorkspaceModelManager(true);
	}

	protected static TestWorkspaceModelManager createWorkspaceModelManager(boolean init) {
		TestWorkspaceModelManager mm = new TestWorkspaceModelManager();
		openManagers.add(mm);
		if (init) {
			// ensure manager is initialized, otherwise events can be missed
			assertEquals(0, mm.getPluginModels().length);
		}
		return mm;
	}

	protected static IProject getWorkspaceProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	private static IProject createModelProject(String symbolicName, String version) throws CoreException {
		IProject project = ProjectUtils.createPluginProject(symbolicName, symbolicName, version);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		return project;
	}

	private static IFile manifest(IProject project) {
		return project.getFile("META-INF/MANIFEST.MF");
	}

	private static IFile manifestIn(IProject project, String path) {
		return project.getFile(path + "/META-INF/MANIFEST.MF");
	}

	private void setBundleRoot(IProject project, String path) throws CoreException {
		PDEProject.setBundleRoot(project, path != null ? project.getFolder(path) : null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	private void copyFile(IFile source, IFile target, UnaryOperator<String> modifications)
			throws IOException, CoreException {
		try (Stream<String> lines = Files.lines(java.nio.file.Path.of(source.getLocationURI()))) {
			Iterable<String> bs = lines.map(modifications)::iterator;
			java.nio.file.Path targetPath = java.nio.file.Path.of(target.getLocationURI());
			Files.createDirectories(targetPath.getParent());
			Files.write(targetPath, bs);
			target.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
	}

	private static UnaryOperator<String> replaceVersionTo(String newVersion) {
		return l -> l.startsWith(Constants.BUNDLE_VERSION) ? Constants.BUNDLE_VERSION + ": " + newVersion : l;
	}

	private static void assertExistingModel(String symbolicName, String version, IPluginModelBase model) {
		assertNotNull(model);
		assertEquals(symbolicName, model.getPluginBase().getId());
		assertEquals(version, model.getPluginBase().getVersion());
	}

}
