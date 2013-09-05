/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Tests for element descriptors.
 * 
 * @since 1.0.0
 */
public class ElementDescriptorTests extends TestCase {

	public static Test suite() {
		return new TestSuite(ElementDescriptorTests.class);
	}	
	
	public ElementDescriptorTests() {
		super();
	}
	
	public ElementDescriptorTests(String name) {
		super(name);
	}
	
	/**
	 * Tests equality of default package
	 */
	public void testDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor(""); //$NON-NLS-1$
		assertEquals("Default packages should be equal", pkg1, pkg2); //$NON-NLS-1$
		assertEquals("wrong value", "<default package>", String.valueOf(pkg1)); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Tests non-equality of different packages
	 */
	public void testPackageNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f"); //$NON-NLS-1$
		assertFalse("packages should be equal", pkg1.equals(pkg2)); //$NON-NLS-1$
	}	
	
	/**
	 * Tests equality of non-default package
	 */
	public void testNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		assertEquals("a.b.c packages should be equal", pkg1, pkg2); //$NON-NLS-1$
		assertEquals("wrong value", "a.b.c", String.valueOf(pkg1)); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Tests equality of types in the default package
	 */
	public void testTypeDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		assertEquals("Types in default package should be equal", type1, type2); //$NON-NLS-1$
	}

	/**
	 * Tests equality of inner types in the default package
	 */
	public void testInnerTypeDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = type2.getType("B"); //$NON-NLS-1$
		assertEquals("Types in default package should be equal", inner1, inner2); //$NON-NLS-1$
	}	
	
	
	/**
	 * Tests equality of inner types in non-default package
	 */
	public void testInnerTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = type2.getType("B"); //$NON-NLS-1$
		assertEquals("Types in default package should be equal", inner1, inner2); //$NON-NLS-1$
	}	
	
	/**
	 * Tests package retrieval
	 */
	public void testInnerTypePackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		assertEquals("Wrong package", pkg1, inner1.getPackage()); //$NON-NLS-1$
	}		
	
	/**
	 * Tests non-equality of inner types in non-default package
	 */
	public void testInnerTypeNonDefaultPackageNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = type2.getType("B"); //$NON-NLS-1$
		assertFalse("Types in different package should not be equal", inner1.equals(inner2)); //$NON-NLS-1$
	}	
	
	/**
	 * Tests equality of inner types in non-default package
	 */
	public void testDeepInnerTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor i1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor i2 = type2.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = i1.getType("C"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = i2.getType("C"); //$NON-NLS-1$
		assertEquals("Types in default package should be equal", inner1, inner2); //$NON-NLS-1$
	}	
	
	/**
	 * Tests non-equality of different types
	 */
	public void testTypeNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		assertFalse("Types in different package should not be equal", type1.equals(type2)); //$NON-NLS-1$
	}	
	
	/**
	 * Tests equality of types in non-default package
	 */
	public void testTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		assertEquals("Types in default package should be equal", type1, type2); //$NON-NLS-1$
	}
	
	/**
	 * Tests package retrieval
	 */
	public void testTypePackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		assertEquals("Wrong package", pkg1, type1.getPackage()); //$NON-NLS-1$
	}
	
	/**
	 * Tests equality of fields
	 */
	public void testFieldEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IFieldDescriptor field1 = type1.getField("name"); //$NON-NLS-1$
		IFieldDescriptor field2 = type2.getField("name"); //$NON-NLS-1$
		assertEquals("Fields should be equal", field1, field2); //$NON-NLS-1$
	}
	
	/**
	 * Tests package retrieval
	 */
	public void testFieldPackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IFieldDescriptor field1 = type1.getField("name"); //$NON-NLS-1$
		assertEquals("Wrong package", pkg1, field1.getPackage()); //$NON-NLS-1$
	}
	
	/**
	 * Tests non-equality of fields
	 */
	public void testFieldNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IFieldDescriptor field1 = type1.getField("name"); //$NON-NLS-1$
		IFieldDescriptor field2 = type2.getField("age"); //$NON-NLS-1$
		assertFalse("Fields should not be equal", field1.equals(field2)); //$NON-NLS-1$
	}	
	
	/**
	 * Tests equality of methods without parameters
	 */
	public void testMethodNoParamsEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", "()V"); //$NON-NLS-1$ //$NON-NLS-2$
		IMethodDescriptor m2 = type2.getMethod("foo", "()V"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Methods should be equal", m1, m2); //$NON-NLS-1$
	}	
	
	/**
	 * Tests equality of methods with parameters
	 */
	public void testMethodParamsEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, type1.getSignature()}, "V")); //$NON-NLS-1$
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, type2.getSignature()}, "V")); //$NON-NLS-1$
		assertEquals("Methods should be equal", m1, m2); //$NON-NLS-1$
	}	
	
	/**
	 * Tests non-equality of methods with parameters= types
	 */
	public void testMethodParamsNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, type1.getSignature()}, "V")); //$NON-NLS-1$
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, Signature.SIG_BOOLEAN}, "V")); //$NON-NLS-1$
		assertFalse("Methods should not be equal", m1.equals(m2)); //$NON-NLS-1$
	}
	
	/**
	 * Tests non-equality of methods with different number of parameters
	 */
	public void testMethodDiffParamsNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT}, "V")); //$NON-NLS-1$
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, Signature.SIG_BOOLEAN}, "V")); //$NON-NLS-1$
		assertFalse("Methods should not be equal", m1.equals(m2)); //$NON-NLS-1$
	}	
	
	/**
	 * Tests package retrieval
	 */
	public void testMethodPackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT}, "V")); //$NON-NLS-1$
		assertEquals("Wrong package", pkg1, m1.getPackage()); //$NON-NLS-1$
	}		
			
	/**
	 * Tests reference type signature generation
	 */
	public void testTypeSignature() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("java.lang"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("Object"); //$NON-NLS-1$
		assertEquals("Wrong signature", "Ljava.lang.Object;", type1.getSignature()); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testComponent() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent"); //$NON-NLS-1$
		assertEquals("Wrong id", "com.mycomponent", descriptor.getId()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("Wrong value", descriptor.getPath()); //$NON-NLS-1$
		assertEquals("Wrong id", "com.mycomponent", descriptor.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Wrong element type", IElementDescriptor.COMPONENT, descriptor.getElementType()); //$NON-NLS-1$
	}
	
	public void testComponentVersion() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Wrong version", "1.2.3", descriptor.getVersion()); //$NON-NLS-1$ //$NON-NLS-2$
	}	
	
	public void testComponentsEqual() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		IComponentDescriptor descriptor2 = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(descriptor, descriptor2);
	}
	
	public void testComponentsNotEqual() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		IComponentDescriptor descriptor2 = Factory.componentDescriptor("com.mycomponent", "2.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(descriptor.equals(descriptor2));
	}	
	
}
