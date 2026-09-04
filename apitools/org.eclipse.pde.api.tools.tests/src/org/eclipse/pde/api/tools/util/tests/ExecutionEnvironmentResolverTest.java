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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.ExecutionEnvironmentResolver;
import org.junit.Test;
import org.osgi.framework.BundleException;

/**
 * Tests for {@link ExecutionEnvironmentResolver}.
 *
 * <p>
 * Verifies that the correct JDT compliance string is derived from bundle
 * manifest maps for all relevant execution environment header combinations.
 * </p>
 */
public class ExecutionEnvironmentResolverTest {

	// --- null / empty input ---

	/** A null manifest must return the fallback (latest supported). */
	@Test
	public void testNullManifestReturnsLatestSupported() {
		String result = ExecutionEnvironmentResolver.resolveCompliance(null);
		assertNotNull(result);
		assertEquals(JavaCore.latestSupportedJavaVersion(), result);
	}

	/** An empty manifest (no EE headers) must return the fallback. */
	@Test
	public void testEmptyManifestReturnsLatestSupported() {
		String result = ExecutionEnvironmentResolver.resolveCompliance(new HashMap<>());
		assertNotNull(result);
		assertEquals(JavaCore.latestSupportedJavaVersion(), result);
	}

	// --- BREE: all supported JavaSE-X versions ---

	/**
	 * For every version in {@link JavaCore#getAllJavaSourceVersionsSupportedByCompiler()}
	 * a BREE of the form {@code JavaSE-X} must resolve to exactly that version.
	 */
	@Test
	public void testBreeSingleVersionAllSupportedVersions() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		for (String version : supported) {
			Map<String, String> manifest = breeManifest("JavaSE-" + version); //$NON-NLS-1$
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
			assertEquals("BREE JavaSE-" + version + " should resolve to " + version, version, result); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	// --- BREE: multiple values → lowest wins ---

	/**
	 * When BREE lists two supported versions, the lower one must be returned.
	 */
	@Test
	public void testBreeMultipleVersionsReturnsLowest() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		if (supported.size() < 2) {
			return; // not enough versions to test
		}
		String lowest = supported.first();
		String highest = supported.last();
		Map<String, String> manifest = breeManifest("JavaSE-" + highest + ", JavaSE-" + lowest); //$NON-NLS-1$ //$NON-NLS-2$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("Should return lowest of multiple BREE versions", lowest, result); //$NON-NLS-1$
	}

