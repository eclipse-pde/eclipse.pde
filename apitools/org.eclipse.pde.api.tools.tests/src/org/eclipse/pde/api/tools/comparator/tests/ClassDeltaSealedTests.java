/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
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
package org.eclipse.pde.api.tools.comparator.tests;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertNotNull;

import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.junit.Test;

/**
 * Observes what deltas the API comparator produces for three scenarios,
 * all starting from a baseline with a {@code final} GC class with a
 * {@code @noreference} handle field:
 *
 * <ul>
 *   <li>test100: final GC + handle → final GC + handle (no modifier change)</li>
 *   <li>test101: final GC + handle → sealed GC + handle + GCExtension</li>
 *   <li>test102: final GC + handle → normal GC + handle + GCExtension</li>
 * </ul>
 *
 * All tests compile with Java 17.
 * The tests never fail — they print the resulting deltas to System.out.
 */
public class ClassDeltaSealedTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "class"; //$NON-NLS-1$
	}

	@Override
	protected String[] getCompilerOptions() {
		return new String[] { "-17", "-preserveAllLocals", "-nowarn" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Scenario 1: final GC + handle → final GC + handle (no modifier change).
	 */
	@Test
	public void test100() {
		observeDeltas("test100"); //$NON-NLS-1$
		fail("show"); //$NON-NLS-1$
	}

	/**
	 * Scenario 2: final GC + handle → sealed GC + handle + GCExtension.
	 */
	@Test
	public void test101() {
		observeDeltas("test101"); //$NON-NLS-1$
		fail("show"); //$NON-NLS-1$
	}

	/**
	 * Scenario 3: final GC + handle → normal GC + handle + GCExtension.
	 */
	@Test
	public void test102() {
		observeDeltas("test102"); //$NON-NLS-1$
		fail("show"); //$NON-NLS-1$

	}

	private void observeDeltas(String testName) {
		deployBundles(testName);
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeComp = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no before api component", beforeComp); //$NON-NLS-1$
		IApiComponent afterComp = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no after api component", afterComp); //$NON-NLS-1$

		IDelta delta = ApiComparator.compare(beforeComp, afterComp, before, after,
				VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$

		System.out.println("=== Deltas for " + testName + " ==="); //$NON-NLS-1$ //$NON-NLS-2$
		if (delta == ApiComparator.NO_DELTA) {
			System.out.println("  NO_DELTA"); //$NON-NLS-1$
			return;
		}
		IDelta[] leaves = collectLeaves(delta);
		System.out.println("  count=" + leaves.length); //$NON-NLS-1$
		for (IDelta leaf : leaves) {
			System.out.println("  kind=" + leaf.getKind() //$NON-NLS-1$
					+ " flags=" + leaf.getFlags() //$NON-NLS-1$
					+ " elementType=" + leaf.getElementType() //$NON-NLS-1$
					+ " key=" + leaf.getKey() //$NON-NLS-1$
					+ " msg=" + leaf.getMessage()); //$NON-NLS-1$
		}
	}
}
