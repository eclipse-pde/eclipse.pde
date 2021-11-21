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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.junit.rules.TestRule;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.osgi.framework.*;

public class TargetPlatformUtil {

	public static final TestRule RESTORE_CURRENT_TARGET_DEFINITION_AFTER = (base, description) -> new Statement() {
		@Override
		public void evaluate() throws Throwable {
			ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			ITargetDefinition beforeTarget = null;
			List<Throwable> errors = new ArrayList<>();
			try {
				beforeTarget = tps.getWorkspaceTargetDefinition();

				base.evaluate();
			} catch (Throwable t) {
				errors.add(t);
			} finally {
				try {
					if (beforeTarget != null && beforeTarget != tps.getWorkspaceTargetDefinition()) {
						TargetPlatformUtil.loadAndSetTargetForWorkspace(beforeTarget);
					}
				} catch (Throwable t) {
					errors.add(t);
				}
			}
			MultipleFailureException.assertEmpty(errors);
		}
	};

	private static final String TARGET_NAME = TargetPlatformUtil.class + "_target";

	public static void setRunningPlatformAsTarget() throws IOException, CoreException, InterruptedException {
		setRunningPlatformSubSetAsTarget(TARGET_NAME, null);
	}

	public static void setRunningPlatformSubSetAsTarget(String name, Predicate<Bundle> bundleFilter)
			throws IOException, CoreException, InterruptedException {
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition currentTarget = tps.getWorkspaceTargetDefinition();
		if (name.equals(currentTarget.getName())) {
			return;
		}
		ITargetDefinition target = createRunningPlatformSubSetTarget(tps, name, bundleFilter);
		loadAndSetTargetForWorkspace(target);
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

	private static ITargetDefinition createRunningPlatformSubSetTarget(ITargetPlatformService tps, String name,
			Predicate<Bundle> bundleFilter) throws IOException, CoreException {
		ITargetDefinition targetDefinition = tps.newTarget();
		targetDefinition.setName(TARGET_NAME);

		Bundle[] installedBundles = FrameworkUtil.getBundle(TargetPlatformUtil.class).getBundleContext().getBundles();
		Bundle[] targetBundles = bundleFilter != null
				? Arrays.stream(installedBundles).filter(bundleFilter).toArray(Bundle[]::new)
						: installedBundles;

		Set<File> bundleContainerDirectories = new HashSet<>();
		for (Bundle bundle : targetBundles) {
			File bundleContainer = FileLocator.getBundleFile(bundle).getParentFile();
			bundleContainerDirectories.add(bundleContainer);
		}
		ITargetLocation[] bundleContainers = bundleContainerDirectories.stream()
				.map(dir -> tps.newDirectoryLocation(dir.getAbsolutePath())).toArray(ITargetLocation[]::new);

		// always only include targetBundles bundles to speed up resolution in
		// cases where large bundle-pools with many other bundles are used,
		// e.g. when one uses a Eclipse provisioned by Oomph
		NameVersionDescriptor[] included = Arrays.stream(targetBundles)
				.map(b -> new NameVersionDescriptor(b.getSymbolicName(), b.getVersion().toString()))
				.toArray(NameVersionDescriptor[]::new);
		targetDefinition.setIncluded(included);

		setTargetProperties(targetDefinition, bundleContainers);

		tps.saveTargetDefinition(targetDefinition);
		return targetDefinition;
	}

	public static void setTargetProperties(ITargetDefinition targetDefinition, ITargetLocation[] bundleContainers) {
		targetDefinition.setTargetLocations(bundleContainers);
		targetDefinition.setArch(Platform.getOSArch());
		targetDefinition.setOS(Platform.getOS());
		targetDefinition.setWS(Platform.getWS());
		targetDefinition.setNL(Platform.getNL());
	}

	public static void setDummyBundlesAsTarget(List<NameVersionDescriptor> targetPlugins, Path jarDirectory)
			throws IOException, CoreException, InterruptedException {
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
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition target = tps.newTarget();

		ITargetLocation location1 = tps.newDirectoryLocation(jarDirectory.toString());

		setTargetProperties(target, new ITargetLocation[] { location1 });
		tps.saveTargetDefinition(target);
		loadAndSetTargetForWorkspace(target);
	}

	public static Manifest createDummyBundleManifest(String bundleSymbolicName, String bundleVersion) {
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
