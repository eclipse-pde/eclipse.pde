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
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 *     Hannes Wellmann - Bug 577385: Add tests for Plug-in based Eclipse-App launches
 *     Hannes Wellmann - Bug 544838 - Unify and generalize test bundle/feature creation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.util;

import static java.util.Map.entry;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils.CoreConsumer;
import org.junit.rules.TestRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

public class TargetPlatformUtil {

	public static final ITargetPlatformService TPS = PDECore.getDefault().acquireService(ITargetPlatformService.class);
	public static final TestRule RESTORE_CURRENT_TARGET_DEFINITION_AFTER = TestUtils.getThrowingTestRule(
			TPS::getWorkspaceTargetDefinition, //
			beforeTarget -> {
				if (beforeTarget != TPS.getWorkspaceTargetDefinition()) {
					TargetPlatformUtil.loadAndSetTarget(beforeTarget);
				}
			});

	private static final String RUNNING_PLATFORM_TARGET_NAME = TargetPlatformUtil.class + "_RunningPlatformTarget";

	public static void setRunningPlatformAsTarget() throws CoreException, InterruptedException {
		setRunningPlatformSubSetAsTarget(RUNNING_PLATFORM_TARGET_NAME, null);
	}

	public static void setRunningPlatformSubSetAsTarget(String name, Predicate<Bundle> bundleFilter)
			throws CoreException, InterruptedException {
		ITargetDefinition currentTarget = TPS.getWorkspaceTargetDefinition();
		if (name.equals(currentTarget.getName())) {
			return;
		}
		List<ITargetLocation> bundleContainers = new ArrayList<>();
		List<NameVersionDescriptor> included = addRunningPlatformBundles(bundleContainers, bundleFilter);
		createAndSetTarget(name, bundleContainers, included);
	}

	public static void setRunningPlatformWithDummyBundlesAsTarget(Predicate<Bundle> rpBundleFilter,
			Map<NameVersionDescriptor, Map<String, String>> bundles, Collection<NameVersionDescriptor> features,
			Path jarDirectory) throws Exception {

		Set<ITargetLocation> locations = new LinkedHashSet<>();
		List<NameVersionDescriptor> rpBundles = addRunningPlatformBundles(locations, rpBundleFilter);
		locations.add(createDummyBundlesLocation(bundles, jarDirectory));

		createAndSetTarget(null, locations, concat(rpBundles, bundles.keySet(), features));
	}

	public static void loadAndSetTarget(ITargetDefinition target) throws InterruptedException {
		Job job = new LoadTargetDefinitionJob(target);
		job.schedule();
		job.join();

		IStatus result = job.getResult();
		if (!result.isOK()) {
			throw new AssertionError(result.getMessage(), result.getException());
		}
		TestUtils.waitForJobs("loadAndSetTarget", 100, 10000);
	}

	private static List<NameVersionDescriptor> addRunningPlatformBundles(Collection<ITargetLocation> bundleContainers,
			Predicate<Bundle> bundleFilter) {
		Bundle[] installedBundles = FrameworkUtil.getBundle(TargetPlatformUtil.class).getBundleContext().getBundles();
		List<Bundle> targetBundles = Arrays.asList(installedBundles);
		if (bundleFilter != null) {
			targetBundles = targetBundles.stream().filter(bundleFilter).toList();
		}

		var containerDirs = targetBundles.stream().map(FileLocator::getBundleFileLocation).map(Optional::orElseThrow)
				.map(File::getParentFile).distinct();
		containerDirs.map(dir -> TPS.newDirectoryLocation(dir.getAbsolutePath())).forEach(bundleContainers::add);

		return targetBundles.stream()
				.map(b -> new NameVersionDescriptor(b.getSymbolicName(), b.getVersion().toString())).toList();
	}

	public static void createAndSetTarget(String name, Collection<ITargetLocation> locations,
			Collection<NameVersionDescriptor> includedBundles) throws InterruptedException {
		ITargetDefinition targetDefinition = TPS.newTarget();
		targetDefinition.setName(name);
		targetDefinition.setArch(Platform.getOSArch());
		targetDefinition.setOS(Platform.getOS());
		targetDefinition.setWS(Platform.getWS());
		targetDefinition.setNL(Platform.getNL());
		targetDefinition.setTargetLocations(locations.toArray(ITargetLocation[]::new));

		// only include intended target bundles to speed up resolution in cases
		// where direction-location points into large bundle-pools with many
		// other bundles, e.g. when one uses a Eclipse provisioned by Oomph
		if (includedBundles != null) {
			targetDefinition.setIncluded(includedBundles.toArray(NameVersionDescriptor[]::new));
		}
		loadAndSetTarget(targetDefinition);
	}

