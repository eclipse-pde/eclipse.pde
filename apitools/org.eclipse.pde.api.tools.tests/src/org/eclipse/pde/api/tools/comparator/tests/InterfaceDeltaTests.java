/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;

/**
 * Delta tests for interface
 */

public class InterfaceDeltaTests extends DeltaTestSetup {
	public static Test suite() {
		if (true) return new TestSuite(InterfaceDeltaTests.class);
		TestSuite suite = new TestSuite(InterfaceDeltaTests.class.getName());
		suite.addTest(new InterfaceDeltaTests("test8"));
		return suite;
	}

	public String getTestRoot() {
		return "interface";
	}
	
	public InterfaceDeltaTests(String name) {
		super(name);
	}
	/**
	 * delete API method
	 */
	public void test1() {
		deployBundles("test1");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * delete API field
	 */
	public void test2() {
		deployBundles("test2");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Reorder of members
	 */
	public void test3() {
		deployBundles("test3");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		assertTrue("Not empty", delta.isEmpty());
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA);
	}

	/**
	 * Add type parameter
	 */
	public void test4() {
		deployBundles("test4");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETERS, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Add type parameter
	 */
	public void test5() {
		deployBundles("test5");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Delete type parameter
	 */
	public void test6() {
		deployBundles("test6");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Delete type parameter
	 */
	public void test7() {
		deployBundles("test7");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Reorder of type parameter
	 */
	public void test8() {
		deployBundles("test8");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 4, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLASS_BOUND, child.getFlags());
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.INTERFACE_BOUND, child.getFlags());
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLASS_BOUND, child.getFlags());
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.INTERFACE_BOUND, child.getFlags());
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Rename of type parameter
	 */
	public void test9() {
		deployBundles("test9");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags());
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_PARAMETER_NAME, child.getFlags());
		assertEquals("Wrong element type", IDelta.TYPE_PARAMETER_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Add super interface
	 */
	public void test10() {
		deployBundles("test10");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Remove super interface
	 */
	public void test11() {
		deployBundles("test11");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Remove super interface
	 */
	public void test12() {
		deployBundles("test12");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.CONTRACTED_SUPERINTERFACES_SET, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Add super interface
	 */
	public void test13() {
		deployBundles("test13");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXPANDED_SUPERINTERFACES_SET, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Addition of a field in an interface that cannot be implemented
	 */
	public void test14() {
		deployBundles("test14");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Addition of a field in an interface that can be implemented
	 */
	public void test15() {
		deployBundles("test15");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Implement restrictions", !RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Move method up in hierarchy
	 */
	public void test16() {
		deployBundles("test16");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Move method up in hierarchy
	 */
	public void test17() {
		deployBundles("test17");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_MOVED_UP, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Add a member type
	 */
	public void test18() {
		deployBundles("test18");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Add a member types
	 */
	public void test19() {
		deployBundles("test19");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Add member types
	 */
	public void test20() {
		deployBundles("test20");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Not implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Remove member types
	 */
	public void test21() {
		deployBundles("test21");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Add member types
	 */
	public void test22() {
		deployBundles("test22");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.TYPE_MEMBER, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Move field up in hierarchy
	 */
	public void test23() {
		deployBundles("test23");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertFalse("Is implement restrictions", RestrictionModifiers.isImplementRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.FIELD_MOVED_UP, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Added one parameter to an API method (=> addition and removal of a method)
	 */
	public void test24() {
		deployBundles("test24");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
}
