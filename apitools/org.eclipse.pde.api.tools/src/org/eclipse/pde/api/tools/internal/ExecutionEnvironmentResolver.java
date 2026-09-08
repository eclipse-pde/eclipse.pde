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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.internal.core.util.ManifestUtils;

/**
 * Resolves the Java compiler compliance level from a parsed bundle manifest
 * map.
 *
 * <h2>Background</h2>
 * <p>
 * The {@link APIFileGenerator} scans Java source files with the JDT AST parser
 * to extract Javadoc tags (e.g. {@code @noreference}) and write them into the
 * bundle's {@code .api_description} file. The AST parser must be configured
 * with a compiler compliance level that matches (or exceeds) the Java version
 * used in the source code. If the compliance level is too low, the parser
 * cannot understand modern Java syntax (e.g. {@code sealed} classes introduced
 * in Java 17) and silently skips the Javadoc tags, resulting in an incorrect or
 * incomplete {@code .api_description}.
 * </p>
 *
 * <h2>Header resolution</h2>
 * <p>
 * Two OSGi manifest headers are considered:
 * </p>
 * <ul>
 * <li><b>{@code Bundle-RequiredExecutionEnvironment} (BREE)</b> — the legacy
 * header, deprecated since OSGi 1.6. May list multiple comma-separated EE names
 * (e.g. {@code JavaSE-17, JavaSE-21}).</li>
 * <li><b>{@code Require-Capability: osgi.ee}</b> — the modern OSGi replacement
 * for BREE. The EE is expressed as an LDAP filter on the {@code osgi.ee}
 * namespace, delegated to
 * {@link ManifestUtils#getRequiredExecutionEnvironments(Map)}.</li>
 * </ul>
 * <p>
 * All EE IDs from both headers are collected and mapped to JavaCore version
 * strings via {@link ManifestUtils#eeIdToJavaVersion(String)}. The
 * <em>highest</em> version is returned: since every listed EE must be satisfied
 * (conjunctive requirements), the parser must understand the syntax of the
 * most-recent required version.
 * </p>
 *
 * <h2>Unsupported / unknown versions</h2>
 * <p>
 * Java versions that are no longer supported by the JDT compiler (supported are
 * those from {@link JavaCore#getAllJavaSourceVersionsSupportedByCompiler()})
 * are ignored. If no supported version can be determined
 * {@link JavaCore#latestSupportedJavaVersion()} is returned.
 */
public class ExecutionEnvironmentResolver {

	private ExecutionEnvironmentResolver() {
		// utility class
	}

	/**
	 * Returns the JDT compiler compliance string for the given manifest map.
	 *
	 * @param manifestMap the parsed bundle manifest, may be {@code null}
	 * @return a JDT compliance string (e.g. {@code "17"}), never {@code null}
	 */
	public static String resolveCompliance(Map<String, String> manifestMap) {
		if (manifestMap == null) {
			ApiPlugin.logErrorMessage("ExecutionEnvironmentResolver: manifestMap is null, falling back to compliance " //$NON-NLS-1$
					+ getFallbackJavaVersion());
			return getFallbackJavaVersion();
		}

		try {
			Optional<String> highest = ManifestUtils.getRequiredExecutionEnvironments(manifestMap) // extract ee ids to
																									// stream
					.map(ManifestUtils::eeIdToJavaVersion) // ee to java version string
					.filter(Objects::nonNull) // remove nulls (unknown ee ids)
					.filter(v -> JavaCore.getAllJavaSourceVersionsSupportedByCompiler().contains(v)) // remove
																										// unsupported
																										// versions
					.max(JavaCore::compareJavaVersions); // choose the highest version
			if (highest.isPresent()) {
				return highest.get();
			}
		} catch (IllegalArgumentException e) {
			ApiPlugin.log(e);
		}

		ApiPlugin.logErrorMessage(
				"ExecutionEnvironmentResolver: unknown or unsupported execution environment in manifest, falling back to compliance " //$NON-NLS-1$
						+ getFallbackJavaVersion());
		return getFallbackJavaVersion();
	}

	private static String getFallbackJavaVersion() {
		return JavaCore.latestSupportedJavaVersion();
	}
}