	public static void setDummyBundlesAsTarget(Map<NameVersionDescriptor, Map<String, String>> bundles,
			Collection<NameVersionDescriptor> features, Path jarDirectory) throws Exception {
		ITargetLocation location = createDummyBundlesLocation(bundles, jarDirectory);
		createAndSetTarget(null, List.of(location), concat(bundles.keySet(), features));
	}

	private static ITargetLocation createDummyBundlesLocation(Map<NameVersionDescriptor, Map<String, String>> plugins,
			Path jarDirectory) throws IOException {
		Path pluginsDirectory = jarDirectory.resolve("plugins");
		Files.createDirectories(pluginsDirectory);
		for (Entry<NameVersionDescriptor, Map<String, String>> entry : plugins.entrySet()) {
			NameVersionDescriptor bundleNameVersion = entry.getKey();
			Map<String, String> extraAttributes = entry.getValue();

			Manifest manifest = new Manifest();
			Attributes mainAttributes1 = manifest.getMainAttributes();
			mainAttributes1.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			mainAttributes1.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
			mainAttributes1.putValue(Constants.BUNDLE_SYMBOLICNAME, bundleNameVersion.getId());
			mainAttributes1.putValue(Constants.BUNDLE_VERSION, bundleNameVersion.getVersion());
			mainAttributes1.putValue(Constants.BUNDLE_NAME, bundleNameVersion.getId().replace('.', ' '));
			mainAttributes1.putValue("Automatic-Module-Name", bundleNameVersion.getId());
			if (extraAttributes != null) {
				extraAttributes.forEach(manifest.getMainAttributes()::putValue);
			}

			Attributes mainAttributes = manifest.getMainAttributes();
			String bundleSymbolicName = Objects.requireNonNull(mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
			String bundleVersion = Objects.requireNonNull(mainAttributes.getValue(Constants.BUNDLE_VERSION));

			Path jarPath = pluginsDirectory.resolve(bundleSymbolicName + "_" + bundleVersion + ".jar");
			try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jarPath));) {
				out.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
				manifest.write(out);
			}
		}
		return TPS.newDirectoryLocation(jarDirectory.toString());
	}

	@SafeVarargs
	public static Entry<NameVersionDescriptor, Map<String, String>> bundle(String id, String version,
			Entry<String, String>... additionalManifestEntries) {
		return entry(new NameVersionDescriptor(id, version), Map.ofEntries(additionalManifestEntries));
	}

	public static String version(String version) {
		return ";" + Constants.VERSION_ATTRIBUTE + "=\"" + version + "\"";
	}

	public static String bundleVersion(String lowerBound, String upperBound) {
		return ";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"[" + lowerBound + "," + upperBound + ")\"";
	}

	public static String resolution(String resolutionType) {
		return ";" + Constants.RESOLUTION_DIRECTIVE + ":=\"" + resolutionType + "\"";
	}

	public static NameVersionDescriptor targetFeature(String featureId, String featureVersion,
			CoreConsumer<IFeature> featureSetup, Path bundlePoolRoot) throws Exception, IOException, CoreException {
		IFeature feature = ProjectUtils.createFeature(featureId, featureVersion, "tp-feature-temp-project",
				featureSetup);

		WorkspaceFeatureModel model = (WorkspaceFeatureModel) feature.getModel();
		IResource resource = model.getUnderlyingResource();

		Path featureDirectory = bundlePoolRoot.resolve(Path.of("features", featureId + "_" + featureVersion));
		Files.createDirectories(featureDirectory);
		Path featureFile = featureDirectory.resolve(resource.getProjectRelativePath().toString());
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(featureFile));) {
			model.save(writer);
		}
		IProject project = resource.getProject();
		project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);

		return new NameVersionDescriptor(featureId, featureVersion, NameVersionDescriptor.TYPE_FEATURE);
	}

	@SafeVarargs
	private static <T> Set<T> concat(Collection<T>... colls) {
		return Arrays.stream(colls).flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
