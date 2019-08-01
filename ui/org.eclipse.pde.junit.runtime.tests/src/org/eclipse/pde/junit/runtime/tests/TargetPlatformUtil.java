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
package org.eclipse.pde.junit.runtime.tests;

import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class TargetPlatformUtil {

	private static final String TARGET_NAME = TargetPlatformUtil.class + "_target";

	public static void setRunningPlatformAsTarget() throws CoreException, InterruptedException {
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition currentTarget = tps.getWorkspaceTargetDefinition();
		if (TARGET_NAME.equals(currentTarget.getName())) {
			return;
		}

		Job job = new LoadTargetDefinitionJob(createTarget(tps));
		job.schedule();
		job.join();

		IStatus result = job.getResult();
		if (!result.isOK()) {
			throw new AssertionError(result.getMessage(), result.getException());
		}
	}

	private static ITargetDefinition createTarget(ITargetPlatformService tps) throws CoreException {
		ITargetDefinition targetDefinition = tps.newTarget();
		targetDefinition.setName(TARGET_NAME);

		Bundle[] installedBundles = FrameworkUtil.getBundle(TargetPlatformUtil.class).getBundleContext().getBundles();
		ITargetLocation[] bundleContainers = Arrays.stream(installedBundles).map(bundle -> {
			EquinoxBundle bundleImpl = (EquinoxBundle) bundle;
			Generation generation = (Generation) bundleImpl.getModule().getCurrentRevision().getRevisionInfo();
			return generation.getBundleFile();
		}).map(f -> f.getBaseFile().getParentFile()).distinct()
				.map(dir -> tps.newDirectoryLocation(dir.getAbsolutePath())).toArray(ITargetLocation[]::new);

		NameVersionDescriptor[] included = Arrays.stream(installedBundles)
				.map(b -> new NameVersionDescriptor(b.getSymbolicName(), b.getVersion().toString()))
				.toArray(NameVersionDescriptor[]::new);
		targetDefinition.setIncluded(included);

		targetDefinition.setTargetLocations(bundleContainers);
		targetDefinition.setArch(Platform.getOSArch());
		targetDefinition.setOS(Platform.getOS());
		targetDefinition.setWS(Platform.getWS());
		targetDefinition.setNL(Platform.getNL());

		tps.saveTargetDefinition(targetDefinition);
		return targetDefinition;
	}

}
