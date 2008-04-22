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

/**
 * Delta tests for restrictions delta
 */
public class RestrictionsDeltaTests extends DeltaTestSetup {
	
	public static Test suite() {
		if (true) return new TestSuite(RestrictionsDeltaTests.class);
		TestSuite suite = new TestSuite(RestrictionsDeltaTests.class.getName());
		suite.addTest(new RestrictionsDeltaTests("test1"));
		return suite;
	}

	public RestrictionsDeltaTests(String name) {
		super(name);
	}

	public String getTestRoot() {
		return "restrictions";
	}
	
	/**
	 * Change restrictions
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
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Extend restrictions", !RestrictionModifiers.isExtendRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.RESTRICTIONS, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Add restrictions
	 */
	public void test2() {
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Extend restrictions", !RestrictionModifiers.isExtendRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.RESTRICTIONS, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Add restrictions
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertTrue("Extend restrictions", !RestrictionModifiers.isExtendRestriction(child.getRestrictions()));
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.RESTRICTIONS, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
}
