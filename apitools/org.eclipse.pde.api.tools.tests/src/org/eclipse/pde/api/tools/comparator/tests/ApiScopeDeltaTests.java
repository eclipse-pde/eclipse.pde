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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
import org.junit.Test;

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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		scope.addElement(after);
		IApiElement[] apiElement = scope.getApiElements();
		assertEquals("Empty", 1, apiElement.length); //$NON-NLS-1$
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		IApiComponent[] apiComponents = after.getApiComponents();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			scope.addElement(apiComponents[i]);
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		IApiComponent[] apiComponents = after.getApiComponents();
		for (IApiComponent apiComponent : apiComponents) {
			IApiTypeContainer[] apiTypeContainers = apiComponent.getApiTypeContainers();
			for (int j = 0; j < apiTypeContainers.length; j++) {
				IApiTypeContainer iApiTypeContainer = apiTypeContainers[j];
				scope.addElement(iApiTypeContainer);
			}
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
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
		assertNotNull("No delta", delta); //$NON-NLS-1$
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length); //$NON-NLS-1$
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind()); //$NON-NLS-1$
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags()); //$NON-NLS-1$
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType()); //$NON-NLS-1$
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child)); //$NON-NLS-1$
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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		IApiElement[] apiElement = scope.getApiElements();
		assertEquals("Not empty", 0, apiElement.length); //$NON-NLS-1$
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
			IApiMethod[] methods = structure.getMethods();
			for (int i = 0, max = methods.length; i < max; i++) {
				scope.addElement(methods[i]);
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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
		ApiScope scope = new ApiScope();
		scope.addElement(after);
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, false, null);
		assertNotNull("No delta", delta); //$NON-NLS-1$
		assertEquals("Not NO_DELTA", ApiComparator.NO_DELTA, delta); //$NON-NLS-1$
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
		assertNotNull("no api component", beforeApiComponent); //$NON-NLS-1$
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent); //$NON-NLS-1$
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
