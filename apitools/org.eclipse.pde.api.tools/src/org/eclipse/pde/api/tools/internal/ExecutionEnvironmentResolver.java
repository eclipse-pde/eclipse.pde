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

import java.util.Hashtable;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;

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
 * <h2>Header resolution order</h2>
 * <p>
 * Two OSGi manifest headers are considered, in priority order:
 * </p>
 * <ol>
 * <li><b>{@code Bundle-RequiredExecutionEnvironment} (BREE)</b> — the legacy
 * header, deprecated since OSGi 1.6. May list multiple comma-separated EE names
 * (e.g. {@code JavaSE-17, JavaSE-21}), in which case the lowest supported
 * version is used so that the source is parsed with the minimum required
 * compliance.</li>
 * <li><b>{@code Require-Capability: osgi.ee}</b> — the modern OSGi replacement
 * for BREE. The EE is expressed as an LDAP filter on the {@code osgi.ee}
 * namespace. Multiple {@code osgi.ee} entries are treated as alternatives and
 * the lowest matching version is returned, consistent with the BREE behaviour.
 * Each filter is evaluated against all versions known to
 * {@link JavaCore#getAllJavaSourceVersionsSupportedByCompiler()} using
 * {@link FrameworkUtil#createFilter(String)}.</li>
 * </ol>
 *
 * <h2>Unsupported / unknown versions</h2>
 * <p>
 * Java versions that are no longer supported by the JDT compiler ( supported
 * are those from
 * {@link JavaCore#getAllJavaSourceVersionsSupportedByCompiler()}) are ignored.
 * If no supported version can be determined
 * {@link JavaCore#latestSupportedJavaVersion()} is returned.
 */
public class ExecutionEnvironmentResolver {

	private static final String JAVASE_EE_NAME = "JavaSE"; //$NON-NLS-1$
	private static final String BREE_SEPARATOR = ","; //$NON-NLS-1$
	private static final String FILTER_DIRECTIVE = "filter"; //$NON-NLS-1$

	private ExecutionEnvironmentResolver() {
		// utility class
	}

	/**
	 * Returns {@link JavaCore#latestSupportedJavaVersion()} as the fallback
	 * compliance.
	 */
	private static String getFallbackJavaVersion() {
		return JavaCore.latestSupportedJavaVersion();
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

		// 1. Legacy BREE header (may contain multiple comma-separated values)
		@SuppressWarnings("deprecation")
		String bree = manifestMap.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (bree != null) {
			String result = fromBree(bree);
			if (result != null) {
				return result;
			}
		}

		// 2. Modern osgi.ee Require-Capability header
		String requireCapability = manifestMap.get(Constants.REQUIRE_CAPABILITY);
		if (requireCapability != null) {
			String result = fromRequireCapability(requireCapability);
			if (result != null) {
				return result;
			}
		}

		ApiPlugin.logErrorMessage(
				"ExecutionEnvironmentResolver: unknown or unsupported execution environment in manifest, falling back to compliance " //$NON-NLS-1$
						+ getFallbackJavaVersion());

		return getFallbackJavaVersion();
	}

	/**
	 * Returns the lowest supported compliance from a comma-separated BREE header,
	 * or {@code null} if no supported {@code JavaSE-X} entry is found.
	 */
	private static String fromBree(String breeHeader) {
		String[] entries = breeHeader.split(BREE_SEPARATOR);
		String lowest = null;
		for (String entry : entries) {
			String result = fromSingleBree(entry.trim());
			if (result == null) {
				continue;
			}
			if (lowest == null || JavaCore.compareJavaVersions(result, lowest) < 0) {
				lowest = result;
			}
		}
		return lowest;
	}

	/**
	 * Returns the JDT compliance string for a single BREE entry, or {@code null}.
	 */
	private static String fromSingleBree(String eename) {
		// Java 8 compact profiles: only three variants exist, all require Java 1.8
		if (JavaCore.getAllJavaSourceVersionsSupportedByCompiler().contains(JavaCore.VERSION_1_8)) {
			if ("JavaSE/compact1-1.8".equals(eename) //$NON-NLS-1$
					|| "JavaSE/compact2-1.8".equals(eename) //$NON-NLS-1$
					|| "JavaSE/compact3-1.8".equals(eename)) { //$NON-NLS-1$
				return JavaCore.VERSION_1_8;
			}
		}
		int separator = eename.lastIndexOf('-');
		if (separator > 0) {
			String eeName = eename.substring(0, separator);
			String version = eename.substring(separator + 1);
			if (JAVASE_EE_NAME.equals(eeName)
					&& JavaCore.getAllJavaSourceVersionsSupportedByCompiler().contains(version)) {
				return version;
			}
		}
		ApiPlugin.logErrorMessage("ExecutionEnvironmentResolver: unknown or unsupported execution environment '" //$NON-NLS-1$
				+ eename);
		return null;
	}

	/**
	 * Returns the highest supported compliance across all {@code osgi.ee=JavaSE}
	 * capabilities in the {@code Require-Capability} header, or {@code null}.
	 * Multiple {@code osgi.ee} entries are conjunctive (AND) requirements, so the
	 * parser must understand the syntax of the highest required version. Within a
	 * single filter expression (e.g. an OR filter) the lowest matching version is
	 * used, since the filter itself defines what suffices for that entry.
	 */
	private static String fromRequireCapability(String requireCapability) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, requireCapability);
			if (elements == null) {
				return null;
			}
			String highest = null;
			for (ManifestElement element : elements) {
				if (!ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(element.getValue())) {
					continue;
				}
				String filterString = element.getDirective(FILTER_DIRECTIVE);
				if (filterString == null) {
					continue;
				}
				String version = matchJavaSEVersion(filterString);
				if (version == null) {
					continue;
				}
				if (highest == null || JavaCore.compareJavaVersions(version, highest) > 0) {
					highest = version;
				}
			}
			return highest;
		} catch (BundleException e) {
			ApiPlugin.log(e);
		}
		return null;
	}

	/**
	 * Evaluates the LDAP filter against a {@code {osgi.ee=JavaSE, version=X}}
	 * dictionary for each supported version and returns the first match, or
	 * {@code null}. Handles complex filters (e.g. OR, version ranges) correctly
	 * without custom parsing. Returns {@code null} for malformed filters.
	 */
	private static String matchJavaSEVersion(String filterString) {
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter(filterString);
		} catch (InvalidSyntaxException e) {
			return null;
		}
		SortedSet<String> supportedVersions = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		for (String version : supportedVersions) {
			Hashtable<String, String> dict = new Hashtable<>();
			dict.put(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, JAVASE_EE_NAME);
			dict.put(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
			if (filter.matches(dict)) {
				return version;
			}
		}
		return null;
	}
}
