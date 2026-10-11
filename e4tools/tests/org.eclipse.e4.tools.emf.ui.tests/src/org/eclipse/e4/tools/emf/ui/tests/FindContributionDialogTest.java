/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider.ContributionData;
import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs.FindContributionDialog;
import org.junit.jupiter.api.Test;

public class FindContributionDialogTest {

	private static ContributionData data(String bundle, String className) {
		return new ContributionData(bundle, className, "Java", null); //$NON-NLS-1$
	}

	@Test
	public void testBundleWithSeveralClassesIsShownOnce() {
		ContributionData first = data("a.bundle", "a.First"); //$NON-NLS-1$ //$NON-NLS-2$
		ContributionData second = data("a.bundle", "a.Second"); //$NON-NLS-1$ //$NON-NLS-2$
		ContributionData other = data("b.bundle", "b.Only"); //$NON-NLS-1$ //$NON-NLS-2$
		List<ContributionData> list = new ArrayList<>(List.of(first, second, other));

		assertTrue(FindContributionDialog.isFirstWithKey(list, first, d -> d.bundleName, d -> true));
		assertFalse(FindContributionDialog.isFirstWithKey(list, second, d -> d.bundleName, d -> true));
		assertTrue(FindContributionDialog.isFirstWithKey(list, other, d -> d.bundleName, d -> true));
	}

	@Test
	public void testPackageIsShownOnceAndSubpackagesAreDistinct() {
		ContributionData first = data("a.bundle", "a.p.First"); //$NON-NLS-1$ //$NON-NLS-2$
		ContributionData second = data("a.bundle", "a.p.Second"); //$NON-NLS-1$ //$NON-NLS-2$
		ContributionData sub = data("a.bundle", "a.p.sub.Third"); //$NON-NLS-1$ //$NON-NLS-2$
		List<ContributionData> list = new ArrayList<>(List.of(first, second, sub));

		assertTrue(FindContributionDialog.isFirstWithKey(list, first, d -> FindContributionDialog.packageOf(d.className), d -> true));
		assertFalse(FindContributionDialog.isFirstWithKey(list, second, d -> FindContributionDialog.packageOf(d.className), d -> true));
		assertTrue(FindContributionDialog.isFirstWithKey(list, sub, d -> FindContributionDialog.packageOf(d.className), d -> true));
	}

	@Test
	public void testCandidatesRestrictTheComparison() {
		ContributionData hidden = data("a.bundle", "p.First"); //$NON-NLS-1$ //$NON-NLS-2$
		ContributionData shown = data("b.bundle", "p.Second"); //$NON-NLS-1$ //$NON-NLS-2$
		List<ContributionData> list = new ArrayList<>(List.of(hidden, shown));

		assertTrue(FindContributionDialog.isFirstWithKey(list, shown, d -> FindContributionDialog.packageOf(d.className),
				d -> "b.bundle".equals(d.bundleName))); //$NON-NLS-1$
	}

	@Test
	public void testEntriesWithoutBundleNameAreGroupedTogether() {
		ContributionData first = data(null, "a.First"); //$NON-NLS-1$
		ContributionData second = data(null, "b.Second"); //$NON-NLS-1$
		ContributionData named = data("a.bundle", "c.Third"); //$NON-NLS-1$ //$NON-NLS-2$
		List<ContributionData> list = new ArrayList<>(List.of(first, second, named));

		assertTrue(FindContributionDialog.isFirstWithKey(list, first, d -> d.bundleName, d -> true));
		assertFalse(FindContributionDialog.isFirstWithKey(list, second, d -> d.bundleName, d -> true));
		assertTrue(FindContributionDialog.isFirstWithKey(list, named, d -> d.bundleName, d -> true));
	}

	@Test
	public void testPackageOf() {
		assertEquals("org.example", FindContributionDialog.packageOf("org.example.Handler")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("", FindContributionDialog.packageOf("Handler")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
