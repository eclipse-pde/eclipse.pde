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
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 *     Hannes Wellmann - Bug 577385: Add tests for Plug-in based Eclipse-App launches
 *******************************************************************************/
package org.eclipse.pde.ui.tests.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.junit.rules.TestRule;
import org.osgi.framework.*;

public class TargetPlatformUtil {

	public static final ITargetPlatformService TPS = PDECore.getDefault().acquireService(ITargetPlatformService.class);
	public static final TestRule RESTORE_CURRENT_TARGET_DEFINITION_AFTER = TestUtils.getThrowingTestRule(
			TPS::getWorkspaceTargetDefinition, //
			beforeTarget -> {
				if (beforeTarget != TPS.getWorkspaceTargetDefinition()) {
					TargetPlatformUtil.loadAndSetTargetForWorkspace(beforeTarget);
				}
			});

	private static final String RUNNING_PLATFORM_TARGET_NAME = TargetPlatformUtil.class + "_RunningPlatformTarget";

	public static void setRunningPlatformAsTarget() throws IOException, CoreException, InterruptedException {
		setRunningPlatformSubSetAsTarget(RUNNING_PLATFORM_TARGET_NAME, null);
	}

	public static void setRunningPlatformSubSetAsTarget(String name, Predicate<Bundle> bundleFilter)
			throws IOException, CoreException, InterruptedException {
		ITargetDefinition currentTarget = TPS.getWorkspaceTargetDefinition();
		if (name.equals(currentTarget.getName())) {
			return;
		}
		List<ITargetLocation> bundleContainers = new ArrayList<>();
		List<NameVersionDescriptor> included = new ArrayList<>();
		addRunningPlatformBundles(bundleContainers, included, bundleFilter);
		createAndSetTargetForWorkspace(name, bundleContainers, included);
	}

	public static void loadAndSetTargetForWorkspace(ITargetDefinition target) throws InterruptedException {
		Job job = new LoadTargetDefinitionJob(target);
		job.schedule();
		job.join();

		IStatus result = job.getResult();
		if (!result.isOK()) {
			throw new AssertionError(result.getMessage(), result.getException());
		}
	}

	private static void addRunningPlatformBundles(Collection<ITargetLocation> bundleContainers,
			Collection<NameVersionDescriptor> included, Predicate<Bundle> bundleFilter) throws IOException {
		Bundle[] installedBundles = FrameworkUtil.getBundle(TargetPlatformUtil.class).getBundleContext().getBundles();
		List<Bundle> targetBundles = Arrays.asList(installedBundles);
		if (bundleFilter != null) {
			targetBundles = targetBundles.stream().filter(bundleFilter).collect(Collectors.toList());
		}

		Set<File> bundleContainerDirectories = new HashSet<>();
		for (Bundle bundle : targetBundles) {
			File bundleContainer = FileLocator.getBundleFile(bundle).getParentFile();
			bundleContainerDirectories.add(bundleContainer);
		}
		bundleContainerDirectories.stream().map(dir -> TPS.newDirectoryLocation(dir.getAbsolutePath()))
		.forEach(bundleContainers::add);

		for (Bundle bundle : targetBundles) {
			included.add(new NameVersionDescriptor(bundle.getSymbolicName(), bundle.getVersion().toString()));
		}
	}

	public static void createAndSetTargetForWorkspace(String name, Collection<ITargetLocation> locations,
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
		loadAndSetTargetForWorkspace(targetDefinition);
	}

	public static void setDummyBundlesAsTarget(List<NameVersionDescriptor> targetPlugins, Path jarDirectory)
			throws IOException, InterruptedException {
		ITargetLocation location = createDummyBundlesLocation(targetPlugins, jarDirectory);
		createAndSetTargetForWorkspace(null, List.of(location), targetPlugins);
	}

	private static ITargetLocation createDummyBundlesLocation(List<NameVersionDescriptor> targetPlugins,
			Path jarDirectory) throws IOException {
		for (NameVersionDescriptor bundleNameVersion : targetPlugins) {

			Manifest manifest = createDummyBundleManifest(bundleNameVersion.getId(), bundleNameVersion.getVersion());

			Attributes mainAttributes = manifest.getMainAttributes();
			String bundleSymbolicName = Objects.requireNonNull(mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
			String bundleVersion = Objects.requireNonNull(mainAttributes.getValue(Constants.BUNDLE_VERSION));

			Path jarPath = jarDirectory.resolve(bundleSymbolicName + "_" + bundleVersion + ".jar");
			try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jarPath));) {
				out.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
				manifest.write(out);
			}
		}
		return TPS.newDirectoryLocation(jarDirectory.toString());
	}

	private static Manifest createDummyBundleManifest(String bundleSymbolicName, String bundleVersion) {
		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mainAttributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		mainAttributes.putValue(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
		mainAttributes.putValue(Constants.BUNDLE_VERSION, bundleVersion);
		mainAttributes.putValue(Constants.BUNDLE_NAME, bundleSymbolicName.replace('.', ' '));
		mainAttributes.putValue("Automatic-Module-Name", bundleSymbolicName);
		return manifest;
	}

}
