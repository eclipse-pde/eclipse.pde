/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;

import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Delta tests for interface
 */

public class InterfaceDeltaTests extends DeltaTestSetup {
	public static Test suite() {
		return new TestSuite(InterfaceDeltaTests.class);
//		TestSuite suite = new TestSuite(InterfaceDeltaTests.class.getName());
//		suite.addTest(new InterfaceDeltaTests("test38"));
//		return suite;
	}

	@Override
	public String getTestRoot() {
		return "interface"; //$NON-NLS-1$
	}

	public InterfaceDeltaTests(String name) {
		super(name);
	}
	/**
	 * delete API method
	 */
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
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * delete API field
	 */
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
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Reorder of members
	 */
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
		assertTrue("Not empty", delta.isEmpty()); //$NON-NLS-1$
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Add type parameter
	 */
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
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETERS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add type parameter
	 */
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
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Delete type parameter
	 */
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Delete type parameter
	 */
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
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Reorder of type parameter
	 */
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
		assertEquals("Wrong size", 4, allLeavesDeltas.length); //$NON-NLS-1$
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CLASS_BOUND, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.INTERFACE_BOUND, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Rename of type parameter
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add super interface
	 */
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
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove super interface
	 */
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
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove super interface
	 */
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
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add super interface
	 */
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
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that cannot be implemented
	 */
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Should be compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that cannot be extended
	 */
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Should not be compatible", !DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that cannot be extended
	 */
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
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Should be compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Addition of a field in an interface that can be implemented
	 */
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Implement restrictions", !RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy with noimplement only
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy with noextend only
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not extend restrictions", RestrictionModifiers.isExtendRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add not compatible", !DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy with noextend and noimplement
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Move method up in hierarchy
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add a member type
	 */
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add a member types
	 */
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add member types
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Remove member types
	 */
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Add member types
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Move field up in hierarchy
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getCurrentRestrictions())); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.FIELD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * Added one parameter to an API method (=> addition and removal of a method)
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=507246
	 */
	public void test29() {
		deployBundles("test29"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after,
				VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.OVERRIDEN_METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SUPER_INTERFACE_DEFAULT_METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Changed superinterfaces set not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add not compatible", !DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Changed superinterfaces set not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.SUPER_INTERFACE_WITH_METHODS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Changed superinterfaces set not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
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
		assertEquals("Wrong size", 3, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=244984
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add compatible", !DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Method Add not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	public void test38() {
		deployBundles("test38"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		assertFalse(beforeApiComponent.hasApiDescription());
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Added deprecation
	 */
	public void test39() {
		deployBundles("test39"); //$NON-NLS-1$
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
		assertEquals("Wrong flag", IDelta.DEPRECATION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Removed deprecation
	 */
	public void test40() {
		deployBundles("test40"); //$NON-NLS-1$
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
		assertEquals("Wrong flag", IDelta.DEPRECATION, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
	/**
	 * Remove @noimplement and add new methods
	 */
	public void test41() {
		deployBundles("test41"); //$NON-NLS-1$
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
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
	}
}
