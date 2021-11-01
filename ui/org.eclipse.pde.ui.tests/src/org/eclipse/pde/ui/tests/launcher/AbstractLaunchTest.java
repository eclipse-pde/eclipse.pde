/*******************************************************************************
 *  Copyright (c) 2019, 2021 Julian Honnen and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *     Andras Peteri <apeteri@b2international.com> - extracted common superclass
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.ui.tests.project.ProjectCreationTests;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
import org.junit.rules.TestRule;
import org.osgi.framework.*;

public abstract class AbstractLaunchTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;

	private static IProject launchConfigsProject;

	@BeforeClass
	public static void setupTargetPlatform() throws Exception {
		deleteProjects(Set.of()); // Clean-up garbage of other test-classes
		TargetPlatformUtil.setRunningPlatformAsTarget();
		setupLaunchConfigurations();
	}

	private static void setupLaunchConfigurations() throws Exception {
		launchConfigsProject = getProject(AbstractLaunchTest.class.getSimpleName());
		launchConfigsProject.create(null);
		launchConfigsProject.open(null);
		Bundle bundle = FrameworkUtil.getBundle(AbstractLaunchTest.class);
		List<URL> resources = Collections.list(bundle.findEntries("tests/launch", "*", false));
		for (URL url : resources) {
			Path path = Path.of(FileLocator.toFileURL(url).toURI());
			IFile file = launchConfigsProject.getFile(path.getFileName().toString());
			try (InputStream in = url.openStream()) {
				file.create(in, true, null);
			}
		}
	}

	@AfterClass
	public static void restoreInitialWorkspaceState() throws Exception {
		deleteProjects(Set.of());
	}

	private Set<IProject> projectsBefore;

	@Before
	public void storeExistingProjects() {
		projectsBefore = Set.of(ResourcesPlugin.getWorkspace().getRoot().getProjects());
	}

	@After
	public void deleteCreatedProjects() throws CoreException {
		deleteProjects(projectsBefore);
	}

	private static void deleteProjects(Set<IProject> retainedProjects) throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (retainedProjects.stream().noneMatch(s -> s.contains(project))) {
				project.delete(true, true, null);
			}
		}
	}

	protected ILaunchConfiguration getLaunchConfiguration(String name) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		return launchManager.getLaunchConfiguration(launchConfigsProject.getFile(name));
	}

	protected static IProject createPluginProject(String projectName, String bundleSymbolicName, String version)
			throws CoreException {
		IProject project = getProject(projectName);

		IBundleProjectDescription description = ProjectCreationTests.getBundleProjectService().getDescription(project);
		description.setSymbolicName(bundleSymbolicName);
		if (version != null) {
			description.setBundleVersion(Version.parseVersion(version));
		}
		description.apply(null);
		return project;
	}

	private static IProject getProject(String projectName) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {
			project.delete(true, null);
		}
		return project;
	}

	protected static IPluginModelBase findWorkspaceModel(String id, String version) {
		return getModel(id, version, ModelEntry::getWorkspaceModels, "workspace");
	}

	protected static IPluginModelBase findTargetModel(String id, String version) {
		return getModel(id, version, ModelEntry::getExternalModels, "target");
	}

	private static IPluginModelBase getModel(String id, String versionStr,
			Function<ModelEntry, IPluginModelBase[]> modelsGetter, String type) {

		ModelEntry entry = PluginRegistry.findEntry(id);
		assertNotNull("entry '" + id + "' should be present in PluginRegistry", entry);
		IPluginModelBase[] models = modelsGetter.apply(entry);
		assertTrue("entry '" + id + "' should have " + type + " models", models.length > 0);

		if (versionStr == null) {
			return models[0];
		}
		Version version = Version.parseVersion(versionStr);
		Stream<IPluginModelBase> candiates = Arrays.stream(models);
		return candiates.filter(model -> version.equals(Version.parseVersion(model.getPluginBase().getVersion())))
				.findFirst() // always take first like BundleLaunchHelper
				.orElseThrow(() -> new NoSuchElementException("No " + type + " model " + id + "-" + version + "found"));
	}
}
