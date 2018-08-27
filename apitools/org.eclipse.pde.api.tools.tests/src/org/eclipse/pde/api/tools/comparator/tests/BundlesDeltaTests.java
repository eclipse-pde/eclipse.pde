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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.junit.Test;

/**
 * Delta tests for class
 */
public class BundlesDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "bundles"; //$NON-NLS-1$
	}

	/**
	 * Change bundle symbolic name
	 */
	@Test
	public void test1() {
		deployBundles("test1"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_BASELINE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_BASELINE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Addition of EE
	 */
	@Test
	public void test2() {
		deployBundles("test2"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Removing EEs
	 */
	@Test
	public void test3() {
		deployBundles("test3"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Changing EEs
	 */
	@Test
	public void test4() {
		deployBundles("test4"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		String[] arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length); //$NON-NLS-1$
		assertEquals("Wrong value", "JRE-1.1", arguments[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length); //$NON-NLS-1$
		assertEquals("Wrong value", "CDC-1.0/Foundation-1.0", arguments[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Changing EEs
	 */
	@Test
	public void test5() {
		deployBundles("test5"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		String[] arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length); //$NON-NLS-1$
		assertEquals("Wrong value", "J2SE-1.4", arguments[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length); //$NON-NLS-1$
		assertEquals("Wrong value", "J2SE-1.5", arguments[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Test null profile
	 */
	@Test(expected = IllegalArgumentException.class)
	public void test6() {
		deployBundles("test6"); //$NON-NLS-1$
		ApiComparator.compare(getBeforeState(), null, VisibilityModifiers.ALL_VISIBILITIES, false, null);
	}

	/**
	 * Test null baseline
	 */
	@Test(expected = IllegalArgumentException.class)
	public void test7() {
		deployBundles("test7"); //$NON-NLS-1$
		ApiComparator.compare((IApiBaseline) null, getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
	}

	/**
	 * Test null components
	 */
	@Test
	public void test8() {
		deployBundles("test8"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent referenceComponent = beforeState.getApiComponent("deltatest1"); //$NON-NLS-1$
		IApiComponent component = afterState.getApiComponent("deltatest1"); //$NON-NLS-1$

		IDelta delta = ApiComparator.compare(null, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_BASELINE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$

		delta = ApiComparator.compare(referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_BASELINE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$

		delta = ApiComparator.compare(null, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_BASELINE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$

		delta = ApiComparator.compare(referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_BASELINE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$

		try {
			ApiComparator.compare(referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		IApiTypeRoot classFile = null;
		try {
			classFile = component.findTypeRoot("Zork"); //$NON-NLS-1$
		} catch (CoreException e) {
			fail("Should not happen"); //$NON-NLS-1$
		}
		assertNull("No class file", classFile); //$NON-NLS-1$

		try {
			classFile = component.findTypeRoot("X"); //$NON-NLS-1$
		} catch (CoreException e) {
			fail("Should not happen"); //$NON-NLS-1$
		}
		assertNotNull("No class file", classFile); //$NON-NLS-1$

		IApiTypeRoot referenceClassFile = null;
		try {
			referenceClassFile = referenceComponent.findTypeRoot("X"); //$NON-NLS-1$
		} catch (CoreException e) {
			fail("Should not happen"); //$NON-NLS-1$
		}
		assertNotNull("No class file", referenceClassFile); //$NON-NLS-1$

		try {
			ApiComparator.compare(null, classFile, component, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, (IApiTypeRoot)null, component, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(null, component, referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, (IApiTypeRoot)null, referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		delta = ApiComparator.compare(classFile, component, referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$

		try {
			ApiComparator.compare(null, classFile, referenceComponent, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, null, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		delta = ApiComparator.compare(referenceClassFile, classFile, referenceComponent, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$

		delta = ApiComparator.compare(beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$

		delta = ApiComparator.compare(beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$

		delta = ApiComparator.compare(referenceComponent, component, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$

		try {
			ApiComparator.compare((IApiComponent) null, beforeState,VisibilityModifiers.ALL_VISIBILITIES, true, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(component, null,VisibilityModifiers.ALL_VISIBILITIES, true, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}

		delta = ApiComparator.compare(component, beforeState, VisibilityModifiers.ALL_VISIBILITIES, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$

		try {
			ApiComparator.compare(null, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, component, null, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, component, referenceComponent, null, null, afterState, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, component, referenceComponent, null, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES, null);
			fail("Should not be reached"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	/**
	 * Removing EEs
	 */
	@Test
	public void test9() {
		deployBundles("test9"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.ALL_VISIBILITIES, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Removed api packages - bug 225473
	 */
	@Test
	public void test10() {
		deployBundles("test10"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.MAJOR_VERSION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.API_TYPE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Add type in non API package
	 */
	@Test
	public void test11() {
		deployBundles("test11"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", apiComponent); //$NON-NLS-1$
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", refApiComponent); //$NON-NLS-1$
		IApiTypeRoot classFile = null;
		try {
			classFile = apiComponent.findTypeRoot("p.X2"); //$NON-NLS-1$
		} catch (CoreException e) {
			// ignore
		}
		assertNotNull("No p.X2", classFile); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(classFile, refApiComponent, apiComponent, null, beforeState, afterState, VisibilityModifiers.API, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Wrong delta", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$
	}
	/**
	 * Remove internal type in non API package
	 */
	@Test
	public void test12() {
		deployBundles("test12"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", apiComponent); //$NON-NLS-1$
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", refApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.MAJOR_VERSION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Change major version
	 */
	@Test
	public void test13() {
		deployBundles("test13"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", apiComponent); //$NON-NLS-1$
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", refApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.MAJOR_VERSION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Change minor version
	 */
	@Test
	public void test14() {
		deployBundles("test14"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", apiComponent); //$NON-NLS-1$
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1"); //$NON-NLS-1$
		assertNotNull("No api component", refApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.MINOR_VERSION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Test if diff is returned using org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator.compare(IApiComponent, IApiBaseline, int, boolean)
	 */
	@Test
	public void test15() {
		deployBundles("test15"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent component = afterState.getApiComponent("deltatest"); //$NON-NLS-1$

		IDelta delta = ApiComparator.compare(component, beforeState, VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Equals to NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Test if diff is returned using org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator.compare(IApiComponent, IApiBaseline, int, boolean)
	 */
	@Test
	public void test16() {
		deployBundles("test16"); //$NON-NLS-1$
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent component = afterState.getApiComponent("deltatest"); //$NON-NLS-1$

		IDelta delta = ApiComparator.compare(component, beforeState, VisibilityModifiers.API, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertFalse("Equals to NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}
}