/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
 * Delta tests for interface
 */

public class InterfaceDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "interface"; //$NON-NLS-1$
	}

	/**
	 * delete API method
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * delete API field
	 */
	@Test
	public void test2() {
		deployBundles("test2"); //$NON-NLS-1$
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Reorder of members
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
		assertTrue(delta.isEmpty(), "Not empty"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Different from NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Add type parameter
	 */
	@Test
	public void test4() {
		deployBundles("test4"); //$NON-NLS-1$
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
		assertEquals(IDelta.TYPE_PARAMETERS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add type parameter
	 */
	@Test
	public void test5() {
		deployBundles("test5"); //$NON-NLS-1$
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
		assertEquals(IDelta.TYPE_PARAMETER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Delete type parameter
	 */
	@Test
	public void test6() {
		deployBundles("test6"); //$NON-NLS-1$
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Delete type parameter
	 */
	@Test
	public void test7() {
		deployBundles("test7"); //$NON-NLS-1$
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Reorder of type parameter
	 */
	@Test
	public void test8() {
		deployBundles("test8"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(4, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_BOUND, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_BOUND, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_BOUND, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[3];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_BOUND, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Rename of type parameter
	 */
	@Test
	public void test9() {
		deployBundles("test9"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_NAME, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_NAME, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add super interface
	 */
	@Test
	public void test10() {
		deployBundles("test10"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove super interface
	 */
	@Test
	public void test11() {
		deployBundles("test11"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove super interface
	 */
	@Test
	public void test12() {
		deployBundles("test12"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add super interface
	 */
	@Test
	public void test13() {
		deployBundles("test13"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that cannot be implemented
	 */
	@Test
	public void test14() {
		deployBundles("test14"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Should be compatible"); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that cannot be extended
	 */
	@Test
	public void test30() {
		deployBundles("test30"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "Not extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Should not be compatible"); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that cannot be extended
	 */
	@Test
	public void test31() {
		deployBundles("test31"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Should be compatible"); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that can be implemented
	 */
	@Test
	public void test15() {
		deployBundles("test15"); //$NON-NLS-1$
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
		assertFalse(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy with noimplement only
	 */
	@Test
	public void test16() {
		deployBundles("test16"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Method Add not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy with noextend only
	 */
	@Test
	public void test32() {
		deployBundles("test32"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "Not extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Method Add not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy with noextend and noimplement
	 */
	@Test
	public void test33() {
		deployBundles("test33"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Method Add compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy
	 */
	@Test
	public void test17() {
		deployBundles("test17"); //$NON-NLS-1$
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
		assertFalse(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Is implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add a member type
	 */
	@Test
	public void test18() {
		deployBundles("test18"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add a member types
	 */
	@Test
	public void test19() {
		deployBundles("test19"); //$NON-NLS-1$
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
		assertFalse(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Is implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add member types
	 */
	@Test
	public void test20() {
		deployBundles("test20"); //$NON-NLS-1$
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
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Not implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove member types
	 */
	@Test
	public void test21() {
		deployBundles("test21"); //$NON-NLS-1$
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add member types
	 */
	@Test
	public void test22() {
		deployBundles("test22"); //$NON-NLS-1$
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
		assertFalse(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Is implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Is implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Move field up in hierarchy
	 */
	@Test
	public void test23() {
		deployBundles("test23"); //$NON-NLS-1$
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
		assertFalse(RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions()), "Is implement restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Added one parameter to an API method (=> addition and removal of a
	 * method)
	 */
	@Test
	public void test24() {
		deployBundles("test24"); //$NON-NLS-1$
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
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
	@Test
	public void test25() {
		deployBundles("test25"); //$NON-NLS-1$
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
		assertEquals(IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=507246
	 */
	@Test
	public void test29() {
		deployBundles("test29"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after,
				VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(3, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.OVERRIDEN_METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.SUPER_INTERFACE_DEFAULT_METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
	@Test
	public void test26() {
		deployBundles("test26"); //$NON-NLS-1$
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
		assertEquals(IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Method Add not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Changed superinterfaces set not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	@Test
	public void test34() {
		deployBundles("test34"); //$NON-NLS-1$
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
		assertEquals(IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Method Add not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Changed superinterfaces set not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	@Test
	public void test35() {
		deployBundles("test35"); //$NON-NLS-1$
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
		assertEquals(IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Method Add compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Changed superinterfaces set not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
	@Test
	public void test27() {
		deployBundles("test27"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(3, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
	@Test
	public void test28() {
		deployBundles("test28"); //$NON-NLS-1$
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
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Method Add compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	@Test
	public void test36() {
		deployBundles("test36"); //$NON-NLS-1$
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
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Method Add compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	@Test
	public void test37() {
		deployBundles("test37"); //$NON-NLS-1$
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
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Method Add not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_MOVED_UP, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	@Test
	public void test38() {
		deployBundles("test38"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		assertFalse(beforeApiComponent.hasApiDescription());
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Added deprecation
	 */
	@Test
	public void test39() {
		deployBundles("test39"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DEPRECATION, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Removed deprecation
	 */
	@Test
	public void test40() {
		deployBundles("test40"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DEPRECATION, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove @noimplement and add new methods
	 */
	@Test
	public void test41() {
		deployBundles("test41"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add a default method to a @noimplement interface - compatible change
	 */
	@Test
	public void test42() {
		deployBundles("test42"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DEFAULT_METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add default method from new superinterface to a @noimplement interface - compatible change
	 */
	@Test
	public void test43() {
		deployBundles("test43"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after,
				VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(3, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.OVERRIDEN_METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.SUPER_INTERFACE_DEFAULT_METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}
}
