/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.reference.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.Test;

/**
 * Tests search scopes.
 *
 * @since 1.0.0
 */
public class SearchScopeTests {

	/**
	 * Tests that visiting a scope with whole components is the same as visiting
	 * the components.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testVisitEntireComponentsScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingBaseline("test-plugins"); //$NON-NLS-1$
		IApiComponent componentA = profile.getApiComponent("component.a"); //$NON-NLS-1$
		IApiComponent componentB = profile.getApiComponent("component.b"); //$NON-NLS-1$
		IApiComponent[] components = new IApiComponent[]{componentA, componentB};
		IApiTypeContainer scope = Factory.newScope(components);
		Collection<String> expectedPackages = new HashSet<>();
		Collection<String> expectedTypes = new HashSet<>();
		visit(componentA, expectedPackages, expectedTypes);
		visit(componentB, expectedPackages, expectedTypes);
		Collection<String> actualPackages = new HashSet<>();
		Collection<String> actualTypes = new HashSet<>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages); //$NON-NLS-1$
		assertEquals("Different types", expectedTypes, actualTypes); //$NON-NLS-1$
	}

	/**
	 * Visits all classes files in the container, add visited packages and types to the
	 * given collections.
	 *
	 * @param container
	 * @param packageNames
	 * @param typeNames
	 * @throws CoreException
	 */
	private void visit(IApiTypeContainer container, final Collection<String> packageNames, final Collection<String> typeNames) throws CoreException {
		ApiTypeContainerVisitor visitor = new ApiTypeContainerVisitor() {

			@Override
			public boolean visitPackage(String packageName) {
				packageNames.add(packageName);
				return true;
			}

			@Override
			public void visit(String packageName, IApiTypeRoot classFile) {
				typeNames.add(classFile.getTypeName());
			}
		};
		container.accept(visitor);
	}

	/**
	 * Tests visiting a type scope - type A in package component.a in component.a
	 *
	 * @throws CoreException
	 */
	@Test
	public void testVisitTypeScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingBaseline("test-plugins"); //$NON-NLS-1$
		IApiComponent componentA = profile.getApiComponent("component.a"); //$NON-NLS-1$
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a"); //$NON-NLS-1$
		IReferenceTypeDescriptor type = pkg.getType("A"); //$NON-NLS-1$
		IApiTypeContainer scope = Factory.newTypeScope(componentA, new IReferenceTypeDescriptor[]{type});
		Collection<String> expectedPackages = new HashSet<>();
		expectedPackages.add("component.a"); //$NON-NLS-1$
		Collection<String> expectedTypes = new HashSet<>();
		expectedTypes.add("component.a.A"); //$NON-NLS-1$
		Collection<String> actualPackages = new HashSet<>();
		Collection<String> actualTypes = new HashSet<>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages); //$NON-NLS-1$
		assertEquals("Different types", expectedTypes, actualTypes); //$NON-NLS-1$
	}

	/**
	 * Tests that visiting a scope with two class files.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testVisitSpecificTypes() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingBaseline("test-plugins"); //$NON-NLS-1$
		IApiComponent componentA = profile.getApiComponent("component.a"); //$NON-NLS-1$
		Collection<String> expectedPackages = new HashSet<>();
		expectedPackages.add("a.b.c"); //$NON-NLS-1$
		expectedPackages.add("component.a"); //$NON-NLS-1$
		Collection<String> expectedTypes = new HashSet<>();
		IReferenceTypeDescriptor one = Factory.typeDescriptor("a.b.c.Generics"); //$NON-NLS-1$
		IReferenceTypeDescriptor two = Factory.typeDescriptor("component.a.NoExtendClass"); //$NON-NLS-1$
		expectedTypes.add(one.getQualifiedName());
		expectedTypes.add(two.getQualifiedName());
		IApiTypeContainer scope = Factory.newTypeScope(componentA, new IReferenceTypeDescriptor[]{one, two});
		Collection<String> actualPackages = new HashSet<>();
		Collection<String> actualTypes = new HashSet<>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages); //$NON-NLS-1$
		assertEquals("Different types", expectedTypes, actualTypes); //$NON-NLS-1$
	}

}
