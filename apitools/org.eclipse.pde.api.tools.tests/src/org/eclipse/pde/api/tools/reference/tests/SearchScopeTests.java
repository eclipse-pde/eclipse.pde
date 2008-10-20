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
package org.eclipse.pde.api.tools.reference.tests;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests search scopes.
 * 
 * @since 1.0.0
 */
public class SearchScopeTests extends TestCase {
	
	/**
	 * Tests that visiting a scope with whole components is the same as visiting
	 * the components.
	 * 
	 * @throws CoreException
	 */
	public void testVisitEntireComponentsScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IApiComponent[] components = new IApiComponent[]{componentA, componentB};
		IApiSearchScope scope = Factory.newScope(components);
		Collection<String> expectedPackages = new HashSet<String>();
		Collection<String> expectedTypes = new HashSet<String>();
		visit(componentA, expectedPackages, expectedTypes);
		visit(componentB, expectedPackages, expectedTypes);
		Collection<String> actualPackages = new HashSet<String>();
		Collection<String> actualTypes = new HashSet<String>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages);
		assertEquals("Different types", expectedTypes, actualTypes);
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
	private void visit(IClassFileContainer container, final Collection<String> packageNames, final Collection<String> typeNames) throws CoreException {
		ClassFileContainerVisitor visitor = new ClassFileContainerVisitor() {
		
			public boolean visitPackage(String packageName) {
				packageNames.add(packageName);
				return true;
			}
		
			public void visit(String packageName, IClassFile classFile) {
				typeNames.add(classFile.getTypeName());
			}
		};
		container.accept(visitor);
	}

	/**
	 * Tests visiting a package scope - package a.b.c in component.a
	 * 
	 * @throws CoreException
	 */
	public void testVisitPackageScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiSearchScope scope = Factory.newScope(componentA, new IElementDescriptor[]{Factory.packageDescriptor("a.b.c")});
		final Collection<String> expectedPackages = new HashSet<String>();
		expectedPackages.add("a.b.c");
		final Collection<String> expectedTypes = new HashSet<String>();
		componentA.accept(new ClassFileContainerVisitor() {
			public boolean visitPackage(String packageName) {
				return expectedPackages.contains(packageName);
			}
			public void visit(String packageName, IClassFile classFile) {
				expectedTypes.add(classFile.getTypeName());
			}
		
		});
		Collection<String> actualPackages = new HashSet<String>();
		Collection<String> actualTypes = new HashSet<String>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages);
		assertEquals("Different types", expectedTypes, actualTypes);
	}
	
	/**
	 * Tests visiting a type scope - type A in package component.a in component.a
	 * 
	 * @throws CoreException
	 */
	public void testVisitTypeScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor type = pkg.getType("A");
		IApiSearchScope scope = Factory.newScope(componentA, new IElementDescriptor[]{type});
		Collection<String> expectedPackages = new HashSet<String>();
		expectedPackages.add("component.a");
		Collection<String> expectedTypes = new HashSet<String>();
		expectedTypes.add("component.a.A");
		Collection<String> actualPackages = new HashSet<String>();
		Collection<String> actualTypes = new HashSet<String>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages);
		assertEquals("Different types", expectedTypes, actualTypes);
	}	
	
	/**
	 * Tests visiting a type scope - method Activator.start(..) in package component.a in component.a
	 * 
	 * @throws CoreException
	 */
	public void testVisitMethodScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor type = pkg.getType("Activator");
		IMethodDescriptor method = type.getMethod("start", "(QBundleContext;)V");
		IApiSearchScope scope = Factory.newScope(componentA, new IElementDescriptor[]{method});
		Collection<String> expectedPackages = new HashSet<String>();
		expectedPackages.add("component.a");
		Collection<String> expectedTypes = new HashSet<String>();
		expectedTypes.add("component.a.Activator");
		Collection<String> actualPackages = new HashSet<String>();
		Collection<String> actualTypes = new HashSet<String>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages);
		assertEquals("Different types", expectedTypes, actualTypes);
	}	
	
	/**
	 * Tests that adding a parent scope replaces the child scope - i.e.
	 * makes the scope wider.
	 * 
	 * @throws CoreException
	 */
	public void testAddingWiderScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor type = pkg.getType("Activator");
		IMethodDescriptor method = type.getMethod("start", "(QBundleContext;)V");
		// and the method and type scopes (type includes method)
		IApiSearchScope scope = Factory.newScope(componentA, new IElementDescriptor[]{method, type});
		// scope should now include everything in the type
		IFieldDescriptor field = type.getField("PLUGIN_ID");
		assertTrue("Should enclose method", scope.encloses(componentA.getId(), method));
		assertTrue("Should enclose field", scope.encloses(componentA.getId(), field));
		assertTrue("Should enclose type", scope.encloses(componentA.getId(), type));
		assertFalse("Should not enclose package", scope.encloses(componentA.getId(), pkg));
	}
	
	/**
	 * Tests that a type does not enclose siblings.
	 * 
	 * @throws CoreException
	 */
	public void testEnclosesSiblingScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor type = pkg.getType("Activator");
		IReferenceTypeDescriptor typeA = pkg.getType("A");
		IApiSearchScope scope = Factory.newScope(componentA, new IElementDescriptor[]{type});
		assertTrue("Should enclose type", scope.encloses(componentA.getId(), type));
		assertFalse("Should not enclose package", scope.encloses(componentA.getId(), pkg));
		assertFalse("Should not enclose A", scope.encloses(componentA.getId(), typeA));
	}
	
	/**
	 * Tests that adding a parent scope encloses its children.
	 * 
	 * @throws CoreException
	 */
	public void testEnclosesChildrenScope() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor type = pkg.getType("Activator");
		IMethodDescriptor method = type.getMethod("start", "(QBundleContext;)V");
		IApiSearchScope scope = Factory.newScope(componentA, new IElementDescriptor[]{type});
		// scope should now include everything in the type
		IFieldDescriptor field = type.getField("PLUGIN_ID");
		assertTrue("Should enclose method", scope.encloses(componentA.getId(), method));
		assertTrue("Should enclose field", scope.encloses(componentA.getId(), field));
	}	
	
	/**
	 * Tests that visiting a scope with two class files.
	 * 
	 * @throws CoreException
	 */
	public void testVisitSpecificTypes() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		Collection<String> expectedPackages = new HashSet<String>();
		expectedPackages.add("a.b.c");
		expectedPackages.add("component.a");
		Collection<String> expectedTypes = new HashSet<String>();
		IReferenceTypeDescriptor one = Factory.typeDescriptor("a.b.c.Generics");
		IReferenceTypeDescriptor two = Factory.typeDescriptor("component.a.NoExtendClass");
		expectedTypes.add(one.getQualifiedName());
		expectedTypes.add(two.getQualifiedName());
		IApiSearchScope scope = Factory.newTypeScope(componentA, new IReferenceTypeDescriptor[]{one, two});
		Collection<String> actualPackages = new HashSet<String>();
		Collection<String> actualTypes = new HashSet<String>();
		visit(scope, actualPackages, actualTypes);
		assertEquals("Different packages", expectedPackages, actualPackages);
		assertEquals("Different types", expectedTypes, actualTypes);
	}
	
}
