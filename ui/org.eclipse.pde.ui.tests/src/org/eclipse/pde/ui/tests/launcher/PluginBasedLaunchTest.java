/*******************************************************************************
 *  Copyright (c) 2021, 2022 Hannes Wellmann and others.
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
package org.eclipse.pde.ui.tests.launcher;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.eclipse.pde.ui.tests.launcher.FeatureBasedLaunchTest.concat;
import static org.eclipse.pde.ui.tests.launcher.FeatureBasedLaunchTest.toDefaultStartData;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.resolution;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.EclipseApplicationLaunchConfiguration;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Constants;

public class PluginBasedLaunchTest extends AbstractLaunchTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Path tpJarDirectory;

	@Before
	public void setupPluginProjects() throws Exception {
		tpJarDirectory = folder.newFolder("TPJarDirectory").toPath();
	}

	// --- test cases for getMergedBundleMap() ----

	@Test
	public void testGetMergedBundleMap_startDataParsing() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.f", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of( //
					"plugin.a*1.0.0@5:autoStart", //
					"plugin.b*1.0.0@anyText", //
					"plugin.c*1.0.0"));
		};

		Map<BundleLocationDescriptor, String> expectedBundleMap = Map.of( //
				workspaceBundle("plugin.a", "1.0.0"), "5:autoStart", //
				workspaceBundle("plugin.b", "1.0.0"), "anyText", //
				workspaceBundle("plugin.c", "1.0.0"), "default:default");

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundleMap);
	}

	@Test
	public void testGetMergedBundleMap_mixedPluginsFromWorkspaceAndTarget_specificTargetVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.x", "1.0.0"), //
				bundle("plugin.x", "2.0.0"), //
				bundle("plugin.x", "3.0.0"), //
				bundle("plugin.y", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0", "plugin.b"));
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES,
					Set.of("plugin.x*2.0.0", "plugin.x*3.0.0", "plugin.y"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.x", "2.0.0"), //
				targetBundle("plugin.x", "3.0.0"), //
				targetBundle("plugin.y", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_mixedPluginsFromWorkspaceWithAutomaticAddAndTarget_specificTargetVersion()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"), //
				bundle("plugin.d", "2.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.x", "1.0.0"), //
				bundle("plugin.x", "2.0.0"), //
				bundle("plugin.x", "3.0.0"), //
				bundle("plugin.y", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0", "plugin.b"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*2.0.0", "plugin.c"));
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES,
					Set.of("plugin.x*2.0.0", "plugin.x*3.0.0", "plugin.y"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.b", "1.0.0"), //
				workspaceBundle("plugin.d", "1.0.0"), //
				workspaceBundle("plugin.d", "2.0.0"), //
				targetBundle("plugin.x", "2.0.0"), //
				targetBundle("plugin.x", "3.0.0"), //
				targetBundle("plugin.y", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_defaultLaunchWithAllPlugins_fragmentsForOtherEnvironmentExcluded()
			throws Exception {
		String os = Platform.getOS();
		String otherOS = !os.equals(Platform.OS_WIN32) ? Platform.OS_WIN32 : Platform.OS_LINUX;
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("fragment.a", "1.0.0", entry(Constants.FRAGMENT_HOST, "plugin.a")), //
				bundle("fragment.b", "1.0.0", entry(Constants.FRAGMENT_HOST, "plugin.a"),
						entry(ICoreConstants.PLATFORM_FILTER, "(osgi.os=" + os + ")")), //
				bundle("fragment.c", "1.0.0", entry(Constants.FRAGMENT_HOST, "plugin.a"),
						entry(ICoreConstants.PLATFORM_FILTER, "(osgi.os=" + otherOS + ")")));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("fragment.a", "1.0.0"), //
				targetBundle("fragment.b", "1.0.0"));

		assertGetMergedBundleMap(Map.of(), targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	// workspace plug-ins selected explicitly

	@Test
	public void testGetMergedBundleMap_singleWorkspacePluginVersion_specificVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_singleWorkspacePluginVersion_noVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_singleWorkspacePluginVersion_notMatchingVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.1"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "1.0.1"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleWorkspacePluginVersions_specificVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleWorkspacePluginVersions_multipleSpecificVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "1.0.1"), //
				bundle("plugin.a", "2.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
					Set.of("plugin.a*1.0.0", "plugin.a*2.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.a", "2.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleWorkspacePluginVersions_noVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "2.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleWorkspacePluginVersions_sameVersion() throws Exception {
		ProjectUtils.createPluginProject("another.project", "plugin.a", "1.0.0");
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleWorkspacePluginVersions_sameMMMVersionButDifferentQualifier()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0.qualifier"), //
				bundle("plugin.a", "1.0.0.202111250056"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
					Set.of("plugin.a*1.0.0.qualifier", "plugin.a*1.0.0.202111250056"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0.qualifier"), //
				workspaceBundle("plugin.a", "1.0.0.202111250056"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMapForIssue88_specifiedWorkspacePluginOnlyInTargetPlatform_emtpySelection()
			throws Exception {
		// Test for https://github.com/eclipse-pde/eclipse.pde/issues/88
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.b"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of();
		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	// workspace plug-ins added automatically

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_noDisabledPlugins() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.c", "3.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.a", "2.0.0"), //
				workspaceBundle("plugin.c", "3.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_singleVersionPluginDisabledWithoutVersion()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.c", "3.0.0"), //
				bundle("plugin.d", "3.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES, Set.of("plugin.c"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.d", "3.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_singleVersionPluginV1_0_0DeselectedButHaveV1_0_1InWorkspace()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.c", "1.0.1"), //
				bundle("plugin.d", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES, Set.of("plugin.c*1.0.0"));
		};

		// Version 1.0.0 is deselected but version 1.0.1 is actually excluded.
		// That seems to be not intuitive at first moment, but it is desired.
		// Think of the following scenario:
		// a) you create plugin with version 1
		// b) it's deselected in the launch config
		// c) [some time later] plugin gets a version increment
		// --> it should still be disabled
		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.d", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_multiVersionPluginDisabledWithSpecificVersion()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "3.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES,
					Set.of("plugin.a*2.0.0", "plugin.a*4.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.a", "3.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_multiVersionPluginDisabledWithMultipleSpecificVersions()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "3.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of());
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES,
					Set.of("plugin.a*1.0.0", "plugin.a*3.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "2.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_multiVersionPluginDisabledWithoutVersion()
			throws Exception {

		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "3.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*2.0.0"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(workspaceBundle("plugin.a", "2.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_automaticAddedWorkspacePlugins_sameMMMVersionButDifferentQualifier()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0.qualifier"), //
				bundle("plugin.a", "1.0.0.202111250056"), //
				bundle("plugin.b", "2.0.0.qualifier"), //
				bundle("plugin.b", "2.0.0.202111250056"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0.202111250056"));
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0.qualifier"), //
				workspaceBundle("plugin.a", "1.0.0.202111250056"), //
				workspaceBundle("plugin.b", "2.0.0.qualifier"), //
				workspaceBundle("plugin.b", "2.0.0.202111250056"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	// only target plug-ins selected explicitly

	@Test
	public void testGetMergedBundleMap_singleTargetPluginVersion_specificVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.b*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(targetBundle("plugin.b", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_singleTargetPluginVersion_noVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.b"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(targetBundle("plugin.b", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_singleTargetPluginVersion_notMatchingVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.1"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.b*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(targetBundle("plugin.b", "1.0.1"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleTargetPluginVersions_specificVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.b", "2.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.b*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of(targetBundle("plugin.b", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleTargetPluginVersions_multipleSpecificVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.b", "2.0.0"), //
				bundle("plugin.b", "3.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.b*1.0.0", "plugin.b*3.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				targetBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.b", "3.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleTargetPluginVersions_noVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.b", "2.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.b"));
		};
		Set<BundleLocationDescriptor> expectedBundles = Set.of(//
				targetBundle("plugin.b", "2.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_singleTargetPluginVersion_notSelectedWorkspacePendant() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a", "plugin.b*1.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.b", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_multipleTargetPluginVersions_sameMMMVersionButDifferentQualifier()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.1.qualifier"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.0.2020"), //
				bundle("plugin.a", "1.0.0.2021"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES,
					new LinkedHashSet<>(List.of("plugin.a*1.0.0.2020", "plugin.a*1.0.0.2021")));
		}; // first entry is selected -> LinkedHashSet ensures its the same

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				targetBundle("plugin.a", "1.0.0.2020"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	// workspace and target plug-ins with same bundle-SymbolicName

	@Test
	public void testGetMergedBundleMap_pluginFromWorkspaceAndTarget_specificTargetVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.1"), //
				bundle("plugin.b", "2.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
					Set.of("plugin.a*1.0.0", "plugin.b*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a*1.0.1", "plugin.b*2.0.0"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.a", "1.0.1"), //
				targetBundle("plugin.b", "2.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_pluginFromWorkspaceAndTarget_noTargetVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "2.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.1"), //
				bundle("plugin.b", "3.0.0"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
					Set.of("plugin.a*1.0.0", "plugin.b*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a", "plugin.b"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.b", "2.0.0"), //
				targetBundle("plugin.a", "1.0.1"), //
				targetBundle("plugin.b", "3.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_pluginFromWorkspaceAndTarget_notMatchingTargetVersion() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.2"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a*1.0.1"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.a", "1.0.2"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_pluginFromWorkspaceAndTarget_targetBundleReplacedByWorkspaceBundleWithSameVersion()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0.qualifier"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0.202111102345"));

		// Only a workspace plug-in with same major-minor-micro version
		// (disregarding the qualifier) replaces a selected target-bundle
		Consumer<ILaunchConfigurationWorkingCopy> launchConfig = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of("plugin.a", "plugin.b"));
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a*1.0.0", "plugin.b"));
		};

		// Expect version from workspace
		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.b", "1.0.0.qualifier"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfig, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_workspacePluginAddedAutomaticallyAndTargetPlugin_differentVersions()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.1"), //
				bundle("plugin.b", "1.0.1"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a", "plugin.b*1.0.1"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.a", "1.0.1"), //
				targetBundle("plugin.b", "1.0.1"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	@Test
	public void testGetMergedBundleMap_workspacePluginAddedAutomaticallyAndTargetPlugin_sameVersionLikeInWorkspace()
			throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "1.0.0.202111102345"));

		Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a*1.0.0.202111102345"));
		};

		Set<BundleLocationDescriptor> expectedBundles = Set.of( //
				workspaceBundle("plugin.a", "1.0.0"));

		assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
	}

	// --- miscellaneous cases ---

	@Test
	public void testGetMergedBundleMap_automaticallyAddRequirements() throws Exception {

		var targetPlatformBundles = Map.ofEntries( //
				bundle("plugin.a", "1.0.0", //
						entry(REQUIRE_BUNDLE, "plugin.b,plugin.c" + resolution(RESOLUTION_OPTIONAL))),
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"));

		Map<BundleLocationDescriptor, String> requiredRPBundlesWithoutOptional = FeatureBasedLaunchTest
				.getEclipseAppRequirementClosureForRunningPlatform(
						DependencyManager.Options.INCLUDE_EXTENSIBLE_FRAGMENTS);

		Map<BundleLocationDescriptor, String> requiredRPBundlesWithOptional = FeatureBasedLaunchTest
				.getEclipseAppRequirementClosureForRunningPlatform(
						DependencyManager.Options.INCLUDE_OPTIONAL_DEPENDENCIES,
						DependencyManager.Options.INCLUDE_EXTENSIBLE_FRAGMENTS);

		TargetPlatformUtil.setRunningPlatformWithDummyBundlesAsTarget(null, targetPlatformBundles, List.of(),
				tpJarDirectory);

		Consumer<ILaunchConfigurationWorkingCopy> basicSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a*1.0.0"));
			wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
			wc.setAttribute(IPDELauncherConstants.PRODUCT, "org.eclipse.platform.ide");
			// prevent BundleLaunchHelper.migrateLaunchConfiguration() from
			// adding more target plug-ins:
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3");
		};

		// test automatic-add-requirements disabled (which is the default)
		for (Boolean autoAddRequirements : Arrays.asList(false, null)) {
			assertGetMergedBundleMap(wc -> {
				basicSetup.accept(wc);
				wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, autoAddRequirements);
			}, toDefaultStartData(Set.of( //
					targetBundle("plugin.a", "1.0.0"))));
		}

		// test enabled automatic-add-requirements
		// with optional requirements enabled (which is the default)
		for (Boolean includeOptional : Arrays.asList(true, null)) {
			assertGetMergedBundleMap(wc -> {
				basicSetup.accept(wc);
				wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, true);
				wc.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, includeOptional);
			}, concat(requiredRPBundlesWithOptional, toDefaultStartData(Set.of( //
					targetBundle("plugin.a", "1.0.0"), //
					targetBundle("plugin.b", "1.0.0"), //
					targetBundle("plugin.c", "1.0.0")))));
		}

		// test automatic-add-requirements enabled without optional requirements
		assertGetMergedBundleMap(wc -> {
			basicSetup.accept(wc);
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, true);
			wc.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, false);
		}, concat(requiredRPBundlesWithoutOptional, toDefaultStartData(Set.of( //
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.b", "1.0.0")))));
	}

	@Test
	public void testGetMergedBundleMap_automaticallyAddRequirements_multipleProviders() throws Exception {

		var targetPlatformBundles = Map.ofEntries( //
				bundle("plugin.a", "1.0.0", //
						entry(IMPORT_PACKAGE, "pack.a")), //

				bundle("plugin.x", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.a")), //
				bundle("plugin.y", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.a")));

		var workspacePlugins = ofEntries( //
				bundle("plugin.z", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.a")) //
				);

		Consumer<ILaunchConfigurationWorkingCopy> basicSetup = wc -> {
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, true);
		};
		{ // three providers are available and none is yet included
			// -> the one originating from the workspace is preferred
			Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
				basicSetup.accept(wc);
				wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, Set.of("plugin.a*1.0.0"));
			};
			Set<BundleLocationDescriptor> expectedBundles = Set.of( //
					targetBundle("plugin.a", "1.0.0"), //
					workspaceBundle("plugin.z", "1.0.0"));

			assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
		}
		{ // three providers are available and one is included
			// -> select included one
			Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
				basicSetup.accept(wc);
				wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES,
						Set.of("plugin.a*1.0.0", "plugin.x*1.0.0"));
			};
			Set<BundleLocationDescriptor> expectedBundles = Set.of( //
					targetBundle("plugin.a", "1.0.0"), //
					targetBundle("plugin.x", "1.0.0"));

			assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
		}
		{// three providers are available and one is included
			// -> select included one
			Consumer<ILaunchConfigurationWorkingCopy> launchConfigSetup = wc -> {
				basicSetup.accept(wc);
				wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES,
						Set.of("plugin.a*1.0.0", "plugin.y*1.0.0"));
			};
			Set<BundleLocationDescriptor> expectedBundles = Set.of( //
					targetBundle("plugin.a", "1.0.0"), //
					targetBundle("plugin.y", "1.0.0"));

			assertGetMergedBundleMap(workspacePlugins, targetPlatformBundles, launchConfigSetup, expectedBundles);
		}
	}

	@Test
	public void testTwoVersionsOfSameBundleConfigIni() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"));
		setUpWorkspace(workspacePlugins, Map.of());

		ILaunchConfigurationWorkingCopy launchConfig = createPluginLaunchConfig("testTwoVersionsOfSameBundleConfigIni");
		launchConfig.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
				Set.of("plugin.a*1.0.0", "plugin.a*2.0.0"));
		IPluginModelBase plugin1 = workspaceBundle("plugin.a", "1.0.0").findModel();
		IPluginModelBase plugin2 = workspaceBundle("plugin.a", "2.0.0").findModel();

		Path configIniFile = getConfigurationFolder(launchConfig).resolve("config.ini");
		Properties configIni = new Properties();
		try (InputStream input = Files.newInputStream(configIniFile)) {
			configIni.load(input);
		}

		String osgiBundles = configIni.getProperty("osgi.bundles");
		assertThat(osgiBundles, containsString(getInstallLocation(plugin1)));
		assertThat(osgiBundles, containsString(getInstallLocation(plugin2)));
	}

	@Test
	public void testTwoVersionsOfSameBundleBundlesInfo() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"), //
				// will trigger usage of bundes.info
				bundle(IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR, "1.0.0"));
		setUpWorkspace(workspacePlugins, Map.of());

		ILaunchConfigurationWorkingCopy launchConfig = createPluginLaunchConfig(
				"testTwoVersionsOfSameBundleBundlesInfo");
		launchConfig.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
				Set.of("plugin.a*1.0.0", "plugin.a*2.0.0", IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR + "*1.0.0"));
		IPluginModelBase plugin1 = workspaceBundle("plugin.a", "1.0.0").findModel();
		IPluginModelBase plugin2 = workspaceBundle("plugin.a", "2.0.0").findModel();

		Path bundlesInfo = getConfigurationFolder(launchConfig)
				.resolve(Path.of(IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR, "bundles.info"));
		String info = Files.readString(bundlesInfo);
		assertThat(info, containsString(getInstallLocation(plugin1)));
		assertThat(info, containsString(getInstallLocation(plugin2)));
	}

	// --- test cases for writeBundleEntry() ----

	@Test
	public void testWriteBundleEntry_singleWorkspacePlugin_noVersionEntry() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "2.0.1"));
		setUpWorkspace(workspacePlugins, targetPlatformBundles);

		IPluginModelBase plugin = workspaceBundle("plugin.a", "1.0.0").findModel();

		String entry = BundleLauncherHelper.formatBundleEntry(plugin, null, null);
		assertEquals("plugin.a", entry);
	}

	@Test
	public void testWriteBundleEntry_oneOfTwoWorkspacePlugins_versionEntry() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "1.0.1"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "2.0.0"));
		setUpWorkspace(workspacePlugins, targetPlatformBundles);

		IPluginModelBase plugin = workspaceBundle("plugin.a", "1.0.0").findModel();

		String entry = BundleLauncherHelper.formatBundleEntry(plugin, null, null);
		assertEquals("plugin.a*1.0.0", entry);
	}

	@Test
	public void testWriteBundleEntry_singleTargetPlugin_noVersionEntry() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "1.0.1"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "2.0.0"));
		setUpWorkspace(workspacePlugins, targetPlatformBundles);

		IPluginModelBase plugin = targetBundle("plugin.a", "2.0.0").findModel();

		String entry = BundleLauncherHelper.formatBundleEntry(plugin, null, null);
		assertEquals("plugin.a", entry);
	}

	@Test
	public void testWriteBundleEntry_oneOfTwoTargetPlugins_versionEntry() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"));
		var targetPlatformBundles = ofEntries( //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "2.0.1"));
		setUpWorkspace(workspacePlugins, targetPlatformBundles);

		IPluginModelBase plugin = targetBundle("plugin.a", "2.0.0").findModel();

		String entry = BundleLauncherHelper.formatBundleEntry(plugin, null, null);
		assertEquals("plugin.a*2.0.0", entry);
	}

	@Test
	public void testWriteBundleEntry_startLevelAndAutoStart() throws Exception {
		var workspacePlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "2.0.0"));
		setUpWorkspace(workspacePlugins, Map.of());

		IPluginModelBase plugin = workspaceBundle("plugin.a", "1.0.0").findModel();

		assertEquals("plugin.a*1.0.0", BundleLauncherHelper.formatBundleEntry(plugin, null, null));
		assertEquals("plugin.a*1.0.0", BundleLauncherHelper.formatBundleEntry(plugin, "", ""));
		assertEquals("plugin.a*1.0.0@4:true", BundleLauncherHelper.formatBundleEntry(plugin, "4", "true"));
		assertEquals("plugin.a*1.0.0@4:", BundleLauncherHelper.formatBundleEntry(plugin, "4", ""));
		assertEquals("plugin.a*1.0.0@:false", BundleLauncherHelper.formatBundleEntry(plugin, null, "false"));
	}

	// --- utilities ---

	private void assertGetMergedBundleMap(Map<NameVersionDescriptor, Map<String, String>> workspacePlugins,
			Map<NameVersionDescriptor, Map<String, String>> targetPlugins,
			Consumer<ILaunchConfigurationWorkingCopy> launchConfigPreparer,
			Set<BundleLocationDescriptor> expectedBundles) throws Exception {

		Map<BundleLocationDescriptor, String> expectedBundleMap = expectedBundles.stream()
				.collect(Collectors.toMap(b -> b, b -> "default:default"));
		assertGetMergedBundleMap(workspacePlugins, targetPlugins, launchConfigPreparer, expectedBundleMap);
	}

	private void assertGetMergedBundleMap(Map<NameVersionDescriptor, Map<String, String>> workspacePlugins,
			Map<NameVersionDescriptor, Map<String, String>> targetPlugins,
			Consumer<ILaunchConfigurationWorkingCopy> launchConfigPreparer,
			Map<BundleLocationDescriptor, String> expectedBundleMap) throws Exception {

		setUpWorkspace(workspacePlugins, targetPlugins);

		assertGetMergedBundleMap(launchConfigPreparer, expectedBundleMap);
	}

	private static void assertGetMergedBundleMap(Consumer<ILaunchConfigurationWorkingCopy> launchConfigPreparer,
			Map<BundleLocationDescriptor, String> expectedBundleMap) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = createPluginLaunchConfig("plugin-based-Eclipse-app");
		launchConfigPreparer.accept(wc);

		Map<IPluginModelBase, String> bundleMap = BundleLauncherHelper.getMergedBundleMap(wc, false);

		Map<IPluginModelBase, String> expectedPluginMap = new HashMap<>();
		expectedBundleMap.forEach((pd, start) -> {
			expectedPluginMap.put(pd.findModel(), start);
		});

		assertPluginMapsEquals(null, expectedPluginMap, bundleMap);
	}

	private void setUpWorkspace(Map<NameVersionDescriptor, Map<String, String>> workspacePlugins,
			Map<NameVersionDescriptor, Map<String, String>> targetPlugins) throws Exception {
		ProjectUtils.createWorkspacePluginProjects(workspacePlugins);
		TargetPlatformUtil.setDummyBundlesAsTarget(targetPlugins, List.of(), tpJarDirectory);
	}

	private static ILaunchConfigurationWorkingCopy createPluginLaunchConfig(String name) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench");
		ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, false);
		wc.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false);
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, false);
		return wc;
	}

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private Path getConfigurationFolder(ILaunchConfigurationWorkingCopy launchConfig) throws CoreException {
		ILaunch launch = new Launch(launchConfig, ILaunchManager.RUN_MODE, null);
		var config = new EclipseApplicationLaunchConfiguration();
		String commandLine = config.showCommandLine(launchConfig, ILaunchManager.RUN_MODE, launch, null);
		String configURL = WHITESPACE.splitAsStream(commandLine) //
				.dropWhile(t -> !"-configuration".equals(t)).skip(1).findFirst().get();
		return Path.of(URI.create(configURL));
	}

	private static String getInstallLocation(IPluginModelBase plugin) {
		return Path.of(plugin.getInstallLocation()).toString().replace(File.separatorChar, '/');
	}
}
