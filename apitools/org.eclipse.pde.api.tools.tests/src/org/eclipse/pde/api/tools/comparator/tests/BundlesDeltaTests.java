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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;

/**
 * Delta tests for class
 */
public class BundlesDeltaTests extends DeltaTestSetup {
	
	public static Test suite() {
		if (true) return new TestSuite(BundlesDeltaTests.class);
		TestSuite suite = new TestSuite(BundlesDeltaTests.class.getName());
		suite.addTest(new BundlesDeltaTests("test14"));
		return suite;
	}

	public BundlesDeltaTests(String name) {
		super(name);
	}

	public String getTestRoot() {
		return "bundles";
	}

	/**
	 * Change bundle symbolic name
	 */
	public void test1() {
		deployBundles("test1");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState());
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_PROFILE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_PROFILE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Addition of EE
	 */
	public void test2() {
		deployBundles("test2");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState());
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Removing EEs
	 */
	public void test3() {
		deployBundles("test3");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState());
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}

	/**
	 * Changing EEs
	 */
	public void test4() {
		deployBundles("test4");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState());
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		String[] arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length);
		assertEquals("Wrong value", "JRE-1.1", arguments[0]);
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length);
		assertEquals("Wrong value", "CDC-1.0/Foundation-1.0", arguments[0]);
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Changing EEs
	 */
	public void test5() {
		deployBundles("test5");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState());
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		String[] arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length);
		assertEquals("Wrong value", "J2SE-1.4", arguments[0]);
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		arguments = child.getArguments();
		assertEquals("Wrong size", 2, arguments.length);
		assertEquals("Wrong value", "J2SE-1.5", arguments[0]);
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	
	/**
	 * Test null profile
	 */
	public void test6() {
		deployBundles("test6");
		try {
			ApiComparator.compare(getBeforeState(), null);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}
	
	/**
	 * Test null profile
	 */
	public void test7() {
		deployBundles("test7");
		try {
			ApiComparator.compare(null, getAfterState());
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}
	
	/**
	 * Test null components
	 */
	public void test8() {
		deployBundles("test8");
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent referenceComponent = beforeState.getApiComponent("deltatest1");
		IApiComponent component = afterState.getApiComponent("deltatest1");
		try {
			ApiComparator.compare(referenceComponent, null, VisibilityModifiers.API);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(null, component, VisibilityModifiers.API);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		IDelta delta = ApiComparator.compare(null, component, beforeState, afterState);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_PROFILE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));

		delta = ApiComparator.compare(referenceComponent, null, beforeState, afterState);
		assertNotNull("No delta", delta);
		allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_PROFILE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));

		delta = ApiComparator.compare(null, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
		assertNotNull("No delta", delta);
		allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_PROFILE_ELEMENT_TYPE, child.getElementType());
		assertTrue("Is compatible", DeltaProcessor.isCompatible(child));

		delta = ApiComparator.compare(referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
		assertNotNull("No delta", delta);
		allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_COMPONENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_PROFILE_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));

		try {
			ApiComparator.compare(referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		
		try {
			ApiComparator.compare(referenceComponent, component, beforeState, null);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceComponent, component, null, afterState);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		IClassFile classFile = null;
		try {
			classFile = component.findClassFile("Zork");
		} catch (CoreException e) {
			assertTrue("Should not happen", false);
		}
		assertNull("No class file", classFile);

		try {
			classFile = component.findClassFile("X");
		} catch (CoreException e) {
			assertTrue("Should not happen", false);
		}
		assertNotNull("No class file", classFile);

		IClassFile referenceClassFile = null;
		try {
			referenceClassFile = referenceComponent.findClassFile("X");
		} catch (CoreException e) {
			assertTrue("Should not happen", false);
		}
		assertNotNull("No class file", referenceClassFile);

		try {
			ApiComparator.compare(null, classFile, component, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, null, component, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(null, component, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		
		try {
			ApiComparator.compare(referenceClassFile, null, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		delta = ApiComparator.compare(classFile, component, referenceComponent, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);

		try {
			ApiComparator.compare(null, classFile, referenceComponent, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, null, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, referenceComponent, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, referenceComponent, component, null, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(referenceClassFile, classFile, referenceComponent, component, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		delta = ApiComparator.compare(referenceClassFile, classFile, referenceComponent, component, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);

		delta = ApiComparator.compare(beforeState, afterState, true);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);

		delta = ApiComparator.compare(beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);

		delta = ApiComparator.compare(referenceComponent, component, VisibilityModifiers.ALL_VISIBILITIES);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);

		try {
			ApiComparator.compare((IApiComponent) null, beforeState,VisibilityModifiers.ALL_VISIBILITIES, true);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			ApiComparator.compare(component, null,VisibilityModifiers.ALL_VISIBILITIES, true);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		delta = ApiComparator.compare(component, beforeState, VisibilityModifiers.ALL_VISIBILITIES, true);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);

		try {
			ApiComparator.compare(null, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, component, null, beforeState, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, component, referenceComponent, null, afterState, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			ApiComparator.compare(classFile, component, referenceComponent, beforeState, null, VisibilityModifiers.ALL_VISIBILITIES);
			assertFalse("Should not be reached", true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	/**
	 * Removing EEs
	 */
	public void test9() {
		deployBundles("test9");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState());
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 3, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.EXECUTION_ENVIRONMENT, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Removed api packages - bug 225473
	 */
	public void test10() {
		deployBundles("test10");
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 2, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.MAJOR_VERSION, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.API_TYPE, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Add type in non API package
	 */
	public void test11() {
		deployBundles("test11");
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1");
		assertNotNull("No api component", apiComponent);
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1");
		assertNotNull("No api component", refApiComponent);
		IClassFile classFile = null;
		try {
			classFile = apiComponent.findClassFile("p.X2");
		} catch (CoreException e) {
			// ignore
		}
		assertNotNull("No p.X2", classFile);
		IDelta delta = ApiComparator.compare(classFile, refApiComponent, apiComponent, beforeState, afterState, VisibilityModifiers.API);
		assertNotNull("No delta", delta);
		assertTrue("Wrong delta", delta == ApiComparator.NO_DELTA);
	}
	/**
	 * Remove internal type in non API package
	 */
	public void test12() {
		deployBundles("test12");
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1");
		assertNotNull("No api component", apiComponent);
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1");
		assertNotNull("No api component", refApiComponent);
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.MAJOR_VERSION, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Change major version
	 */
	public void test13() {
		deployBundles("test13");
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1");
		assertNotNull("No api component", apiComponent);
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1");
		assertNotNull("No api component", refApiComponent);
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.MAJOR_VERSION, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Change minor version
	 */
	public void test14() {
		deployBundles("test14");
		IApiBaseline beforeState = getBeforeState();
		IApiBaseline afterState = getAfterState();
		IApiComponent apiComponent = afterState.getApiComponent("deltatest1");
		assertNotNull("No api component", apiComponent);
		IApiComponent refApiComponent = beforeState.getApiComponent("deltatest1");
		assertNotNull("No api component", refApiComponent);
		IDelta delta = ApiComparator.compare(getBeforeState(), getAfterState(), VisibilityModifiers.API, false);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.MINOR_VERSION, child.getFlags());
		assertEquals("Wrong element type", IDelta.API_COMPONENT_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not compatible", DeltaProcessor.isCompatible(child));
	}
}