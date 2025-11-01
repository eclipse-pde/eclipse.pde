/*******************************************************************************
 * Copyright (c) 2025 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.osgi.framework.Version;

public class JUnitLaunchValidationOperation extends LaunchValidationOperation {

	private static final Set<String> JUNIT_PLATFORM_ENGINE_BUNLDES = Set.of(new String[] { //
			"junit-platform-engine", //$NON-NLS-1$
			"org.junit.platform.engine", //$NON-NLS-1$
	});

	private final Map<Object, Object[]> fErrors = new HashMap<>(2);

	public JUnitLaunchValidationOperation(ILaunchConfiguration configuration, Set<IPluginModelBase> models) {
		super(configuration, models, null);
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		try {
			checkJunitVersion(fLaunchConfiguration, fModels);
		} catch (CoreException e) {
			PDELaunchingPlugin.log(e);
		}
	}

	@SuppressWarnings("restriction")
	private void checkJunitVersion(ILaunchConfiguration configuration, Set<IPluginModelBase> models) throws CoreException {
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			return;
		}
		Set<Version> junitPlatformBundlesVersions = junitPlatformBundleVersions(models);
		String testKindId = testKind.getId();
		switch (testKindId) {
			case TestKindRegistry.JUNIT3_TEST_KIND_ID, TestKindRegistry.JUNIT4_TEST_KIND_ID -> {
			} // nothing to check
			case TestKindRegistry.JUNIT5_TEST_KIND_ID -> {
				// JUnit 5 platform bundles have version range [1.0,2.0)
				junitPlatformBundlesVersions.stream().map(Version::getMajor).filter(i -> i.intValue() != 1).findFirst().ifPresent(otherVersion -> {
					String message = NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_JUnitLaunchAndRuntimeMissmatch, 5, otherVersion);
					addError(message);
				});
			}
			default -> throw new CoreException(Status.error("Unsupported test kind: " + testKindId)); //$NON-NLS-1$
		}
	}

	private void addError(String message) {
		fErrors.put(message.replaceAll("\\R", " "), null); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public boolean hasErrors() {
		return !fErrors.isEmpty();
	}

	@Override
	public Map<Object, Object[]> getInput() {
		Map<Object, Object[]> map = new LinkedHashMap<>();
		map.putAll(fErrors);
		return map;
	}

	private static Set<Version> junitPlatformBundleVersions(Set<IPluginModelBase> models) {
		return models.stream().map(IPluginModelBase::getBundleDescription) //
				.filter(d -> JUNIT_PLATFORM_ENGINE_BUNLDES.contains(d.getSymbolicName())) //
				.map(BundleDescription::getVersion).collect(Collectors.toSet());
	}
}
