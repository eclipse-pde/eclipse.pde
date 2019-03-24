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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.junit.Test;

/**
 * Delta tests for method
 */
public class MethodDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "method"; //$NON-NLS-1$
	}

	/**
	 * Change method body
	 */
	@Test
	public void test1() {
		deployBundles("test1"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Not empty", delta.isEmpty()); //$NON-NLS-1$
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * rename method parameter
	 */
	@Test
	public void test2() {
		deployBundles("test2"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Not empty", delta.isEmpty()); //$NON-NLS-1$
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Change method name
	 */
	@Test
	public void test3() {
		deployBundles("test3"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add formal parameter
	 */
	@Test
	public void test4() {
		deployBundles("test4"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Delete formal parameter
	 */
	@Test
	public void test5() {
		deployBundles("test5"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Change type of formal parameter
	 */
	@Test
	public void test6() {
		deployBundles("test6"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Change result type
	 */
	@Test
	public void test7() {
		deployBundles("test7"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add checked exception
	 */
	@Test
	public void test8() {
		deployBundles("test8"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add unchecked exception
	 */
	@Test
	public void test9() {
		deployBundles("test9"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.UNCHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove checked exception
	 */
	@Test
	public void test10() {
		deployBundles("test10"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove unchecked exception
	 */
	@Test
	public void test11() {
		deployBundles("test11"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.UNCHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Reorder list of thrown exceptions
	 */
	@Test
	public void test12() {
		deployBundles("test12"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Not empty", delta.isEmpty()); //$NON-NLS-1$
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Decrease visibility
	 */
	@Test
	public void test13() {
		deployBundles("test13"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Decrease visibility
	 */
	@Test
	public void test14() {
		deployBundles("test14"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Decrease visibility
	 */
	@Test
	public void test15() {
		deployBundles("test15"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Decrease visibility
	 */
	@Test
	public void test16() {
		deployBundles("test16"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Decrease visibility
	 */
	@Test
	public void test17() {
		deployBundles("test17"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Increase visibility
	 */
	@Test
	public void test18() {
		deployBundles("test18"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Increase visibility
	 */
	@Test
	public void test19() {
		deployBundles("test19"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Increase visibility
	 */
	@Test
	public void test20() {
		deployBundles("test20"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Abstract to non-abstract
	 */
	@Test
	public void test21() {
		deployBundles("test21"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.ABSTRACT_TO_NON_ABSTRACT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * non-abstract to abstract
	 */
	@Test
	public void test22() {
		deployBundles("test22"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_ABSTRACT_TO_ABSTRACT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * final to non-final
	 */
	@Test
	public void test23() {
		deployBundles("test23"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * non-final to final
	 */
	@Test
	public void test24() {
		deployBundles("test24"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * static to non-static
	 */
	@Test
	public void test25() {
		deployBundles("test25"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.STATIC_TO_NON_STATIC, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * non-static to static
	 */
	@Test
	public void test26() {
		deployBundles("test26"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Was visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_STATIC_TO_STATIC, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * native to non-native
	 */
	@Test
	public void test27() {
		deployBundles("test27"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NATIVE_TO_NON_NATIVE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * non-native to native
	 */
	@Test
	public void test28() {
		deployBundles("test28"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_NATIVE_TO_NATIVE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * synchronized to non-synchronized
	 */
	@Test
	public void test29() {
		deployBundles("test29"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * non-synchronized to synchronized
	 */
	@Test
	public void test30() {
		deployBundles("test30"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add new type parameter
	 */
	@Test
	public void test31() {
		deployBundles("test31"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETERS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add new type parameter to a method that did not have a type parameter but
	 * that had a generic signature.
	 */
	@Test
	public void test31a() {
		deployBundles("test31a"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETERS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		assertEquals("foo<U:Ljava/lang/Number;>(TU;Ljava/util/List<TU;>;)V", child.getKey()); //$NON-NLS-1$
	}

	/**
	 * Add another type parameter
	 */
	@Test
	public void test32() {
		deployBundles("test32"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		assertEquals("foo<U:Ljava/lang/Object;V:Ljava/lang/Object;>(TU;)V", child.getKey()); //$NON-NLS-1$
	}

	/**
	 * Delete type parameters
	 */
	@Test
	public void test33() {
		deployBundles("test33"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Delete type parameter
	 */
	@Test
	public void test34() {
		deployBundles("test34"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Rename type parameter
	 */
	@Test
	public void test35() {
		deployBundles("test35"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Reorder type parameters + changed class bound and interface bound
	 */
	@Test
	public void test36() {
		deployBundles("test36"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 6, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CLASS_BOUND, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INTERFACE_BOUND, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[4];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CLASS_BOUND, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[5];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INTERFACE_BOUND, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Change last parameter from array to varargs
	 */
	@Test
	public void test37() {
		deployBundles("test37"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.ARRAY_TO_VARARGS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Change last parameter from varargs to array
	 */
	@Test
	public void test38() {
		deployBundles("test38"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.VARARGS_TO_ARRAY, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Removed unchecked exception
	 */
	@Test
	public void test39() {
		deployBundles("test39"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.UNCHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Removed checked exception
	 */
	@Test
	public void test40() {
		deployBundles("test40"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add checked exception
	 */
	@Test
	public void test41() {
		deployBundles("test41"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Added unchecked exception
	 */
	@Test
	public void test42() {
		deployBundles("test42"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.UNCHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Added default value
	 */
	@Test
	public void test43() {
		deployBundles("test43"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Removed default value
	 */
	@Test
	public void test44() {
		deployBundles("test44"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Changed default value
	 */
	@Test
	public void test45() {
		deployBundles("test45"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * non-final to final
	 */
	@Test
	public void test46() {
		deployBundles("test46"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * change return type of a package visible method
	 */
	@Test
	public void test47() {
		deployBundles("test47"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add checked exception
	 */
	@Test
	public void test48() {
		deployBundles("test48"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Added abstract method
	 */
	@Test
	public void test49() {
		deployBundles("test49"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Changes in a non-visible type should not report delta when only API is
	 * requested
	 */
	@Test
	public void test50() {
		deployBundles("test50"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("No NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Changes in a visible type should report delta when only API is requested
	 */
	@Test
	public void test51() {
		deployBundles("test51"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Changes in a visible type should report delta when only API is requested
	 * with extend restriction
	 */
	@Test
	public void test52() {
		deployBundles("test52"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("No extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Changes in a visible type should report delta when only API is requested
	 */
	@Test
	public void test53() {
		deployBundles("test53"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add static to a private method
	 */
	@Test
	public void test54() {
		deployBundles("test54"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_STATIC_TO_STATIC, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=226128
	 */
	@Test
	public void test55() {
		deployBundles("test55"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=226128
	 */
	@Test
	public void test56() {
		deployBundles("test56"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		assertTrue("Not private", Flags.isPrivate(child.getNewModifiers())); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228209
	 */
	@Test
	public void test57() {
		deployBundles("test57"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228209
	 */
	@Test
	public void test58() {
		deployBundles("test58"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228075
	 */
	@Test
	public void test59() {
		deployBundles("test59"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228075
	 */
	@Test
	public void test60() {
		deployBundles("test60"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228209
	 */
	@Test
	public void test61() {
		deployBundles("test61"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228209
	 */
	@Test
	public void test62() {
		deployBundles("test62"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove @noreference on an existing method
	 */
	@Test
	public void test63() {
		deployBundles("test63"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an existing method
	 */
	@Test
	public void test64() {
		deployBundles("test64"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new method
	 */
	@Test
	public void test65() {
		deployBundles("test65"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new method
	 */
	@Test
	public void test66() {
		deployBundles("test66"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove @noreference on an existing constructor
	 */
	@Test
	public void test67() {
		deployBundles("test67"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CONSTRUCTOR, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an existing constructor
	 */
	@Test
	public void test68() {
		deployBundles("test68"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_CONSTRUCTOR, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new constructor
	 */
	@Test
	public void test69() {
		deployBundles("test69"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new constructor
	 */
	@Test
	public void test70() {
		deployBundles("test70"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CONSTRUCTOR, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove @noreference on an existing annotation method
	 */
	@Test
	public void test71() {
		deployBundles("test71"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an existing annotation method
	 */
	@Test
	public void test72() {
		deployBundles("test72"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_METHOD_WITH_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new annotation method
	 */
	@Test
	public void test73() {
		deployBundles("test73"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new annotation method
	 */
	@Test
	public void test74() {
		deployBundles("test74"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove @noreference on an existing annotation method
	 */
	@Test
	public void test75() {
		deployBundles("test75"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an existing annotation method
	 */
	@Test
	public void test76() {
		deployBundles("test76"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new annotation method
	 */
	@Test
	public void test77() {
		deployBundles("test77"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Add @noreference on an new annotation method
	 */
	@Test
	public void test78() {
		deployBundles("test78"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * 229688
	 */
	@Test
	public void test79() {
		deployBundles("test79"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SUPERCLASS, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_DOWN, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * 244673
	 */
	@Test
	public void test80() {
		deployBundles("test80"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * 244673
	 */
	@Test
	public void test81() {
		deployBundles("test81"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @noreferece restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * 244941
	 */
	@Test
	public void test82() {
		deployBundles("test82"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not protected", Flags.isProtected(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @extend restriction", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244995
	 */
	@Test
	public void test83() {
		deployBundles("test83"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244995
	 */
	@Test
	public void test84() {
		deployBundles("test84"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test85() {
		deployBundles("test85"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test86() {
		deployBundles("test86"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test87() {
		deployBundles("test87"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test88() {
		deployBundles("test88"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test89() {
		deployBundles("test89"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_STATIC_TO_STATIC, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test90() {
		deployBundles("test90"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.STATIC_TO_NON_STATIC, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test91() {
		deployBundles("test91"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test92() {
		deployBundles("test92"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=245166
	 */
	@Test
	public void test93() {
		deployBundles("test93"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @reference restriction", RestrictionModifiers.isReferenceRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244620
	 */
	@Test
	public void test94() {
		deployBundles("test94"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244620
	 */
	@Test
	public void test95() {
		deployBundles("test95"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244620
	 */
	@Test
	public void test96() {
		deployBundles("test96"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244620
	 */
	@Test
	public void test97() {
		deployBundles("test97"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertTrue("Not no delta", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244620
	 */
	@Test
	public void test98() {
		deployBundles("test98"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246767
	 */
	@Test
	public void test99() {
		deployBundles("test99"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.RESTRICTIONS, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @override restriction", RestrictionModifiers.isOverrideRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246767
	 */
	@Test
	public void test100() {
		deployBundles("test100"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246767
	 */
	@Test
	public void test101() {
		deployBundles("test101"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246767
	 */
	@Test
	public void test102() {
		deployBundles("test102"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246767
	 */
	@Test
	public void test103() {
		deployBundles("test103"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246767
	 */
	@Test
	public void test104() {
		deployBundles("test104"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.RESTRICTIONS, child.getFlags()); //$NON-NLS-1$
		assertFalse("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertTrue("Not @override restriction", RestrictionModifiers.isOverrideRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228209
	 */
	@Test
	public void test105() {
		deployBundles("test105"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=242598
	 */
	@Test
	public void test106() {
		deployBundles("test106"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Should not be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=247632
	 */
	@Test
	public void test107() {
		deployBundles("test107"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Should be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=247952
	 */
	@Test
	public void test108() {
		deployBundles("test108"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=251513
	 */
	@Test
	public void test109() {
		deployBundles("test109"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=261176
	 */
	@Test
	public void test110() {
		deployBundles("test110"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=267545
	 */
	@Test
	public void test111() {
		deployBundles("test111"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(before, after, VisibilityModifiers.API, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Changed Map to Map&lt;String, String&gt;
	 */
	@Test
	public void test112() {
		deployBundles("test112"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Changed Map to Map&lt;String, String&gt;
	 */
	@Test
	public void test113() {
		deployBundles("test113"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Add type parameters (constructor)
	 */
	@Test
	public void test114() {
		deployBundles("test114"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETERS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Add checked exception (constructor)
	 */
	@Test
	public void test115() {
		deployBundles("test115"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", !Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Add unchecked exception (constructor)
	 */
	@Test
	public void test116() {
		deployBundles("test116"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", !Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.UNCHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Change last parameter from array to varargs (constructor)
	 */
	@Test
	public void test117() {
		deployBundles("test117"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.ARRAY_TO_VARARGS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Removed unchecked exception (constructor)
	 */
	@Test
	public void test118() {
		deployBundles("test118"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", !Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.UNCHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Removed checked exception (constructor)
	 */
	@Test
	public void test119() {
		deployBundles("test119"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", !Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CHECKED_EXCEPTION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Increased access (constructor)
	 */
	@Test
	public void test120() {
		deployBundles("test120"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", !Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CONSTRUCTOR_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Non-final to final for @nooverride method
	 */
	@Test
	public void test121() {
		deployBundles("test121"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertTrue("Is visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	@Test
	public void test122() {
		deployBundles("test122"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_DOWN, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getNewModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags()); //$NON-NLS-1$
		assertTrue("Not visible", Util.isVisible(child.getOldModifiers())); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Added deprecation
	 */
	@Test
	public void test123() {
		deployBundles("test123"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DEPRECATION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Removed deprecation
	 */
	@Test
	public void test124() {
		deployBundles("test124"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DEPRECATION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	
	/**
	 * Added public method into protected member interface inside a class tagged
	 * as noextend
	 */
	@Test
	public void test125() {
		deployBundles("test125"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertTrue("Not no delta", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}
	
	/**
	 * Added public method into protected member interface inside a class tagged
	 * as noextend
	 */
	@Test
	public void test126() {
		deployBundles("test126"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		assertTrue("Not noextend", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
	}
}
