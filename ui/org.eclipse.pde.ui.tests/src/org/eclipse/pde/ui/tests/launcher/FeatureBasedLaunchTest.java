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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class FeatureBasedLaunchTest {

	private ILaunchConfiguration fFeatureBasedWithStartLevels;

	@BeforeClass
	public static void setupTargetPlatform() throws Exception {
		TargetPlatformUtil.setRunningPlatformAsTarget();
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
