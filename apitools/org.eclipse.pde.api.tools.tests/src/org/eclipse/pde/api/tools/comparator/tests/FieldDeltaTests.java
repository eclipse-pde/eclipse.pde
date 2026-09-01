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

import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.junit.jupiter.api.Test;

/**
 * Delta tests for field
 */

public class FieldDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "field"; //$NON-NLS-1$
	}

	/**
	 * Check change field type (interface)
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field visibility - default to public
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
		assertEquals(IDelta.INCREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field visibility - private to public
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
		assertEquals(IDelta.INCREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
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
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL_NON_STATIC, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
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
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_FINAL_TO_FINAL, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
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
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field modifiers - static to non static
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.STATIC_TO_NON_STATIC, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field modifiers - non static to static
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_STATIC_TO_STATIC, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field modifiers - transient to non transient
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TRANSIENT_TO_NON_TRANSIENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field modifiers - non transient to transient
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_TRANSIENT_TO_TRANSIENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field value (interface)
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Decrease access
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Decrease access
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed static non final to static final
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
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_FINAL_TO_FINAL, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
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
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed value of non-visible field (default)
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
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
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed value of non-visible field (private)
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Added value of non-visible field (private)
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
		assertEquals(2, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.NON_FINAL_TO_FINAL, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed value of non-visible field (protected with extend restrictions)
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "No extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed List&lt;Integer&gt; to List&lt;String&gt;
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field type (class)
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed Map&lt;String, Integer&gt; to Map&lt;String, String&gt;
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed Map to Map&lt;String, String&gt;
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
		assertEquals(IDelta.TYPE_ARGUMENTS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed List&lt;String&gt; to ArrayList&lt;String&gt;
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed ArrayList&lt;String&gt; to ArrayList
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed X&lt;String, Integer, Number&gt; to X&lt;Integer, String,
	 * Number&gt;
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=218976
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "No extend restrictions"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=222905
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
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Should be compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getOldModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=222905
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=222905
	 */
	@Test
	public void test38() {
		deployBundles("test38"); //$NON-NLS-1$
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
		assertFalse(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove compile-time constant
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=224994
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(3, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field value (class)
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Field addition
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(6, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[3];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[4];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[5];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=225164
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions()), "Wrong restrictions"); //$NON-NLS-1$
		assertTrue(Flags.isProtected(child.getNewModifiers()), "Wrong modifier"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Added type arguments List&lt;String&gt;
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENTS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field modifiers - volatile to non volatile
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VOLATILE_TO_NON_VOLATILE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field modifiers - non volatile to volatile
	 */
	@Test
	public void test44() {
		deployBundles("test44"); //$NON-NLS-1$
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
		assertEquals(IDelta.NON_VOLATILE_TO_VOLATILE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}


	/**
	 * Tag one existing field with @noreference
	 */
	@Test
	public void test45() {
		deployBundles("test45"); //$NON-NLS-1$
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
		assertEquals(IDelta.API_FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Adding a field with @noreference
	 */
	@Test
	public void test46() {
		deployBundles("test46"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta.isEmpty(), "Not empty"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Different from NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Adding a field with @noreference
	 */
	@Test
	public void test47() {
		deployBundles("test47"); //$NON-NLS-1$
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
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
	}

	/**
	 * Removing @noreference on an existing field
	 */
	@Test
	public void test48() {
		deployBundles("test48"); //$NON-NLS-1$
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
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
	}

	/**
	 * Removing field tagged as @noreference
	 */
	@Test
	public void test49() {
		deployBundles("test49"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Not NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Check change field value (class) - no delta
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
		assertTrue(delta.isEmpty(), "Not empty"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Different from NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Removing field tagged as @noreference
	 */
	@Test
	public void test50() {
		deployBundles("test50"); //$NON-NLS-1$
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
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Tag one existing protected field with @noreference
	 */
	@Test
	public void test51() {
		deployBundles("test51"); //$NON-NLS-1$
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
		assertEquals(IDelta.API_FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Removing @noreference on an existing field
	 */
	@Test
	public void test52() {
		deployBundles("test52"); //$NON-NLS-1$
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
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Compatible"); //$NON-NLS-1$
	}

	/**
	 * Removing @noreference on an existing field
	 */
	@Test
	public void test53() {
		deployBundles("test53"); //$NON-NLS-1$
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
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check change field type (class) for protected field inside class tagged
	 * as @noextend
	 */
	@Test
	public void test54() {
		deployBundles("test54"); //$NON-NLS-1$
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
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove method from internal super class with protected members (extend
	 * restriction)
	 */
	@Test
	public void test55() {
		deployBundles("test55"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Different from NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Changed Y&lt;Integer, String&gt; to Y&lt;String&gt;
	 */
	@Test
	public void test56() {
		deployBundles("test56"); //$NON-NLS-1$
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
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed Y&lt;String&gt; to Y&lt;Integer, String&gt;
	 */
	@Test
	public void test57() {
		deployBundles("test57"); //$NON-NLS-1$
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
		assertEquals(IDelta.TYPE_PARAMETER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_ARGUMENT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Private field to @noreference public field
	 */
	@Test
	public void test58() {
		deployBundles("test58"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertTrue(delta == ApiComparator.NO_DELTA, "Different from NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Private field to @noreference public field
	 */
	@Test
	public void test59() {
		deployBundles("test59"); //$NON-NLS-1$
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
		assertEquals(IDelta.INCREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @noreferece restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check decrease field visibility - public to protected
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244995
	 */
	@Test
	public void test60() {
		deployBundles("test60"); //$NON-NLS-1$
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
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test61() {
		deployBundles("test61"); //$NON-NLS-1$
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
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_FINAL_TO_FINAL, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test62() {
		deployBundles("test62"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_STATIC_TO_STATIC, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test63() {
		deployBundles("test63"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.STATIC_TO_NON_STATIC, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test64() {
		deployBundles("test64"); //$NON-NLS-1$
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
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.NON_FINAL_TO_FINAL, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test65() {
		deployBundles("test65"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test66() {
		deployBundles("test66"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test67() {
		deployBundles("test67"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test68() {
		deployBundles("test68"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244993
	 */
	@Test
	public void test69() {
		deployBundles("test69"); //$NON-NLS-1$
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getNewModifiers()), "Not visible"); //$NON-NLS-1$
		assertTrue(RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions()), "Not @reference restriction"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check decrease field visibility - public to default
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
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244994
	 */
	@Test
	public void test70() {
		deployBundles("test70"); //$NON-NLS-1$
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
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getOldModifiers()), "Not visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	@Test
	public void test71() {
		deployBundles("test71"); //$NON-NLS-1$
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
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getOldModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	@Test
	public void test72() {
		deployBundles("test72"); //$NON-NLS-1$
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
		assertTrue(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(!DeltaProcessor.isCompatible(child), "Should not be compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getOldModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244994
	 */
	@Test
	public void test73() {
		deployBundles("test73"); //$NON-NLS-1$
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
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertTrue(Util.isVisible(child.getOldModifiers()), "Not visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244994
	 */
	@Test
	public void test74() {
		deployBundles("test74"); //$NON-NLS-1$
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
		assertEquals(IDelta.CLINIT, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getOldModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244994
	 */
	@Test
	public void test75() {
		deployBundles("test75"); //$NON-NLS-1$
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
		assertEquals(IDelta.VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getNewModifiers()), "Is visible"); //$NON-NLS-1$
		assertFalse(Util.isVisible(child.getOldModifiers()), "Is visible"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=277925
	 */
	@Test
	public void test76() {
		deployBundles("test76"); //$NON-NLS-1$
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
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=277925
	 */
	@Test
	public void test77() {
		deployBundles("test77"); //$NON-NLS-1$
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
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Check decrease field visibility - public to private
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DECREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Check increase field visibility - protected to public
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
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.INCREASE_ACCESS, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}
}