/*******************************************************************************
 *  Copyright (c) 2019, 2022 Julian Honnen and others.
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
 *     Hannes Wellmann - Bug 577116 - Improve test utility method reusability
 *     Hannes Wellmann - Bug 578005 - Extend tests to fully cover feature-based launches
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.pde.internal.core.ICoreConstants.DEFAULT_VERSION;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.addIncludedFeature;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.addIncludedPlugin;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.addRequiredFeature;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.addRequiredPlugin;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.createFeatureProject;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.createPluginProject;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.resolution;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.EclipseApplicationLaunchConfiguration;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.util.ProjectUtils.CoreConsumer;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.FrameworkUtil;

public class FeatureBasedLaunchTest extends AbstractLaunchTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Path tpJarDirectory;

	@Before
	public void setup() throws Exception {
		tpJarDirectory = folder.newFolder("TPJarDirectory").toPath();
	}

	// --- tests ---

	@Test
	public void testGetMergedBundleMap_autostartLevels() throws Throwable {
		TargetPlatformUtil.setRunningPlatformAsTarget();
		String javaxInjectProvider = FrameworkUtil.getBundle(jakarta.inject.Inject.class).getSymbolicName();
		createFeatureProject(FeatureBasedLaunchTest.class.getName() + "-feature", "1.0.0", f -> {
			addIncludedPlugin(f, javaxInjectProvider, DEFAULT_VERSION);
			addIncludedPlugin(f, "org.eclipse.core.runtime", DEFAULT_VERSION);
			addIncludedPlugin(f, "org.eclipse.ui", DEFAULT_VERSION);
		});
		ILaunchConfiguration lc = getLaunchConfiguration("feature-based-with-startlevels.launch");

		Map<IPluginModelBase, String> bundleMap = BundleLauncherHelper.getMergedBundleMap(lc, false);

		Map<String, String> byId = new LinkedHashMap<>();
		for (Entry<IPluginModelBase, String> entry : bundleMap.entrySet()) {
			byId.put(entry.getKey().getPluginBase().getId(), entry.getValue());
		}

		assertThat(byId)//
		.as("old entry without config has defaults").containsEntry(javaxInjectProvider, "default:default")
		.as("use configured start-levels").containsEntry("org.eclipse.core.runtime", "1:true")
		.as("ignore configured start-levels of unchecked plugin")
		.containsEntry("org.eclipse.ui", "default:default");
	}

	// --- defined feature selection ---

	@Test
	public void testGetMergedBundleMap_featureSelectionForLocationWorkspace_latestWorkspaceFeature() throws Throwable {
		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"));

		createFeatureProject("feature.a", "2.0.0", f -> {
			addIncludedPlugin(f, "plugin.a", "1.0.0");
		});
		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.b", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "2.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}), //
				targetFeature("feature.z", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.d", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));
		wc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

		assertGetMergedBundleMap(wc, Set.of( //
				targetBundle("plugin.a", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_featureSelectionForLocationWorkspaceButNoWorkspaceFeaturePresent_latestExternalFeature()
			throws Throwable {
		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "2.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
				}), //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.b", "1.0.0");
				}), //
				targetFeature("feature.z", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));
		wc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

		assertGetMergedBundleMap(wc, Set.of( //
				targetBundle("plugin.a", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_featureSelectionForLocationExternal_latestExternalFeature() throws Throwable {
		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"));

		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.a", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "2.0.0", f -> {
					addIncludedPlugin(f, "plugin.b", "1.0.0");
				}), //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}), //
				targetFeature("feature.z", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.d", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));
		wc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

		assertGetMergedBundleMap(wc, Set.of( //
				targetBundle("plugin.b", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_featureSelectionForLocationExternalButNoExternalFeaturePresent_noFeature()
			throws Throwable {
		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"));

		createFeatureProject("feature.a", "2.0.0", f -> {
			addIncludedPlugin(f, "plugin.a", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of();

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));
		wc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

		assertGetMergedBundleMap(wc, emptySet());
	}

	// --- included plug-ins ---

	@Test
	public void testGetMergedBundleMap_includedPluginWithDefaultVersion() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.a", "1.0.1");
		createPluginProject("plugin.b", "1.0.0");

		var targetBundles = ofEntries( //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "2.0.1"), //
				bundle("plugin.c", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", DEFAULT_VERSION);
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.b", DEFAULT_VERSION);
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", DEFAULT_VERSION);
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:workspace"));

			assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.0.1")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));

			assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
					targetBundle("plugin.a", "2.0.1")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));

			assertGetMergedBundleMap("pluginResolution default workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.0.1")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));

			assertGetMergedBundleMap("pluginResolution default external", lc, Set.of( //
					targetBundle("plugin.a", "2.0.1")));
		}
		// Plug-ins only in secondary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:external"));

			assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
					workspaceBundle("plugin.b", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:workspace"));

			assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
					targetBundle("plugin.c", "1.0.0")));
		}
	}

	@Test
	public void testGetMergedBundleMap_includedPluginWithSpecificVersion() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.a", "1.2.0");
		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "1.1.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "2.0.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.1.0");
				}), //
				targetFeature("feature.d", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.2.0");
				}), //
				targetFeature("feature.e", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0.someQualifier");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// Perfect version-match in primary location (while secondary location
		// has matches too)
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:workspace"));

			assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));

			assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
					targetBundle("plugin.a", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));

			assertGetMergedBundleMap("pluginResolution default workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));

			assertGetMergedBundleMap("pluginResolution default external", lc, Set.of( //
					targetBundle("plugin.a", "1.0.0")));
		}
		// Unqualified version-match in primary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.e:workspace"));

			assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.e:external"));

			assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
					targetBundle("plugin.a", "1.0.0")));
		}
		// no version-match at all (for included plug-ins the latest plug-in of
		// a location is added if there is no match in the primary location)
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:workspace"));

			assertGetMergedBundleMap("pluginResolution workspace no match", lc, Set.of( //
					workspaceBundle("plugin.a", "1.2.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:external"));

			assertGetMergedBundleMap("pluginResolution external no match", lc, Set.of( //
					targetBundle("plugin.a", "1.1.0")));
		}
		// Perfect version match only in secondary location (but another version
		// is present in the primary too).
		// To be able to conveniently override a specific version of a plug-in,
		// which is actually included by a (transitive) feature from the TP, by
		// a plug-in from the workspace (which happens frequently when
		// one has just updated the plug-in version) the latest plug-in from the
		// primary location is taken if one is present and none matches the
		// specified version exactly (with or without considering qualifiers).
		// See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=576887
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:workspace"));

			assertGetMergedBundleMap("pluginResolution workspace but match is external", lc, Set.of( //
					workspaceBundle("plugin.a", "1.2.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.d:external"));

			assertGetMergedBundleMap("pluginResolution external but match is in workspace", lc, Set.of( //
					targetBundle("plugin.a", "1.1.0")));
		}
	}

	// --- included features ---

	@Test
	public void testGetMergedBundleMap_includedFeatureWithDefaultVersion() throws Throwable {
		// plug-in names contain version-like suffixes to have no chance to
		// interfere with conveniences of plug-in resolution
		var targetBundles = ofEntries( //
				bundle("plugin.a.e.100", "1.0.0"), //
				bundle("plugin.a.w.101", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		createFeatureProject("feature.a", "1.0.3", f -> {
			addIncludedPlugin(f, "plugin.a.w.101", "1.0.0");
		});
		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.z", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.z", "1.0.0", f -> {
					addIncludedFeature(f, "feature.a", DEFAULT_VERSION);
				}), //

				targetFeature("feature.a", "1.0.2", f -> {
					addIncludedPlugin(f, "plugin.a.e.100", "1.0.0");
				}), //
				targetFeature("feature.a", "1.0.1", f -> {
					addIncludedPlugin(f, "plugin.z", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		{
			ILaunchConfigurationWorkingCopy lcW = createFeatureLaunchConfig();
			lcW.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.z:default"));
			lcW.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

			assertGetMergedBundleMap("feature-location workspace", lcW, Set.of( //
					targetBundle("plugin.a.w.101", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lcE = createFeatureLaunchConfig();
			lcE.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.z:default"));
			lcE.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

			assertGetMergedBundleMap("feature-location external", lcE, Set.of( //
					targetBundle("plugin.a.e.100", "1.0.0")));
		}
	}

	@Test
	public void testGetMergedBundleMap_includedFeatureWithSpecificVersion() throws Throwable {
		// plug-in names contain version-like suffixes to have no chance to
		// interfere with conveniences of plug-in resolution
		var targetBundles = ofEntries( //
				bundle("plugin.a.w.100", "1.0.0"), //
				bundle("plugin.a.w.300", "1.0.0"), //
				bundle("plugin.a.e.100", "1.0.0"), //
				bundle("plugin.a.e.200", "1.0.0"), //
				bundle("plugin.a.e.400", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.100", "1.0.0");
		});
		createFeatureProject("feature.a", "3.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.300", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.100", "1.0.0");
				}), //
				targetFeature("feature.a", "2.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.200", "1.0.0");
				}), //
				targetFeature("feature.a", "4.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.400", "1.0.0");
				}), //

				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedFeature(f, "feature.a", "1.0.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedFeature(f, "feature.a", "1.2.0");
				}), //
				targetFeature("feature.d", "1.0.0", f -> {
					addIncludedFeature(f, "feature.a", "2.0.0");
				}), //
				targetFeature("feature.e", "1.0.0", f -> {
					addIncludedFeature(f, "feature.a", "3.0.0");
				}), //
				targetFeature("feature.f", "1.0.0", f -> {
					addIncludedFeature(f, "feature.a", "1.0.0.someQualifier");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// Perfect version-match in primary location (while secondary location
		// has matches too)
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

			assertGetMergedBundleMap("feature-location workspace", lc, Set.of( //
					targetBundle("plugin.a.w.100", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

			assertGetMergedBundleMap("feature-location external", lc, Set.of( //
					targetBundle("plugin.a.e.100", "1.0.0")));
		}
		// Unqualified version-match in primary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.f:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

			assertGetMergedBundleMap("feature-location workspace", lc, Set.of( //
					targetBundle("plugin.a.w.100", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.f:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

			assertGetMergedBundleMap("feature-location external", lc, Set.of( //
					targetBundle("plugin.a.e.100", "1.0.0")));
		}
		// no version-match at all (for included features the latest features of
		// a location is added if there is no match in the primary location)
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

			assertGetMergedBundleMap("feature-location workspace no match", lc, Set.of( //
					targetBundle("plugin.a.w.300", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

			assertGetMergedBundleMap("feature-location external no match", lc, Set.of( //
					targetBundle("plugin.a.e.400", "1.0.0")));
		}
		// Perfect version match only in secondary location (but another version
		// is present in the primary too).
		// To be able to conveniently override a specific version of a feature,
		// which is actually included (transitively) by a feature from the TP,
		// by a feature from the workspace (which happens frequently when
		// one has just updated the feature version) the latest feature from the
		// primary location is taken if one is present and none matches the
		// specified version exactly (with or without considering qualifiers).
		// See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=576887
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.d:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

			assertGetMergedBundleMap("feature-location workspace but exact match is external", lc, Set.of( //
					targetBundle("plugin.a.w.300", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.e:default"));
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);

			assertGetMergedBundleMap("feature-location external but exact match is in workspace", lc, Set.of(//
					targetBundle("plugin.a.e.400", "1.0.0")));
		}
	}

	// --- required/imported plug-in dependencies ---

	@Test
	public void testGetMergedBundleMap_requiredPluginWithNoVersion() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.a", "1.1.0");
		createPluginProject("plugin.b", "1.0.0");

		var targetBundles = ofEntries( //
				bundle("plugin.a", "2.0.0"), //
				bundle("plugin.a", "2.1.0"), //
				bundle("plugin.c", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", null, IMatchRules.NONE);
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.b", null, IMatchRules.NONE);
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.c", null, IMatchRules.NONE);
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// explicit pluginResolution location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:workspace"));

			assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.1.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));

			assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
					targetBundle("plugin.a", "2.1.0")));
		}
		// default pluginResolution location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));

			assertGetMergedBundleMap("pluginResolution default workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.1.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:default"));

			assertGetMergedBundleMap("pluginResolution default external", lc, Set.of( //
					targetBundle("plugin.a", "2.1.0")));
		}
		// Plug-ins only in secondary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:external"));

			assertGetMergedBundleMap("pluginResolution external but match in workspace", lc, Set.of( //
					workspaceBundle("plugin.b", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:workspace"));

			assertGetMergedBundleMap("pluginResolution workspace but match is external", lc, Set.of( //
					targetBundle("plugin.c", "1.0.0")));
		}
	}

	@Test
	public void testGetMergedBundleMap_requiredPluginWithSpecificVersion1() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.a", "1.1.2");
		createPluginProject("plugin.a", "2.0.0");

		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "1.2.3"), //
				bundle("plugin.a", "3.0.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", "1.0.0", IMatchRules.NONE);
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", "1.0.0", IMatchRules.COMPATIBLE);
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// MatchRule NONE/COMPATIBLE (behave the same according to VersionUtil)
		// and location resolution tests.
		// explicit pluginResolution location
		{
			for (String feature : List.of("feature.a", "feature.b")) {
				ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
				lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of(feature + ":workspace"));

				assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
						workspaceBundle("plugin.a", "1.1.2")));
			}
		}
		{
			for (String feature : List.of("feature.a", "feature.b")) {
				ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
				lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of(feature + ":external"));

				assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
						targetBundle("plugin.a", "1.2.3")));
			}
		}
		// default pluginResolution location
		{
			for (String feature : List.of("feature.a", "feature.b")) {
				ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
				lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION,
						IPDELauncherConstants.LOCATION_WORKSPACE);
				lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of(feature + ":default"));

				assertGetMergedBundleMap("pluginResolution default workspace", lc, Set.of( //
						workspaceBundle("plugin.a", "1.1.2")));
			}
		}
		{
			for (String feature : List.of("feature.a", "feature.b")) {
				ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
				lc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION,
						IPDELauncherConstants.LOCATION_EXTERNAL);
				lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of(feature + ":default"));

				assertGetMergedBundleMap("pluginResolution default external", lc, Set.of( //
						targetBundle("plugin.a", "1.2.3")));
			}
		}
	}

	@Test
	public void testGetMergedBundleMap_requiredPluginWithSpecificVersion2() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.a", "1.0.1");
		createPluginProject("plugin.a", "1.1.0");
		createPluginProject("plugin.a", "1.1.2");
		createPluginProject("plugin.a", "2.0.0");

		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.a", "1.0.2"), //
				bundle("plugin.a", "1.2.0"), //
				bundle("plugin.a", "1.2.3"), //
				bundle("plugin.a", "3.0.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.c", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", "1.0.0", IMatchRules.EQUIVALENT);
				}), //
				targetFeature("feature.d", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", "1.1.0", IMatchRules.EQUIVALENT);
				}), //
				targetFeature("feature.e", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", "1.2.0", IMatchRules.EQUIVALENT);
				}), //
				targetFeature("feature.f", "1.0.0", f -> {
					addRequiredPlugin(f, "plugin.a", "8.0.0", IMatchRules.EQUIVALENT);
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// No need to test all match-rules. Just have to check that version
		// match rules are obeyed. For the following cases match-rule EQUIVALENT
		// is used to check different per-location-availability scenarios.

		// match in primary location (while secondary location has matches too)
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:workspace"));

			assertGetMergedBundleMap("pluginResolution workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.0.1")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:external"));

			assertGetMergedBundleMap("pluginResolution external", lc, Set.of( //
					targetBundle("plugin.a", "1.0.2")));
		}
		// match only in secondary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.d:external"));

			assertGetMergedBundleMap("pluginResolution external but match in workspace", lc, Set.of( //
					workspaceBundle("plugin.a", "1.1.2")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.e:workspace"));

			assertGetMergedBundleMap("pluginResolution workspace but match is external", lc, Set.of( //
					targetBundle("plugin.a", "1.2.3")));
		}
		// no match at all
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.f:external"));

			assertGetMergedBundleMap("pluginResolution external no match", lc, Set.of());
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.f:workspace"));

			assertGetMergedBundleMap("pluginResolution workspace no match", lc, Set.of());
		}
	}

	// --- required/imported feature dependencies ---

	@Test
	public void testGetMergedBundleMap_requiredFeatureWithNoVersion() throws Throwable {
		// plug-in names contain version-like suffixes to have no chance to
		// interfere with conveniences of plug-in resolution
		var targetBundles = ofEntries( //
				bundle("plugin.a.w.100", "1.0.0"), //
				bundle("plugin.a.w.110", "1.0.0"), //
				bundle("plugin.a.e.200", "1.0.0"), //
				bundle("plugin.a.e.210", "1.0.0"), //
				bundle("plugin.b.w.100", "1.0.0"), //
				bundle("plugin.c.e.100", "1.0.0"), //
				bundle("plugin.xyz", "1.0.0"));

		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.100", "1.0.0");
		});
		createFeatureProject("feature.a", "1.1.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.110", "1.0.0");
		});
		createFeatureProject("feature.b", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.b.w.100", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.z", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", null, IMatchRules.NONE);
				}), //
				targetFeature("feature.y", "1.0.0", f -> {
					addRequiredFeature(f, "feature.b", null, IMatchRules.NONE);
				}), //
				targetFeature("feature.x", "1.0.0", f -> {
					addRequiredFeature(f, "feature.c", null, IMatchRules.NONE);
				}), //

				targetFeature("feature.a", "2.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.200", "1.0.0");
				}), //
				targetFeature("feature.a", "2.1.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.210", "1.0.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c.e.100", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.z:default"));

			assertGetMergedBundleMap("featureResolution explicit workspace", lc, Set.of( //
					targetBundle("plugin.a.w.110", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.z:default"));

			assertGetMergedBundleMap("featureResolution explicit external", lc, Set.of( //
					targetBundle("plugin.a.e.210", "1.0.0")));
		}
		// Features only in secondary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.y:default"));

			assertGetMergedBundleMap("featureResolution external but match in workspace", lc, Set.of());
			// if featureResolution is 'external', workspace features are not
			// considered at all
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.x:default"));

			assertGetMergedBundleMap("featureResolution workspace but match is external", lc, Set.of( //
					targetBundle("plugin.c.e.100", "1.0.0")));
		}
	}

	@Test
	public void testGetMergedBundleMap_requiredFeatureWithSpecificVersion1() throws Throwable {
		// plug-in names contain version-like suffixes to have no chance to
		// interfere with conveniences of plug-in resolution
		var targetBundles = ofEntries( //
				bundle("plugin.a.w.100", "1.0.0"), //
				bundle("plugin.a.w.110", "1.0.0"), //
				bundle("plugin.a.w.200", "1.0.0"), //
				bundle("plugin.a.e.100", "1.0.0"), //
				bundle("plugin.a.e.123", "1.0.0"), //
				bundle("plugin.a.e.300", "1.0.0"), //
				bundle("plugin.xyz", "1.0.0"));

		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.100", "1.0.0");
		});
		createFeatureProject("feature.a", "1.1.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.110", "1.0.0");
		});
		createFeatureProject("feature.a", "2.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.200", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.z", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", "1.0.0", IMatchRules.NONE);
				}), //
				targetFeature("feature.y", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", "1.0.0", IMatchRules.COMPATIBLE);
				}), //

				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.100", "1.0.0");
				}), //
				targetFeature("feature.a", "1.2.3", f -> {
					addIncludedPlugin(f, "plugin.a.e.123", "1.0.0");
				}), //
				targetFeature("feature.a", "3.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.300", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// MatchRule NONE/COMPATIBLE (behave the same according to VersionUtil)
		// and location resolution tests.
		// explicit pluginResolution location
		{
			for (String feature : List.of("feature.z", "feature.y")) {
				ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
				lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION,
						IPDELauncherConstants.LOCATION_WORKSPACE);
				lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of(feature + ":default"));

				assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
						targetBundle("plugin.a.w.110", "1.0.0")));
			}
		}
		{
			for (String feature : List.of("feature.z", "feature.y")) {
				ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
				lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION,
						IPDELauncherConstants.LOCATION_EXTERNAL);
				lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of(feature + ":default"));

				assertGetMergedBundleMap("pluginResolution explicit external", lc, Set.of( //
						targetBundle("plugin.a.e.123", "1.0.0")));
			}
		}
	}

	@Test
	public void testGetMergedBundleMap_requiredFeatureWithSpecificVersion2() throws Throwable {
		var targetBundles = ofEntries( //
				bundle("plugin.a.w.100", "1.0.0"), //
				bundle("plugin.a.w.101", "1.0.0"), //
				bundle("plugin.a.w.110", "1.0.0"), //
				bundle("plugin.a.w.112", "1.0.0"), //
				bundle("plugin.a.w.200", "1.0.0"), //
				bundle("plugin.a.e.100", "1.0.0"), //
				bundle("plugin.a.e.102", "1.0.0"), //
				bundle("plugin.a.e.120", "1.0.0"), //
				bundle("plugin.a.e.123", "1.0.0"), //
				bundle("plugin.a.e.300", "1.0.0"), //
				bundle("plugin.xyz", "1.0.0"));

		createFeatureProject("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.100", "1.0.0");
		});
		createFeatureProject("feature.a", "1.0.1", f -> {
			addIncludedPlugin(f, "plugin.a.w.101", "1.0.0");
		});
		createFeatureProject("feature.a", "1.1.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.110", "1.0.0");
		});
		createFeatureProject("feature.a", "1.1.2", f -> {
			addIncludedPlugin(f, "plugin.a.w.112", "1.0.0");
		});
		createFeatureProject("feature.a", "2.0.0", f -> {
			addIncludedPlugin(f, "plugin.a.w.200", "1.0.0");
		});

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.z", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", "1.0.0", IMatchRules.EQUIVALENT);
				}), //
				targetFeature("feature.y", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", "1.1.0", IMatchRules.EQUIVALENT);
				}), //
				targetFeature("feature.x", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", "1.2.0", IMatchRules.EQUIVALENT);
				}), //
				targetFeature("feature.w", "1.0.0", f -> {
					addRequiredFeature(f, "feature.a", "8.0.0", IMatchRules.EQUIVALENT);
				}), //

				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.100", "1.0.0");
				}), //
				targetFeature("feature.a", "1.0.2", f -> {
					addIncludedPlugin(f, "plugin.a.e.102", "1.0.0");
				}), //
				targetFeature("feature.a", "1.2.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.120", "1.0.0");
				}), //
				targetFeature("feature.a", "1.2.3", f -> {
					addIncludedPlugin(f, "plugin.a.e.123", "1.0.0");
				}), //
				targetFeature("feature.a", "3.0.0", f -> {
					addIncludedPlugin(f, "plugin.a.e.300", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		// No need to test all match-rules. Just have to check that version
		// match rules are obeyed. For the following cases match-rule EQUIVALENT
		// is used to check different per-location-availability scenarios.

		// match in primary location (while secondary location has matches too)
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.z:default"));

			assertGetMergedBundleMap("featureResolution workspace", lc, Set.of( //
					targetBundle("plugin.a.w.101", "1.0.0")));
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.z:default"));

			assertGetMergedBundleMap("featureResolution external", lc, Set.of( //
					targetBundle("plugin.a.e.102", "1.0.0")));
		}
		// match only in secondary location
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.y:default"));

			assertGetMergedBundleMap("featureResolution external but match in workspace", lc, Set.of());
			// if featureResolution is 'external', workspace features are not
			// considered at all
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.x:default"));

			assertGetMergedBundleMap("featureResolution workspace but match is external", lc, Set.of( //
					targetBundle("plugin.a.e.123", "1.0.0")));
		}
		// no match at all
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_EXTERNAL);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.w:default"));

			assertGetMergedBundleMap("featureResolution external no match", lc, Set.of());
		}
		{
			ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
			lc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
			lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.w:default"));

			assertGetMergedBundleMap("featureResolution workspace no match", lc, Set.of());
		}
	}

	// --- miscellaneous cases ---

	@Test
	public void testGetMergedBundleMap_includedPluginAndFeatureEnvironmentNotMatchingTargetEnvironment()
			throws Throwable {
		String thisOS = Platform.getOS();
		String otherOS = thisOS.equals(Platform.OS_LINUX) ? Platform.OS_WIN32 : Platform.OS_LINUX;

		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}), //
				targetFeature("feature.d", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.d", "1.0.0");
				}), //

				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0").setOS(thisOS);
					addIncludedPlugin(f, "plugin.b", "1.0.0").setOS(otherOS);

					addIncludedFeature(f, "feature.c", "1.0.0").setOS(thisOS);
					addIncludedFeature(f, "feature.d", "1.0.0").setOS(otherOS);
				}));

		setTargetPlatform(targetBundles, targetFeatures);
		// No need to test all aspects of environment matches, just test if
		// environment is considered.

		ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
		lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));

		assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.c", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_featureEnvironmentNotMatchingTargetEnvironment() throws Throwable {
		String thisOS = Platform.getOS();
		String otherOS = thisOS.equals(Platform.OS_LINUX) ? Platform.OS_WIN32 : Platform.OS_LINUX;

		var targetBundles = ofEntries( //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.b", "1.1.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.c", "1.1.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.b", "1.0.0", f -> {
					f.setOS(thisOS);
					addIncludedPlugin(f, "plugin.b", "1.0.0");
				}), //
				targetFeature("feature.b", "1.1.0", f -> {
					f.setOS(otherOS);
					addIncludedPlugin(f, "plugin.b", "1.1.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					f.setOS(thisOS);
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}), //
				targetFeature("feature.c", "1.1.0", f -> {
					f.setOS(otherOS);
					addIncludedPlugin(f, "plugin.c", "1.1.0");
				}), //

				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedFeature(f, "feature.b", DEFAULT_VERSION);
					addRequiredFeature(f, "feature.c", "1.0.0", IMatchRules.COMPATIBLE);
				}));

		setTargetPlatform(targetBundles, targetFeatures);
		// No need to test all aspects of environment matches, just test if
		// environment is considered.

		ILaunchConfigurationWorkingCopy lc = createFeatureLaunchConfig();
		lc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));

		assertGetMergedBundleMap("pluginResolution explicit workspace", lc, Set.of( //
				targetBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.c", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_multipleInclusionOfPluginAndFeature() throws Throwable {

		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
					addRequiredPlugin(f, "plugin.a", "1.0.0", IMatchRules.PERFECT);
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
					addRequiredPlugin(f, "plugin.a", "1.0.0", IMatchRules.PERFECT);
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedFeature(f, "feature.z", "1.0.0");
					addRequiredFeature(f, "feature.z", "1.0.0", IMatchRules.PERFECT);
				}), //
				targetFeature("feature.d", "1.0.0", f -> {
					addIncludedFeature(f, "feature.z", "1.0.0");
					addRequiredFeature(f, "feature.z", "1.0.0", IMatchRules.PERFECT);
				}), //

				targetFeature("feature.z", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.z", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of( //
				"feature.a:default", //
				"feature.b:default", //
				"feature.c:default", //
				"feature.d:default"));

		assertGetMergedBundleMap(wc, Set.of( //
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.z", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_additionalPlugins() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.b", "1.0.0");
		createPluginProject("plugin.d", "1.0.0");
		createPluginProject("plugin.e", "1.0.0");

		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"), //
				bundle("plugin.e", "1.0.0"), //
				bundle("plugin.f", "2.0.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.d", "1.0.0");
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.e", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of( //
				"feature.a:external", //
				"feature.b:workspace"));
		wc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_EXTERNAL);
		wc.setAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, Set.of( //
				// id:version:location:enabled:startLeval:autoStart
				"plugin.a:1.0.0:default:true:1:true", //
				"plugin.b:1.0.0:workspace:true:2:true", //
				"plugin.c:1.0.0:workspace:true:3:true", //
				"plugin.d:1.0.0:external:true:4:true", // overwrite from feature
				"plugin.e:1.0.0:external:true:5:true", // attempted overwrite
				"plugin.f:1.0.0:default:true:6:true", // not matching version
				"plugin.z:1.0.0:external:false:7:true") // disabled
				);
		// overwriting the plug-in also included by a feature only works
		// if the same primary location is used and both pull in the
		// same version. Otherwise two different bundles are added

		assertGetMergedBundleMap(wc, Map.of( //
				targetBundle("plugin.a", "1.0.0"), "1:true", //
				workspaceBundle("plugin.b", "1.0.0"), "2:true", //
				targetBundle("plugin.c", "1.0.0"), "3:true", //
				targetBundle("plugin.d", "1.0.0"), "4:true", //
				targetBundle("plugin.e", "1.0.0"), "5:true", //
				workspaceBundle("plugin.e", "1.0.0"), "default:default", //
				targetBundle("plugin.f", "2.0.0"), "6:true"));
	}

	@Test
	public void testGetMergedBundleMap_inheritanceOfPluginResolution() throws Throwable {
		createPluginProject("plugin.a", "1.0.0");
		createPluginProject("plugin.b", "1.0.0");
		createPluginProject("plugin.d", "1.0.0");
		createPluginProject("plugin.e", "1.0.0");

		var targetBundles = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"), //
				bundle("plugin.e", "1.0.0"), //
				bundle("plugin.z", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedFeature(f, "feature.b", "1.0.0");
					addRequiredFeature(f, "feature.c", "", IMatchRules.COMPATIBLE);
					addIncludedFeature(f, "feature.d", "1.0.0");
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.b", "1.0.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}), //
				targetFeature("feature.d", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.d", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of( //
				"feature.a:external", //
				"feature.d:workspace"));

		assertGetMergedBundleMap(wc, Set.of( //
				targetBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.c", "1.0.0"), //
				workspaceBundle("plugin.d", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_automaticallyAddRequirements() throws Throwable {

		var targetBundles = Map.ofEntries( //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //

				bundle("plugin.z", "1.0.0", //
						entry(REQUIRE_BUNDLE, "plugin.x,plugin.w" + resolution(RESOLUTION_OPTIONAL))),
				bundle("plugin.y", "1.0.0"), //
				bundle("plugin.x", "1.0.0"), //
				bundle("plugin.w", "1.0.0"));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedFeature(f, "feature.b", "1.0.0");
					addRequiredFeature(f, "feature.c", "", IMatchRules.COMPATIBLE);
					addIncludedPlugin(f, "plugin.z", "1.0.0");
					addRequiredPlugin(f, "plugin.y", "", IMatchRules.COMPATIBLE);
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.b", "1.0.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.c", "1.0.0");
				}));

		// Gather requirements of the product used below
		Map<BundleLocationDescriptor, String> requiredRPBundles = getEclipseAppRequirementClosureForRunningPlatform();

		TargetPlatformUtil.setRunningPlatformWithDummyBundlesAsTarget(null, targetBundles, targetFeatures,
				tpJarDirectory);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));
		wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
		wc.setAttribute(IPDELauncherConstants.PRODUCT, "org.eclipse.platform.ide");

		// test AUTOMATIC_ADD_REQUIREMENTS=true and its default (true)
		for (Boolean autoAddRequirements : Arrays.asList(true, null)) {
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, autoAddRequirements);
			assertGetMergedBundleMap(wc, concat(requiredRPBundles, toDefaultStartData(Set.of(//
					targetBundle("plugin.b", "1.0.0"), //
					targetBundle("plugin.c", "1.0.0"), //
					targetBundle("plugin.z", "1.0.0"), //
					targetBundle("plugin.y", "1.0.0"), //
					targetBundle("plugin.x", "1.0.0")))));
		}

		// test AUTOMATIC_ADD_REQUIREMENTS=false
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, false);
		assertGetMergedBundleMap(wc, Set.of( //
				targetBundle("plugin.b", "1.0.0"), //
				targetBundle("plugin.z", "1.0.0")));
	}

	@Test
	public void testGetMergedBundleMap_automaticallyAddRequirements_multipleProviders() throws Throwable {
		// Test that, in case multiple-providers of a capability exists, the one
		// explicitly included into the launch is preferred, when adding
		// required dependencies

		var targetBundles = Map.ofEntries( //
				bundle("plugin.a", "1.0.0", //
						entry(IMPORT_PACKAGE, "pack.a")), //

				bundle("plugin.x", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.a")), //
				bundle("plugin.y", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.a")));

		createPluginProject("plugin.z", "1.0.0", Map.ofEntries(//
				entry(EXPORT_PACKAGE, "pack.a")));

		List<NameVersionDescriptor> targetFeatures = List.of( //
				targetFeature("feature.a", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
					addIncludedPlugin(f, "plugin.x", "1.0.0");
				}), //
				targetFeature("feature.b", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
					addIncludedPlugin(f, "plugin.y", "1.0.0");
				}), //
				targetFeature("feature.c", "1.0.0", f -> {
					addIncludedPlugin(f, "plugin.a", "1.0.0");
				}));

		setTargetPlatform(targetBundles, targetFeatures);

		ILaunchConfigurationWorkingCopy wc = createFeatureLaunchConfig();
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, true);
		wc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE); // default

		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.a:external"));
		assertGetMergedBundleMap(wc, Set.of(//
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.x", "1.0.0")));

		// The second feature included the other provider, so it's expected that
		// only that one is included
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.b:external"));
		assertGetMergedBundleMap(wc, Set.of(//
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.y", "1.0.0")));

		// If FEATURE_PLUGIN_RESOLUTION=LOCATION_WORKSPACE, bundles originating
		// from the workspace are to prefer:
		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, Set.of("feature.c:external"));
		assertGetMergedBundleMap(wc, Set.of(//
				targetBundle("plugin.a", "1.0.0"), //
				workspaceBundle("plugin.z", "1.0.0")));

		// If FEATURE_PLUGIN_RESOLUTION=LOCATION_EXTERNAL, bundles originating
		// from the TP are to prefer. And without other distinction the bundle
		// with the higher version and then 'lower' id is preferred.
		wc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_EXTERNAL);
		assertGetMergedBundleMap(wc, Set.of(//
				targetBundle("plugin.a", "1.0.0"), //
				targetBundle("plugin.x", "1.0.0")));
	}

	static Map<BundleLocationDescriptor, String> getEclipseAppRequirementClosureForRunningPlatform(
			DependencyManager.Options... closureOptions) throws Exception {
		// ensure app requirements are registered (done at class initialization)
		new EclipseApplicationLaunchConfiguration();
		@SuppressWarnings("unused") // prevent bundle removal
		org.eclipse.platform.internal.LaunchUpdateIntroAction a;
		@SuppressWarnings("unused") // prevent bundle removal
		org.eclipse.ui.internal.ide.application.IDEApplication app;

		TargetPlatformUtil.setRunningPlatformAsTarget();

		List<String> productPlugins = List.of("org.eclipse.platform", "org.eclipse.ui.ide.application");
		Set<BundleDescription> appBundles = productPlugins.stream().map(PluginRegistry::findModel)
				.map(IPluginModelBase::getBundleDescription).collect(Collectors.toSet());

		Set<BundleDescription> appBundleClosure = DependencyManager.findRequirementsClosure(appBundles, closureOptions);
		assertThat(appBundleClosure).hasSizeGreaterThanOrEqualTo(productPlugins.size());
		return DependencyManager.findRequirementsClosure(appBundles, closureOptions).stream().collect(Collectors.toMap( //
				d -> targetBundle(d.getSymbolicName(), d.getVersion().toString()),
				d -> BundleLauncherHelper.getStartData(d, "default:default")));
	}

	// --- utility methods ---

	private NameVersionDescriptor targetFeature(String featureId, String featureVersion,
			CoreConsumer<IFeature> featureSetup) throws Throwable {
		return TargetPlatformUtil.targetFeature(featureId, featureVersion, featureSetup, tpJarDirectory);
	}

	private void setTargetPlatform(Map<NameVersionDescriptor, Map<String, String>> bundleDescriptions,
			List<NameVersionDescriptor> targetFeatures) throws Exception {
		TargetPlatformUtil.setDummyBundlesAsTarget(bundleDescriptions, targetFeatures, tpJarDirectory);
	}

	private static ILaunchConfigurationWorkingCopy createFeatureLaunchConfig() throws CoreException {
		String name = "feature-based-Eclipse-app";
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench");
		ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);
		wc.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, true);
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, false);
		wc.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
		wc.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
		return wc;
	}

	private static void assertGetMergedBundleMap(ILaunchConfiguration launchConfig,
			Set<BundleLocationDescriptor> expectedBundles) throws Exception {
		assertGetMergedBundleMap(null, launchConfig, expectedBundles);
	}

	private static void assertGetMergedBundleMap(String message, ILaunchConfiguration launchConfig,
			Set<BundleLocationDescriptor> expectedBundles) throws Exception {
		assertGetMergedBundleMap(message, launchConfig, toDefaultStartData(expectedBundles));
	}

	static Map<BundleLocationDescriptor, String> toDefaultStartData(Collection<BundleLocationDescriptor> bundles) {
		return bundles.stream().collect(Collectors.toMap(b -> b, b -> "default:default"));
	}

	private static void assertGetMergedBundleMap(ILaunchConfiguration launchConfig,
			Map<BundleLocationDescriptor, String> expectedBundleMap) throws Exception {
		assertGetMergedBundleMap(null, launchConfig, expectedBundleMap);
	}

	private static void assertGetMergedBundleMap(String message, ILaunchConfiguration launchConfig,
			Map<BundleLocationDescriptor, String> expectedBundleMap) throws Exception {

		Map<IPluginModelBase, String> bundleMap = BundleLauncherHelper.getMergedBundleMap(launchConfig, false);

		Map<IPluginModelBase, String> expectedPluginMap = new HashMap<>();
		expectedBundleMap.forEach((pd, start) -> {
			expectedPluginMap.put(pd.findModel(), start);
		});

		assertPluginMapsEquals(message, expectedPluginMap, bundleMap);
	}

	@SafeVarargs
	static <K, V> Map<K, V> concat(Map<K, V>... maps) {
		Map<K, V> map = new HashMap<>();
		Arrays.stream(maps).forEach(map::putAll);
		return map;
	}
}
