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
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;

/**
 * Delta tests for enum
 */
public class EnumDeltaTests extends DeltaTestSetup {
	
	public static Test suite() {
		if (true) return new TestSuite(EnumDeltaTests.class);
		TestSuite suite = new TestSuite(EnumDeltaTests.class.getName());
		suite.addTest(new EnumDeltaTests("test3"));
		suite.addTest(new EnumDeltaTests("test4"));
		return suite;
	}

	public EnumDeltaTests(String name) {
		super(name);
	}

	public String getTestRoot() {
		return "enum";
	}
	
	/**
	 * delete enum constant
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.ENUM_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * rename enum constant = remove + add
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.ENUM_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.ENUM_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Add enum constant arguments
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED_NON_VISIBLE, child.getKind());
		assertEquals("Wrong flag", IDelta.CONSTRUCTOR, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED_NON_VISIBLE, child.getKind());
		assertEquals("Wrong flag", IDelta.CONSTRUCTOR, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Change enum constant arguments
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED_NON_VISIBLE, child.getKind());
		assertEquals("Wrong flag", IDelta.CONSTRUCTOR, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED_NON_VISIBLE, child.getKind());
		assertEquals("Wrong flag", IDelta.CONSTRUCTOR, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
	
	/**
	 * Add new enum constant
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
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.ENUM_CONSTANT, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Add new enum constant
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
		assertTrue("Not empty", delta.isEmpty());
		assertTrue("Different from NO_DELTA", delta == ApiComparator.NO_DELTA);
	}
	
	/**
	 * Added non visible method
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
		assertEquals("Wrong kind", IDelta.ADDED_NON_VISIBLE, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
	
	/**
	 * Added non visible method
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
		assertEquals("Wrong kind", IDelta.ADDED_NON_VISIBLE, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.ENUM_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
}
