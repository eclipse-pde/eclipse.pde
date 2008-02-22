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
 * Delta tests for annotation
 */
public class AnnotationDeltaTests extends DeltaTestSetup {
	
	public static Test suite() {
		if (true) return new TestSuite(AnnotationDeltaTests.class);
		TestSuite suite = new TestSuite(AnnotationDeltaTests.class.getName());
		suite.addTest(new AnnotationDeltaTests("test6"));
		return suite;
	}

	public AnnotationDeltaTests(String name) {
		super(name);
	}

	public String getTestRoot() {
		return "annotation";
	}

	/**
	 * Add element to annotation type
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
	
	/**
	 * remove element to annotation type
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Add element to annotation type
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
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Add elements with all different types
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
		assertEquals("Wrong size", 11, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[4];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[5];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[6];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[7];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[8];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[9];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[10];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
	/**
	 * Add elements with all different types (array)
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
		assertEquals("Wrong size", 13, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[4];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[5];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[6];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[7];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[8];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[9];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[10];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[11];
		assertEquals("Wrong kind", IDelta.ADDED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[12];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
	
	/**
	 * Changed default values
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
		assertEquals("Wrong size", 12, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[1];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[2];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child= allLeavesDeltas[3];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[4];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[5];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[6];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[7];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[8];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[9];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[10];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
		child = allLeavesDeltas[11];
		assertEquals("Wrong kind", IDelta.CHANGED, child.getKind());
		assertEquals("Wrong flag", IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.METHOD_ELEMENT_TYPE, child.getElementType());
		assertTrue("Not binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Remove method with default value
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}

	/**
	 * Remove method with no default value
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
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags());
		assertEquals("Wrong element type", IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is binary compatible", DeltaProcessor.isBinaryCompatible(child));
	}
}
