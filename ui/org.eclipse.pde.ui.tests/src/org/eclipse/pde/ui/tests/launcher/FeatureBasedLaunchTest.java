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
 *     Andras Peteri <apeteri@b2international.com> - extracted common superclass
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.junit.Before;
import org.junit.Test;

public class FeatureBasedLaunchTest extends AbstractLaunchTest {

	private ILaunchConfiguration fFeatureBasedWithStartLevels;

	@Before
	public void setupLaunchConfig() throws Exception {
		fFeatureBasedWithStartLevels = getLaunchConfiguration("feature-based-with-startlevels.launch");
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
				.orElseThrow(() -> new AssertionError("no entry found for " + pluginId));

		assertThat("start levels of " + pluginId, actualLevels, is(equalTo(expectedStartLevels)));

	}
}
