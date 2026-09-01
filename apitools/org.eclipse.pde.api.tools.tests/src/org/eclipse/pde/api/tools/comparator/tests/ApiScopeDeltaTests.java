/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.junit.jupiter.api.Test;

/**
 * Delta tests using api scope
 */
public class ApiScopeDeltaTests extends DeltaTestSetup {

	@Override
	public String getTestRoot() {
		return "scope"; //$NON-NLS-1$
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test1() throws CoreException {
		deployBundles("test1"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		scope.addElement(after);
		IApiElement[] apiElement = scope.getApiElements();
		assertEquals(1, apiElement.length, "Empty"); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, false, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test2() throws CoreException {
		deployBundles("test2"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		for (IApiComponent apiComponent : after.getApiComponents()) {
			scope.addElement(apiComponent);
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test3() throws CoreException {
		deployBundles("test3"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		for (IApiComponent apiComponent : after.getApiComponents()) {
			for (IApiTypeContainer iApiTypeContainer : apiComponent.getApiTypeContainers()) {
				scope.addElement(iApiTypeContainer);
			}
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test4() throws CoreException {
		deployBundles("test4"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		IApiComponent[] apiComponents = after.getApiComponents();
		IApiTypeRoot root = null;
		for (IApiComponent apiComponent : apiComponents) {
			IApiTypeRoot findTypeRoot = apiComponent.findTypeRoot("p.X"); //$NON-NLS-1$
			if (findTypeRoot != null) {
				root = findTypeRoot;
				break;
			}
		}
		if (root != null) {
			scope.addElement(root);
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals(1, allLeavesDeltas.length, "Wrong size"); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals(IDelta.REMOVED, child.getKind(), "Wrong kind"); //$NON-NLS-1$
		assertEquals(IDelta.METHOD, child.getFlags(), "Wrong flag"); //$NON-NLS-1$
		assertEquals(IDelta.CLASS_ELEMENT_TYPE, child.getElementType(), "Wrong element type"); //$NON-NLS-1$
		assertFalse(DeltaProcessor.isCompatible(child), "Is compatible"); //$NON-NLS-1$
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test5() throws CoreException {
		deployBundles("test5"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		IApiElement[] apiElement = scope.getApiElements();
		assertEquals(0, apiElement.length, "Not empty"); //$NON-NLS-1$
		IApiComponent[] apiComponents = after.getApiComponents();
		IApiTypeRoot root = null;
		for (IApiComponent apiComponent : apiComponents) {
			IApiTypeRoot findTypeRoot = apiComponent.findTypeRoot("p.X"); //$NON-NLS-1$
			if (findTypeRoot != null) {
				root = findTypeRoot;
				break;
			}
		}
		if (root != null) {
			IApiType structure = root.getStructure();
			for (IApiMethod method : structure.getMethods()) {
				scope.addElement(method);
			}
		}
		try {
			ApiComparator.compare(scope, before, VisibilityModifiers.API, true, null);
			fail("Should not be there"); //$NON-NLS-1$
		} catch (CoreException e) {
			// should fail to visit a method
		}
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test6() throws CoreException {
		deployBundles("test6"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		scope.addElement(after);
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, false, null);
		assertNotNull(delta, "No delta"); //$NON-NLS-1$
		assertEquals(ApiComparator.NO_DELTA, delta, "Not NO_DELTA"); //$NON-NLS-1$
	}

	/**
	 * Use api scope
	 */
	@Test
	public void test7() throws CoreException {
		deployBundles("test7"); //$NON-NLS-1$
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull(beforeApiComponent, "no api component"); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull(afterApiComponent, "no api component"); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		scope.addElement(after);
		try {
			ApiComparator.compare((IApiScope) null, before, VisibilityModifiers.API, false, null);
			fail("Should not be there"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// expected as scope is null
		}
		try {
			ApiComparator.compare(scope, null, VisibilityModifiers.API, false, null);
			fail("Should not be there"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// expected as scope is null
		}
	}
}
