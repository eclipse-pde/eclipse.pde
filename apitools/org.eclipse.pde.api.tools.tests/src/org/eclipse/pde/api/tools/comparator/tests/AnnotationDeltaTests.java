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

import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.junit.jupiter.api.Test;

/**
 * Delta tests for annotation
 */
public class AnnotationDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "annotation"; //$NON-NLS-1$
	}

	/**
	 * Add element to annotation type
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
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * remove element to annotation type
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add element to annotation type
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
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add elements with all different types
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
		assertEquals(11, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[3];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[4];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[5];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[6];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[7];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[8];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[9];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[10];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}
	/**
	 * Add elements with all different types (array)
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
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(13, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[3];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[4];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[5];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[6];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[7];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[8];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[9];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[10];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[11];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[12];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Changed default values
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
		assertEquals(12, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[1];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[2];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child= allLeavesDeltas[3];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[4];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[5];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[6];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[7];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[8];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[9];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[10];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
		child = allLeavesDeltas[11];
		assertEquals(IDelta.CHANGED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove method with default value
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITH_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Remove method with no default value
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
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD_WITHOUT_DEFAULT_VALUE, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Add a field
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
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.FIELD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}
	/**
	 * Added deprecation
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DEPRECATION, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}
	/**
	 * Removed deprecation
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after, VisibilityModifiers.API, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.DEPRECATION, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}

	/**
	 * Add a member type to an annotation - compatible change
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
		IDelta delta = ApiComparator.compare(beforeApiComponent, afterApiComponent, before, after,
				VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.ADDED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.TYPE_MEMBER, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.ANNOTATION_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertTrue(DeltaProcessor.isCompatible(child), "Not compatible"); //$NON-NLS-1$
	}
}