	/**
	 * When BREE lists three supported versions in mixed order, the lowest must
	 * be returned.
	 */
	@Test
	public void testBreeThreeVersionsReturnsLowest() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		if (supported.size() < 3) {
			return;
		}
		String[] versions = supported.toArray(new String[0]);
		String low = versions[0];
		String mid = versions[versions.length / 2];
		String high = versions[versions.length - 1];
		// put them in non-ascending order to ensure the logic actually compares
		Map<String, String> manifest = breeManifest(
				"JavaSE-" + high + ", JavaSE-" + low + ", JavaSE-" + mid); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("Should return lowest of three BREE versions", low, result); //$NON-NLS-1$
	}

	// --- BREE: unsupported / old versions ---

	/**
	 * BREE values for Java versions older than 1.8 (unsupported by JDT) must
	 * fall back to the latest supported version.
	 */
	@Test
	public void testBreeUnsupportedVersionFallsBackToLatest() {
		for (String old : new String[] { "J2SE-1.4", "J2SE-1.5", "JavaSE-1.6", "JavaSE-1.7" }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			Map<String, String> manifest = breeManifest(old);
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
			assertEquals("Unsupported BREE " + old + " should fall back to latest", //$NON-NLS-1$ //$NON-NLS-2$
					JavaCore.latestSupportedJavaVersion(), result);
		}
	}

	/**
	 * A mix of an unsupported BREE and a supported one must return the
	 * supported version (not the fallback).
	 */
	@Test
	public void testBreeMixedSupportedAndUnsupportedReturnsSupported() {
		String supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler().first();
		Map<String, String> manifest = breeManifest("J2SE-1.4, JavaSE-" + supported); //$NON-NLS-1$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("Should return supported version from mixed BREE", supported, result); //$NON-NLS-1$
	}

	// --- BREE: nonsense values ---

	/** A completely invalid BREE value must fall back to the latest. */
	@Test
	public void testBreeTotalNonsenseFallsBackToLatest() {
		Map<String, String> manifest = breeManifest("NotAnEE-XYZ, garbage, 42"); //$NON-NLS-1$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals(JavaCore.latestSupportedJavaVersion(), result);
	}

	/** A JavaSE- prefix with a non-numeric suffix that JDT does not know. */
	@Test
	public void testBreeJavaSEUnknownVersionFallsBackToLatest() {
		Map<String, String> manifest = breeManifest("JavaSE-999"); //$NON-NLS-1$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals(JavaCore.latestSupportedJavaVersion(), result);
	}

	// --- Require-Capability: osgi.ee --- all supported versions ---

	/**
	 * For every supported version, a {@code Require-Capability: osgi.ee} filter
	 * of the form {@code (&(osgi.ee=JavaSE)(version=X))} must resolve to that
	 * version.
	 */
	@Test
	public void testRequireCapabilityAllSupportedVersions() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		for (String version : supported) {
			Map<String, String> manifest = eeCapabilityManifest(
					"(&(osgi.ee=JavaSE)(version=" + version + "))"); //$NON-NLS-1$ //$NON-NLS-2$
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
			assertEquals("Require-Capability osgi.ee version=" + version + " should resolve to " + version, //$NON-NLS-1$ //$NON-NLS-2$
					version, result);
		}
	}

	/** A malformed filter string must fall back to the latest. */
	@Test
	public void testRequireCapabilityMalformedFilterFallsBackToLatest() {
		Map<String, String> manifest = eeCapabilityManifest("(not valid ldap filter!!!"); //$NON-NLS-1$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals(JavaCore.latestSupportedJavaVersion(), result);
	}

	/** A filter that does not match any supported version falls back to latest. */
	@Test
	public void testRequireCapabilityNoMatchFallsBackToLatest() {
		Map<String, String> manifest = eeCapabilityManifest("(&(osgi.ee=JavaSE)(version=999))"); //$NON-NLS-1$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals(JavaCore.latestSupportedJavaVersion(), result);
	}

	/**
	 * Two separate osgi.ee entries (version=19 AND version=12) are conjunctive —
	 * the highest version (19) must be returned so the parser understands both.
	 */
	@Test
	public void testRequireCapabilityTwoEntriesReturnsHighestConjunctive() throws IOException, BundleException {
		String manifestContent = "Manifest-Version: 1.0\n" //$NON-NLS-1$
				+ "Bundle-ManifestVersion: 2\n" //$NON-NLS-1$
				+ "Bundle-SymbolicName: test.bundle\n" //$NON-NLS-1$
				+ "Bundle-Version: 1.0.0\n" //$NON-NLS-1$
				+ "Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=19))\",\n" //$NON-NLS-1$
				+ " osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=12))\"\n"; //$NON-NLS-1$
		Map<String, String> manifest = parseManifestString(manifestContent);
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("Two separate osgi.ee entries (19 AND 12) should resolve to highest (19)", "19", result); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Single OR filter within one osgi.ee entry (version=1.8 OR version=11).
	 * Within one filter the lowest matching version is used — 1.8 satisfies the
	 * OR filter first since versions are iterated in ascending order.
	 */
	@Test
	public void testRequireCapabilitySingleOrFilterReturnsLowest() throws IOException, BundleException {
		String manifestContent = "Manifest-Version: 1.0\n" //$NON-NLS-1$
				+ "Bundle-ManifestVersion: 2\n" //$NON-NLS-1$
				+ "Bundle-SymbolicName: test.bundle\n" //$NON-NLS-1$
				+ "Bundle-Version: 1.0.0\n" //$NON-NLS-1$
				+ "Require-Capability: osgi.ee;filter:=\"(| (&(osgi.ee=JavaSE)(version=1.8)) (&(osgi.ee=JavaSE)(version=11)) )\"\n"; //$NON-NLS-1$
		Map<String, String> manifest = parseManifestString(manifestContent);
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		if (supported.contains(JavaCore.VERSION_1_8)) {
			assertEquals("Single OR filter (1.8 OR 11) should resolve to lowest match (1.8)", JavaCore.VERSION_1_8, result); //$NON-NLS-1$
		} else {
			assertEquals("1.8 unsupported, OR filter should resolve to next match (11)", "11", result); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * An osgi.ee entry with no JavaSE version (OSGi/Minimum only) contributes
	 * nothing — must fall back to latestSupportedJavaVersion().
	 */
	@Test
	public void testRequireCapabilityNoJavaVersionFallsBackToLatest() throws IOException, BundleException {
		String manifestContent = "Manifest-Version: 1.0\n" //$NON-NLS-1$
				+ "Bundle-ManifestVersion: 2\n" //$NON-NLS-1$
				+ "Bundle-SymbolicName: test.bundle\n" //$NON-NLS-1$
				+ "Bundle-Version: 1.0.0\n" //$NON-NLS-1$
				+ "Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=OSGi/Minimum)(version=1.2))\"\n"; //$NON-NLS-1$
		Map<String, String> manifest = parseManifestString(manifestContent);
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("No JavaSE version (OSGi/Minimum only) should fall back to latest", //$NON-NLS-1$
				JavaCore.latestSupportedJavaVersion(), result);
	}

	/**
	 * One JavaSE entry (version=17) AND one non-JavaSE entry (OSGi/Minimum).
	 * The non-JavaSE entry contributes nothing — must resolve to "17".
	 */
	@Test
	public void testRequireCapabilityMixedJavaAndNonJavaReturnsJavaVersion() throws IOException, BundleException {
		String manifestContent = "Manifest-Version: 1.0\n" //$NON-NLS-1$
				+ "Bundle-ManifestVersion: 2\n" //$NON-NLS-1$
				+ "Bundle-SymbolicName: test.bundle\n" //$NON-NLS-1$
				+ "Bundle-Version: 1.0.0\n" //$NON-NLS-1$
				+ "Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=17))\",\n" //$NON-NLS-1$
				+ " osgi.ee;filter:=\"(&(osgi.ee=OSGi/Minimum)(version=1.2))\"\n"; //$NON-NLS-1$
		Map<String, String> manifest = parseManifestString(manifestContent);
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("JavaSE 17 AND OSGi/Minimum should resolve to 17", "17", result); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// --- BREE: Java 8 compact profiles ---

	/**
	 * The three Java 8 compact-profile BREE names must all resolve to
	 * {@code "1.8"} when Java 1.8 is still supported, or fall back to the
	 * latest supported version if it has become unsupported.
	 */
	@Test
	public void testBreeCompactProfilesResolveToJava18() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		String expected = supported.contains(JavaCore.VERSION_1_8)
				? JavaCore.VERSION_1_8
				: JavaCore.latestSupportedJavaVersion();
		for (String bree : new String[] {
				"JavaSE/compact1-1.8", //$NON-NLS-1$
				"JavaSE/compact2-1.8", //$NON-NLS-1$
				"JavaSE/compact3-1.8" //$NON-NLS-1$
		}) {
			Map<String, String> manifest = breeManifest(bree);
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
			assertEquals("BREE " + bree + " should resolve to " + expected, expected, result); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * When both BREE and {@code Require-Capability} are present, BREE must win.
	 */
	@Test
	public void testBreeHasPriorityOverRequireCapability() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		if (supported.size() < 2) {
			return;
		}
		String[] versions = supported.toArray(new String[0]);
		String breeVersion = versions[0]; // lowest
		String capVersion  = versions[versions.length - 1]; // highest
		Map<String, String> manifest = new HashMap<>();
		manifest.put("Bundle-RequiredExecutionEnvironment", "JavaSE-" + breeVersion); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put("Require-Capability", //$NON-NLS-1$
				"osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version=" + capVersion + "))\""); //$NON-NLS-1$ //$NON-NLS-2$
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("BREE should take priority over Require-Capability", breeVersion, result); //$NON-NLS-1$
	}

	// --- result is always a supported version ---

	/** Whatever the input, the result must always be in the supported set. */
	@Test
	public void testResultIsAlwaysASupportedVersion() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		List<Map<String, String>> manifests = List.of(
				new HashMap<>(),
				breeManifest("JavaSE-999"), //$NON-NLS-1$
				breeManifest("garbage"), //$NON-NLS-1$
				eeCapabilityManifest("(&(osgi.ee=JavaSE)(version=999))") //$NON-NLS-1$
		);
		// also test null separately since List.of does not allow null elements
		String nullResult = ExecutionEnvironmentResolver.resolveCompliance(null);
		assertNotNull(nullResult);
		assertTrue("null input: result must be a supported Java version", supported.contains(nullResult)); //$NON-NLS-1$
		for (Map<String, String> manifest : manifests) {
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
			assertNotNull(result);
			assertTrue("Result '" + result + "' must be a supported Java version", supported.contains(result)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testAllVersionsFromGetAllVersions() {
		List<String> allVersions = JavaCore.getAllVersions();
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		String latest = JavaCore.latestSupportedJavaVersion();

		assertNotNull("allVersions should not be null", allVersions); //$NON-NLS-1$
		assertFalse("allVersions should not be empty", allVersions.isEmpty()); //$NON-NLS-1$

		for (String version : allVersions) {
			Map<String, String> manifest = breeManifest("JavaSE-" + version); //$NON-NLS-1$
			String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);

			if (supported.contains(version)) {
				// Supported version: should resolve to itself
				assertEquals("BREE JavaSE-" + version + " (supported) should resolve to " + version, //$NON-NLS-1$ //$NON-NLS-2$
						version, result);
			} else {
				// Unsupported version: should fall back to latest supported
				assertEquals("BREE JavaSE-" + version + " (unsupported) should fall back to " + latest, //$NON-NLS-1$ //$NON-NLS-2$
						latest, result);
			}

			// Result must always be in the supported set
			assertTrue("Result '" + result + "' must be a supported Java version for input version " + version, //$NON-NLS-1$ //$NON-NLS-2$
					supported.contains(result));
		}
	}

	/**
	 * Uses the {@code Require-Capability: osgi.ee} filter from the real
	 * {@code bundle.b} test bundle, reduced to the minimum needed for this test.
	 */
	@Test
	public void testRealManifestBundleBRequireCapabilityJava17() throws IOException, BundleException {
		String manifestContent = "Manifest-Version: 1.0\n" //$NON-NLS-1$
				+ "Bundle-ManifestVersion: 2\n" //$NON-NLS-1$
				+ "Bundle-SymbolicName: bundle.b\n" //$NON-NLS-1$
				+ "Bundle-Version: 1.0.0\n" //$NON-NLS-1$
				+ "Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=17))\"\n"; //$NON-NLS-1$
		Map<String, String> manifest = parseManifestString(manifestContent);
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		assertEquals("bundle.b Require-Capability osgi.ee version=17 should resolve to 17", "17", result); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRealManifestDemoJava8ComplexOrFilter() throws IOException, BundleException {
		// minimal manifest — only the header relevant for EE resolution
		String manifestContent = "Manifest-Version: 1.0\n" //$NON-NLS-1$
				+ "Bundle-ManifestVersion: 2\n" //$NON-NLS-1$
				+ "Bundle-SymbolicName: demoMissedSystemModulePackage\n" //$NON-NLS-1$
				+ "Bundle-Version: 1.0.0\n" //$NON-NLS-1$
				+ "Require-Capability: osgi.ee; filter:=\"(| (&(osgi.ee=JavaSE)(version=1.8)) (&(osgi.ee=JavaSE/compact1)(version=1.8)) )\"\n"; //$NON-NLS-1$
		Map<String, String> manifest = parseManifestString(manifestContent);
		String result = ExecutionEnvironmentResolver.resolveCompliance(manifest);
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		if (supported.contains(JavaCore.VERSION_1_8)) {
			assertEquals("Complex OR filter with JavaSE 1.8 should resolve to 1.8", JavaCore.VERSION_1_8, result); //$NON-NLS-1$
		} else {
			// 1.8 became unsupported — fallback to latest
			assertEquals("1.8 no longer supported, should fall back to latest", //$NON-NLS-1$
					JavaCore.latestSupportedJavaVersion(), result);
		}
	}

	// --- helpers ---

	/**
	 * Parses a manifest from a string literal.
	 */
	private static Map<String, String> parseManifestString(String content) throws IOException, BundleException {
		try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
			return ManifestElement.parseBundleManifest(in, null);
		}
	}

	private static Map<String, String> breeManifest(String breeValue) {
		Map<String, String> map = new HashMap<>();
		map.put("Bundle-RequiredExecutionEnvironment", breeValue); //$NON-NLS-1$
		return map;
	}

	private static Map<String, String> eeCapabilityManifest(String filter) {
		Map<String, String> map = new HashMap<>();
		map.put("Require-Capability", "osgi.ee; filter:=\"" + filter + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return map;
	}
}
