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
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.pde.internal.core.ICoreConstants.DEFAULT_VERSION;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.createPluginProject;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation;
import org.eclipse.pde.internal.ui.wizards.feature.FeatureData;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

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

		createFeatureProject(FeatureBasedLaunchTest.class.getName() + "-feature", "1.0.0", f -> {
			addIncludedPlugin(f, "javax.inject", DEFAULT_VERSION);
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
		.as("old entry without configuration has defaults").containsEntry("javax.inject", "default:default")
		.as("use configured start-levels").containsEntry("org.eclipse.core.runtime", "1:true")
		.as("ignore configured start-levels of uncheckedplugin")
		.containsEntry("org.eclipse.ui", "default:default");
	}

	// --- defined feature selection ---

	@Test
	public void testGetMergedBundleMap_featureSelectionForLocationWorkspace_latestWorkspaceFeature() throws Throwable {
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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

		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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

		List<NameVersionDescriptor> targetBundles = List.of( //
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

		List<NameVersionDescriptor> targetBundles = List.of( //
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

		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
		List<NameVersionDescriptor> targetBundles = List.of( //
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
	public void testGetMergedBundleMap_multipleInclusionOfPluginAndFeature() throws Throwable {

		List<NameVersionDescriptor> targetBundles = List.of( //
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

		List<NameVersionDescriptor> targetBundles = List.of( //
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

		List<NameVersionDescriptor> targetBundles = List.of( //
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

	// --- utility methods ---

	private static interface CoreConsumer<E> {
		void accept(E e) throws CoreException;
	}

	private static void createFeatureProject(String id, String version, CoreConsumer<IFeature> featureSetup)
			throws Throwable {
		createFeature(id, version, id + "_" + version.replace('.', '_'), featureSetup);
	}

	private static IFeature createFeature(String id, String version, String projectName,
			CoreConsumer<IFeature> featureSetup) throws Throwable {
		FeatureData featureData = new FeatureData();
		featureData.id = id;
		featureData.version = version;

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		IPath location = workspaceRoot.getLocation().append(project.getName());

		IRunnableWithProgress operation = new AbstractCreateFeatureOperation(project, location, featureData, null) {
			@Override
			protected void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException {
				featureSetup.accept(feature);
			}

			@Override
			protected void openFeatureEditor(IFile manifestFile) {
				// don't open in headless tests
			}
		};
		operation.run(new NullProgressMonitor());
		FeatureModelManager featureModelManager = PDECore.getDefault().getFeatureModelManager();
		return featureModelManager.getFeatureModel(project).getFeature();
	}

	// Created Feature-projects get reloaded shortly after their creation, in
	// the end of the auto-build job (due to some pending resource-changed
	// events). In the beginning of the reload the feature model is reset and
	// all fields become null/0. So if the model is read inbetween the model
	// state could be inconsistent. This creates a race condition, which
	// occasionally leads to test-failure. All my attempts to consume all
	// resource-change events immediately to resolve the race-condition failed.
	// I also tried to await the World-Changed event fired on a FeatureModel
	// once it was reloaded but then the test spent most of its runtime waiting.
	// Blocking all other operations was the simplest and fastest solution.
	@BeforeClass
	public static void acquireWorkspaceRuleToAvoidFeatureReloadByAutobuild() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Job.getJobManager().beginRule(root, null);
	}

	@AfterClass
	public static void releaseWorkspaceRule() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Job.getJobManager().endRule(root);
	}

	private static void addRequiredPlugin(IFeature feature, String id, String version, int matchRule)
			throws CoreException {
		addImport(feature, id, version, matchRule, IFeatureImport.PLUGIN);
	}

	private static void addRequiredFeature(IFeature feature, String id, String version, int matchRule)
			throws CoreException {
		addImport(feature, id, version, matchRule, IFeatureImport.FEATURE);
	}

	private static void addImport(IFeature feature, String id, String version, int matchRule, int type)
			throws CoreException {
		IFeatureModelFactory factory = feature.getModel().getFactory();
		IFeatureImport featureImport = factory.createImport();
		featureImport.setId(id);
		featureImport.setVersion(version);
		featureImport.setMatch(matchRule);
		featureImport.setType(type);

		feature.addImports(new IFeatureImport[] { featureImport });
	}

	private static FeatureChild addIncludedFeature(IFeature feature, String id, String version) throws CoreException {
		FeatureChild featureChild = (FeatureChild) feature.getModel().getFactory().createChild();
		featureChild.setId(id);
		featureChild.setVersion(version);
		featureChild.setOptional(false);
		feature.addIncludedFeatures(new IFeatureChild[] { featureChild });
		return featureChild;
	}

	private static IFeaturePlugin addIncludedPlugin(IFeature feature, String id, String version) throws CoreException {
		IFeaturePlugin featurePlugin = feature.getModel().getFactory().createPlugin();
		featurePlugin.setId(id);
		featurePlugin.setVersion(version);
		featurePlugin.setUnpack(false);
		feature.addPlugins(new IFeaturePlugin[] { featurePlugin });
		return featurePlugin;
	}

	private NameVersionDescriptor targetFeature(String featureId, String featureVersion,
			CoreConsumer<IFeature> featureSetup) throws Throwable {

		IFeature feature = createFeature(featureId, featureVersion, "tp-feature-temp-project", featureSetup);

		WorkspaceFeatureModel model = (WorkspaceFeatureModel) feature.getModel();
		IResource resource = model.getUnderlyingResource();

		Path featureDirectory = tpJarDirectory.resolve(Path.of("features", featureId + "_" + featureVersion));
		Files.createDirectories(featureDirectory);
		Path featureFile = featureDirectory.resolve(resource.getProjectRelativePath().toString());
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(featureFile));) {
			model.save(writer);
		}
		IProject project = resource.getProject();
		project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);

		return new NameVersionDescriptor(featureId, featureVersion, NameVersionDescriptor.TYPE_FEATURE);
	}

	private void setTargetPlatform(List<NameVersionDescriptor> targetPlugins,
			List<NameVersionDescriptor> targetFeatures) throws Exception {
		TargetPlatformUtil.setDummyBundlesAsTarget(targetPlugins, tpJarDirectory);
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

		Map<BundleLocationDescriptor, String> expectedBundleMap = expectedBundles.stream()
				.collect(Collectors.toMap(b -> b, b -> "default:default"));
		assertGetMergedBundleMap(message, launchConfig, expectedBundleMap);
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
}
