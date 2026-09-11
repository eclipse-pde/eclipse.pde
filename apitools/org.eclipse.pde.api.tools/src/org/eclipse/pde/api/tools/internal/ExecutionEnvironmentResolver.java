/*******************************************************************************
 * Copyright (c) 2026 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.internal.core.util.ManifestUtils;

/**
 * Resolves the JDT compiler compliance level from a bundle manifest map, based
 * on the {@code Bundle-RequiredExecutionEnvironment} and
 * {@code Require-Capability: osgi.ee} headers. Returns the highest version
 * among all stated EEs so the JDT parser can handle all required syntax.
 *
 * If there is no supported EE, the latest supported version is returned and a
 * warning is logged.
 */
public class ExecutionEnvironmentResolver {

	private static final String LATEST_SUPPORTED_JAVA_VERSION = JavaCore.latestSupportedJavaVersion();

	private ExecutionEnvironmentResolver() {
		// utility class
	}

	public static String resolveCompliance(Map<String, String> manifestMap) {
		if (manifestMap == null) {
			return useLatestSupportedVersion("ExecutionEnvironmentResolver: manifestMap is null"); //$NON-NLS-1$
		}
		try {
			Optional<String> highestEE = ManifestUtils.getRequiredExecutionEnvironments(manifestMap)
					.map(ManifestUtils::javaVersionOfExecutionEnvironment).filter(Objects::nonNull)
					.filter(JavaCore::isJavaSourceVersionSupportedByCompiler)
					.max(JavaCore::compareJavaVersions);
			if (highestEE.isPresent()) {
				return highestEE.get();
			}
		} catch (IllegalArgumentException e) {
			ApiPlugin.log(e);
		}
		return useLatestSupportedVersion(
				"ExecutionEnvironmentResolver: unknown or unsupported execution environment in manifest"); //$NON-NLS-1$
	}

	private static String useLatestSupportedVersion(String msg) {
		ILog.get().log(Status.warning(msg + ", falling back to Java " + LATEST_SUPPORTED_JAVA_VERSION)); //$NON-NLS-1$
		return LATEST_SUPPORTED_JAVA_VERSION;
	}
}
