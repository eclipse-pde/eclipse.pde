/*******************************************************************************
 * Copyright (c) 2026 Lakshminarayana Nekkanti and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lakshminarayana Nekkanti - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.ui.util.BundleResourceLocator;
import org.eclipse.pde.internal.ui.util.BundleResourceLocator.BundleResourceRef;
import org.junit.Test;

/**
 * Tests the notations {@link BundleResourceLocator} accepts for the value of a
 * resource attribute, for example the <code>icon</code> of an extension.
 */
public class BundleResourceLocatorTest {

	private static final String ICON = "icons/obj16/plugin_mf_obj.svg"; //$NON-NLS-1$

	@Test
	public void testPlainPathBelongsToTheEditedBundle() {
		BundleResourceRef ref = parse(ICON);
		assertNull(ref.bundleId());
		assertEquals(List.of(ICON), ref.candidatePaths());
	}

	@Test
	public void testLeadingSeparatorIsKept() {
		// resolved below the bundle root first, then as /<project>/<path>
		assertEquals(List.of('/' + ICON), parse('/' + ICON).candidatePaths());
	}

	@Test
	public void testPlatformPluginUrl() {
		BundleResourceRef ref = parse("platform:/plugin/com.example.bundle/" + ICON); //$NON-NLS-1$
		assertEquals("com.example.bundle", ref.bundleId()); //$NON-NLS-1$
		assertEquals(List.of(ICON), ref.candidatePaths());
	}

	@Test
	public void testPlatformFragmentUrl() {
		BundleResourceRef ref = parse("platform:/fragment/com.example.fragment/" + ICON); //$NON-NLS-1$
		assertEquals("com.example.fragment", ref.bundleId()); //$NON-NLS-1$
		assertEquals(List.of(ICON), ref.candidatePaths());
	}

	@Test
	public void testPlatformBaseUrl() {
		BundleResourceRef ref = parse("platform:/base/plugin/com.example.bundle/" + ICON); //$NON-NLS-1$
		assertEquals("com.example.bundle", ref.bundleId()); //$NON-NLS-1$
		assertEquals(List.of(ICON), ref.candidatePaths());
	}

	@Test
	public void testIncompletePlatformUrls() {
		assertNull(BundleResourceLocator.parse("platform:/plugin/com.example.bundle")); //$NON-NLS-1$
		assertNull(BundleResourceLocator.parse("platform:/plugin/")); //$NON-NLS-1$
		assertNull(BundleResourceLocator.parse("platform:/resource/com.example.bundle/" + ICON)); //$NON-NLS-1$
	}

	@Test
	public void testNlVariableIsExpandedMostSpecificFirst() {
		List<String> candidates = parse("$nl$/" + ICON).candidatePaths(); //$NON-NLS-1$

		String[] nl = TargetPlatform.getNL().split("_"); //$NON-NLS-1$
		List<String> expected = new ArrayList<>();
		if (nl.length > 1) {
			expected.add("nl/" + nl[0] + '/' + nl[1] + '/' + ICON); //$NON-NLS-1$
		}
		expected.add("nl/" + nl[0] + '/' + ICON); //$NON-NLS-1$
		expected.add('/' + ICON);
		assertEquals(expected, candidates);
	}

	@Test
	public void testNlVariableInsidePlatformUrl() {
		BundleResourceRef ref = parse("platform:/plugin/com.example.bundle/$nl$/" + ICON); //$NON-NLS-1$
		assertEquals("com.example.bundle", ref.bundleId()); //$NON-NLS-1$
		assertTrue(ref.candidatePaths().contains('/' + ICON));
		assertEquals('/' + ICON, ref.candidatePaths().get(ref.candidatePaths().size() - 1));
	}

	@Test
	public void testOsAndWsVariables() {
		List<String> osCandidates = parse("$os$/" + ICON).candidatePaths(); //$NON-NLS-1$
		assertEquals("os/" + TargetPlatform.getOS() + '/' + TargetPlatform.getOSArch() + '/' + ICON, //$NON-NLS-1$
				osCandidates.get(0));
		assertEquals("os/" + TargetPlatform.getOS() + '/' + ICON, osCandidates.get(1)); //$NON-NLS-1$
		assertEquals('/' + ICON, osCandidates.get(osCandidates.size() - 1));

		List<String> wsCandidates = parse("$ws$/" + ICON).candidatePaths(); //$NON-NLS-1$
		assertEquals(List.of("ws/" + TargetPlatform.getWS() + '/' + ICON, '/' + ICON), wsCandidates); //$NON-NLS-1$
	}

	@Test
	public void testValueIsNormalized() {
		assertEquals(List.of(ICON), parse("  " + ICON + "  ").candidatePaths()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(List.of(ICON), parse("icons//obj16//plugin_mf_obj.svg").candidatePaths()); //$NON-NLS-1$
		// a platform URL is a URL, its scheme is not case sensitive
		assertEquals("com.example.bundle", parse("PLATFORM:/plugin/com.example.bundle/" + ICON).bundleId()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testUnsupportedAndEmptyValues() {
		assertNull(BundleResourceLocator.parse(null));
		assertNull(BundleResourceLocator.parse("")); //$NON-NLS-1$
		assertNull(BundleResourceLocator.parse("   ")); //$NON-NLS-1$
		assertNull(BundleResourceLocator.parse("http://example.org/icon.svg")); //$NON-NLS-1$
		assertNull(BundleResourceLocator.parse("bundleentry://42/icons/icon.svg")); //$NON-NLS-1$
	}

	private static BundleResourceRef parse(String value) {
		BundleResourceRef ref = BundleResourceLocator.parse(value);
		assertNotNull("no reference parsed out of " + value, ref); //$NON-NLS-1$
		return ref;
	}
}
