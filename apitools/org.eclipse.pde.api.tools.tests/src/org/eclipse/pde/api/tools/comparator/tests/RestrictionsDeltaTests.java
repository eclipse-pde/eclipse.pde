/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.junit.jupiter.api.Test;

/**
 * Delta tests for restrictions delta
 */
public class RestrictionsDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "restrictions"; //$NON-NLS-1$
	}

	/**
	 * Change restrictions
	 */
	@Test
	public void test1() {
		deployBundles("test1"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "Extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$

	}

	/**
	 * Add restrictions
	 */
	@Test
	public void test2() {
		deployBundles("test3"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "Extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add restrictions
	 */
	@Test
	public void test3() {
		deployBundles("test3"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "Extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add extend restrictions
	 */
	@Test
	public void test4() {
		deployBundles("test4"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add implement restrictions
	 */
	@Test
	public void test5() {
		deployBundles("test5"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove @noextend on a final class
	 */
	@Test
	public void test6() {
		deployBundles("test6"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Should be NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Remove @noinstantiate on an abstract class
	 */
	@Test
	public void test7() {
		deployBundles("test7"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Should be NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Remove @noinstantiate on an abstract class
	 */
	@Test
	public void test8() {
		deployBundles("test8"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Should be NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Remove @noextend on a final class
	 */
	@Test
	public void test9() {
		deployBundles("test9"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Should be NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Remove @noextend on a non-final class (see 247291)
	 */
	@Test
	public void test10() {
		deployBundles("test10"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove @noimplement on an interface
	 */
	@Test
	public void test11() {
		deployBundles("test11"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add @noextend on a final class and remove final on the new version of the
	 * class 247654
	 */
	@Test
	public void test12() {
		deployBundles("test12"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add @noinstantiate on an abstract class and remove abstract on the new
	 * version of the class 247654
	 */
	@Test
	public void test13() {
		deployBundles("test13"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ABSTRACT_TO_NON_ABSTRACT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add extend restrictions
	 */
	@Test
	public void test14() {
		deployBundles("test14"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(beforeApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		assertTrue(afterApiComponent.hasApiDescription(), "Has no description"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.RESTRICTIONS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}
}
