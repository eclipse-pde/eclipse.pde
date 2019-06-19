/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import junit.framework.AssertionFailedError;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class FeatureBasedLaunchTest {

	private ILaunchConfiguration fFeatureBasedWithStartLevels;

	@Before
	public void setupTargetPlatform() throws Exception {
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition defaultTarget = tps.newDefaultTarget();
		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(defaultTarget);
		job.schedule();
		job.join();

		IStatus result = job.getResult();
		if (!result.isOK()) {
			throw new AssertionError(result.getMessage(), result.getException());
		}

		// FIXME: When run in tycho, the ${eclipse_home} installation doesn't
		// use simpleconfigurator. PDE only picks up the
		// simpleconfigurator/bundles.info from the configuration area when
		// using the installation as target, not the osgi.bundles from
		// config.ini
		// --> target is missing almost all bundles and this test won't work

		boolean functionalTargetPlatform = Arrays.stream(PluginRegistry.getActiveModels(false))
				.anyMatch(p -> Platform.PI_RUNTIME.equals(p.getPluginBase().getId()));
		Assume.assumeTrue("core platform bundles are missing in ${eclipse_home} target", functionalTargetPlatform);
	}

	@Before
	public void setupLaunchConfig() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(getClass().getSimpleName());
		if (project.exists()) {
			project.delete(true, null);
		}

		project.create(null);
		project.open(null);
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		ArrayList<URL> resources = Collections.list(bundle.findEntries("tests/launch", "*", false));
		for (URL url : resources) {
			Path path = Paths.get(FileLocator.toFileURL(url).toURI());
			IFile file = project.getFile(path.getFileName().toString());
			try (InputStream in = url.openStream()) {
				file.create(in, true, null);
			}
		}

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		fFeatureBasedWithStartLevels = launchManager
				.getLaunchConfiguration(project.getFile("feature-based-with-startlevels.launch"));
	}

	@Test
	public void testOldEntryWithoutConfigurationHasDefaults() throws Exception {
		checkStartLevels("javax.inject", "default:default");
	}

	@Test
	public void testUseConfiguredStartLevels() throws Exception {
		checkStartLevels("org.eclipse.core.runtime", "1:true");
	}

	@Test
	public void testIgnoreConfiguredStartLevelsOfUncheckedPlugin() throws Exception {
		checkStartLevels("org.eclipse.ui", "default:default");
	}

	private void checkStartLevels(String pluginId, String expectedStartLevels) throws CoreException {
		Map<IPluginModelBase, String> bundleMap = BundleLauncherHelper.getMergedBundleMap(fFeatureBasedWithStartLevels,
				false);
		String actualLevels = bundleMap.entrySet().stream()
				.filter(e -> pluginId.equals(e.getKey().getPluginBase().getId())).map(e -> e.getValue()).findFirst()
				.orElseThrow(() -> new AssertionFailedError("no entry found for " + pluginId));

		assertThat("start levels of " + pluginId, actualLevels, is(equalTo(expectedStartLevels)));

	}
}
