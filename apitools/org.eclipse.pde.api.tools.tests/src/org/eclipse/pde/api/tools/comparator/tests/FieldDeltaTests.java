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
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Delta tests for field
 */

public class FieldDeltaTests extends DeltaTestSetup {
	public static Test suite() {
		if (false) return new TestSuite(FieldDeltaTests.class);
		TestSuite suite = new TestSuite(FieldDeltaTests.class.getName());
		suite.addTest(new FieldDeltaTests("test42"));
		return suite;
	}

	public String getTestRoot() {
		return "field";
	}
	
	public FieldDeltaTests(String name) {
		super(name);
	}
	/**
	 * Check change field type (interface)
	 */
	public void test1() {
		deployBundles("test1");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field value (interface)
	 */
	public void test2() {
		deployBundles("test2");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Check change field type (class)
	 */
	public void test3() {
		deployBundles("test3");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field value (class)
	 */
	public void test4() {
		deployBundles("test4");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field value (class) - no delta
	 */
	public void test5() {
		deployBundles("test5");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
	 * Check decrease field visibility - public to protected
	 */
	public void test6() {
		deployBundles("test6");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check decrease field visibility - public to default
	 */
	public void test7() {
		deployBundles("test7");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Check decrease field visibility - public to private
	 */
	public void test8() {
		deployBundles("test8");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field visibility - protected to public
	 */
	public void test9() {
		deployBundles("test9");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field visibility - default to public
	 */
	public void test10() {
		deployBundles("test10");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Check increase field visibility - private to public
	 */
	public void test11() {
		deployBundles("test11");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.INCREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
	 */
	public void test12() {
		deployBundles("test12");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL_NON_STATIC, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
	 */
	public void test13() {
		deployBundles("test13");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
	 */
	public void test14() {
		deployBundles("test14");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLINIT, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
	 */
	public void test15() {
		deployBundles("test15");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field modifiers - static to non static
	 */
	public void test16() {
		deployBundles("test16");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.STATIC_TO_NON_STATIC, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field modifiers - non static to static
	 */
	public void test17() {
		deployBundles("test17");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.NON_STATIC_TO_STATIC, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field modifiers - transient to non transient
	 */
	public void test18() {
		deployBundles("test18");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TRANSIENT_TO_NON_TRANSIENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check change field modifiers - non transient to transient
	 */
	public void test19() {
		deployBundles("test19");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.NON_TRANSIENT_TO_TRANSIENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Decrease access
	 */
	public void test20() {
		deployBundles("test20");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Decrease access
	 */
	public void test21() {
		deployBundles("test21");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.DECREASE_ACCESS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed static non final to static final
	 */
	public void test22() {
		deployBundles("test22");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
	 */
	public void test23() {
		deployBundles("test23");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLINIT, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed value of non-visible field (default)
	 */
	public void test24() {
		deployBundles("test24");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Check increase field modifiers - final to non-final (field non static)
	 */
	public void test25() {
		deployBundles("test25");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLINIT, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed value of non-visible field (private)
	 */
	public void test26() {
		deployBundles("test26");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Added value of non-visible field (private)
	 */
	public void test27() {
		deployBundles("test27");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertTrue("Is visible", !Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.NON_FINAL_TO_FINAL, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed value of non-visible field (protected with extend restrictions)
	 */
	public void test28() {
		deployBundles("test28");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", Util.isVisible(child));
		assertTrue("No extend restrictions", RestrictionModifiers.isExtendRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Changed List&lt;Integer&gt; to List&lt;String&gt;
	 */
	public void test29() {
		deployBundles("test29");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Changed Map&lt;String, Integer&gt; to Map&lt;String, String&gt;
	 */
	public void test30() {
		deployBundles("test30");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed Map to Map&lt;String, String&gt;
	 */
	public void test31() {
		deployBundles("test31");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Changed List&lt;String&gt; to ArrayList&lt;String&gt;
	 */
	public void test32() {
		deployBundles("test32");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed ArrayList&lt;String&gt; to ArrayList
	 */
	public void test33() {
		deployBundles("test33");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changed X&lt;String, Integer, Number&gt; to X&lt;Integer, String, Number&gt;
	 */
	public void test34() {
		deployBundles("test34");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=218976
	 */
	public void test35() {
		deployBundles("test35");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", Util.isVisible(child));
		assertTrue("No extend restrictions", RestrictionModifiers.isExtendRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=222905
	 */
	public void test36() {
		deployBundles("test36");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertTrue("Is visible", Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.INTERFACE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=222905
	 */
	public void test37() {
		deployBundles("test37");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertTrue("Is visible", Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}


	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=222905
	 */
	public void test38() {
		deployBundles("test38");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertFalse("Not visible", Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.CLINIT, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertTrue("Is visible", Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertTrue("Is visible", Util.isVisible(child));
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Remove compile-time constant
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=224994
	 */
	public void test39() {
		deployBundles("test39");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLINIT, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Field addition
	 */
	public void test40() {
		deployBundles("test40");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 6, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.CLINIT, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[4];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[5];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.TYPE, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=225164
	 */
	public void test41() {
		deployBundles("test41");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.FIELD, child.getFlags());
		assertTrue("Wrong restrictions", RestrictionModifiers.isExtendRestriction(child.getRestrictions()));
		assertTrue("Wrong modifier", Util.isProtected(child.getModifiers()));
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Added type arguments List&lt;String&gt;
	 */
	public void test42() {
		deployBundles("test42");
		IApiProfile before = getBeforeState();
		IApiProfile after = getAfterState();
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
		assertEquals("Wrong flag", IDelta.TYPE_ARGUMENTS, child.getFlags());
		assertEquals("Wrong element type", IDelta.FIELD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
}