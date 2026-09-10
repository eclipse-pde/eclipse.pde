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
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.ExecutionEnvironmentResolver;
import org.junit.Assume;
import org.junit.Test;

/** Tests for {@link ExecutionEnvironmentResolver}. */
public class ExecutionEnvironmentResolverTest {

	@Test
	public void testNullManifestReturnsLatestSupported() {
		assertEquals(JavaCore.latestSupportedJavaVersion(), ExecutionEnvironmentResolver.resolveCompliance(null));
	}

	@Test
	public void testEmptyManifestReturnsLatestSupported() {
		assertEquals(JavaCore.latestSupportedJavaVersion(),
				ExecutionEnvironmentResolver.resolveCompliance(new HashMap<>()));
	}

	@Test
	public void testSupportedBreeVersionResolvesCorrectly() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		for (String version : supported) {
			Map<String, String> manifest = breeManifest("JavaSE-" + version); //$NON-NLS-1$
			assertEquals("BREE JavaSE-" + version, version, ExecutionEnvironmentResolver.resolveCompliance(manifest)); //$NON-NLS-1$
		}
	}

	@Test
	public void testUnsupportedBreeVersionFallsBackToLatest() {
		for (String old : new String[] { "J2SE-1.4", "J2SE-1.5", "JavaSE-1.6", "JavaSE-1.7" }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			assertEquals("Unsupported BREE " + old, //$NON-NLS-1$
					JavaCore.latestSupportedJavaVersion(),
					ExecutionEnvironmentResolver.resolveCompliance(breeManifest(old)));
		}
	}

	/** Multiple EEs are conjunctive — the resolver must return the highest supported one. */
	@Test
	public void testMultipleVersionsReturnsHighest() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		Assume.assumeFalse("Requires at least two supported java versions", supported.size() < 2); //$NON-NLS-1$
		List<String> versions = new ArrayList<>(supported);
		String low = versions.getFirst();
		String high = versions.getLast();
		Map<String, String> manifest = breeManifest("JavaSE-" + high + ", JavaSE-" + low); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Highest of multiple BREE versions must win", high, //$NON-NLS-1$
				ExecutionEnvironmentResolver.resolveCompliance(manifest));
	}

	/** BREE and Require-Capability together — the resolver must pick the highest across both headers. */
	@Test
	public void testBreeAndRequireCapabilityCombinedHighestWins() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		Assume.assumeFalse("Requires at least two supported java versions", supported.size() < 2); //$NON-NLS-1$
		String low = supported.getFirst();
		String high = supported.getLast();
		Map<String, String> manifest = Map.of(
				"Bundle-RequiredExecutionEnvironment", "JavaSE-" + low, //$NON-NLS-1$ //$NON-NLS-2$
				"Require-Capability", "osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version=" + high + "))\"" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		);
		assertEquals("Highest version across BREE and Require-Capability must win", high, //$NON-NLS-1$
				ExecutionEnvironmentResolver.resolveCompliance(manifest));
	}

	/** The result must always be a JDT-supported version, regardless of input. */
	@Test
	public void testResultIsAlwaysASupportedVersion() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		List<Map<String, String>> manifests = List.of(Map.of(), breeManifest("JavaSE-999"), //$NON-NLS-1$
				breeManifest("garbage"), //$NON-NLS-1$
				eeCapabilityManifest("(&(osgi.ee=JavaSE)(version=999))") //$NON-NLS-1$
		);
		String nullResult = ExecutionEnvironmentResolver.resolveCompliance(null);
		assertNotNull(nullResult);
		assertTrue("null input must yield a supported version", supported.contains(nullResult)); //$NON-NLS-1$
		for (Map<String, String> manifest : manifests) {
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
			assertNotNull(result);
			assertTrue("Result '" + result + "' must be a supported Java version", supported.contains(result)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static Map<String, String> breeManifest(String breeValue) {
		return Map.of("Bundle-RequiredExecutionEnvironment", breeValue); //$NON-NLS-1$
	}

	private static Map<String, String> eeCapabilityManifest(String filter) {
		return Map.of("Require-Capability", "osgi.ee; filter:=\"" + filter + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
