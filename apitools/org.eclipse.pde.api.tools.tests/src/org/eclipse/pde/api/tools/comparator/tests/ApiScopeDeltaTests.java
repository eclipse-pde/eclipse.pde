/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

/**
 * Delta tests using api scope
 */
public class ApiScopeDeltaTests extends DeltaTestSetup {
	
	public static Test suite() {
		return new TestSuite(ApiScopeDeltaTests.class);
//		TestSuite suite = new TestSuite(EnumDeltaTests.class.getName());
//		suite.addTest(new EnumDeltaTests("test13"));
//		return suite;
	}

	public ApiScopeDeltaTests(String name) {
		super(name);
	}

	public String getTestRoot() {
		return "scope";
	}
	
	/**
	 * Use api scope
	 */
	public void test1() throws CoreException {
		deployBundles("test1");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		scope.add(after);
		IApiElement[] apiElement = scope.getApiElements();
		assertEquals("Empty", 1, apiElement.length);
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Use api scope
	 */
	public void test2() throws CoreException {
		deployBundles("test2");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		IApiComponent[] apiComponents = after.getApiComponents();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			scope.add(apiComponents[i]);
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Use api scope
	 */
	public void test3() throws CoreException {
		deployBundles("test3");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		IApiComponent[] apiComponents = after.getApiComponents();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiTypeContainer[] apiTypeContainers = apiComponents[i].getApiTypeContainers();
			for (int j = 0; j < apiTypeContainers.length; j++) {
				IApiTypeContainer iApiTypeContainer = apiTypeContainers[j];
				scope.add(iApiTypeContainer);
			}
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Use api scope
	 */
	public void test4() throws CoreException {
		deployBundles("test4");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		IApiComponent[] apiComponents = after.getApiComponents();
		IApiTypeRoot root = null;
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiTypeRoot findTypeRoot = apiComponents[i].findTypeRoot("p.X");
			if (findTypeRoot != null) {
				root = findTypeRoot;
				break;
			}
		}
		if (root != null) {
			scope.add(root);
		}
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API, true);
		assertNotNull("No delta", delta);
		IDelta[] allLeavesDeltas = collectLeaves(delta);
		assertEquals("Wrong size", 1, allLeavesDeltas.length);
		IDelta child = allLeavesDeltas[0];
		assertEquals("Wrong kind", IDelta.REMOVED, child.getKind());
		assertEquals("Wrong flag", IDelta.METHOD, child.getFlags());
		assertEquals("Wrong element type", IDelta.CLASS_ELEMENT_TYPE, child.getElementType());
		assertFalse("Is compatible", DeltaProcessor.isCompatible(child));
	}
	/**
	 * Use api scope
	 */
	public void test5() throws CoreException {
		deployBundles("test5");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		IApiElement[] apiElement = scope.getApiElements();
		assertEquals("Not empty", 0, apiElement.length);
		IApiComponent[] apiComponents = after.getApiComponents();
		IApiTypeRoot root = null;
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiTypeRoot findTypeRoot = apiComponents[i].findTypeRoot("p.X");
			if (findTypeRoot != null) {
				root = findTypeRoot;
				break;
			}
		}
		if (root != null) {
			IApiType structure = root.getStructure();
			IApiMethod[] methods = structure.getMethods();
			for (int i = 0, max = methods.length; i < max; i++) {
				scope.add(methods[i]);
			}
		}
		try {
			ApiComparator.compare(scope, before, VisibilityModifiers.API, true);
			assertFalse("Should not be there", true);
		} catch (CoreException e) {
			// should fail to visit a method
		}
	}
	/**
	 * Use api scope
	 */
	public void test6() throws CoreException {
		deployBundles("test6");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		scope.add(after);
		IDelta delta = ApiComparator.compare(scope, before, VisibilityModifiers.API);
		assertNotNull("No delta", delta);
		assertTrue("Not NO_DELTA", delta == ApiComparator.NO_DELTA);
	}
	/**
	 * Use api scope
	 */
	public void test7() throws CoreException {
		deployBundles("test7");
		IApiBaseline before = getBeforeState();
		IApiBaseline after = getAfterState();
		IApiComponent beforeApiComponent = before.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", beforeApiComponent);
		IApiComponent afterApiComponent = after.getApiComponent(BUNDLE_NAME);
		assertNotNull("no api component", afterApiComponent);
		ApiScope scope = new ApiScope();
		scope.add(after);
		try {
			ApiComparator.compare((IApiScope) null, before, VisibilityModifiers.API);
			assertFalse("Should not be there", true);
		} catch (IllegalArgumentException e) {
			// expected as scope is null
		}
		try {
			ApiComparator.compare(scope, null, VisibilityModifiers.API);
			assertFalse("Should not be there", true);
		} catch (IllegalArgumentException e) {
			// expected as scope is null
		}
	}
}

