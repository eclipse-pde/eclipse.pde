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
		IPackageDescriptor pkg1 = Factory.packageDescriptor("");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("");
		assertEquals("Default packages should be equal", pkg1, pkg2);
		assertEquals("wrong value", "<default package>", String.valueOf(pkg1));
	}
	
	/**
	 * Tests non-equality of different packages
	 */
	public void testPackageNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f");
		assertFalse("packages should be equal", pkg1.equals(pkg2));
	}	
	
	/**
	 * Tests equality of non-default package
	 */
	public void testNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		assertEquals("a.b.c packages should be equal", pkg1, pkg2);
		assertEquals("wrong value", "a.b.c", String.valueOf(pkg1));
	}
	
	/**
	 * Tests equality of types in the default package
	 */
	public void testTypeDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		assertEquals("Types in default package should be equal", type1, type2);
	}

	/**
	 * Tests equality of inner types in the default package
	 */
	public void testInnerTypeDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IReferenceTypeDescriptor inner1 = type1.getType("B");
		IReferenceTypeDescriptor inner2 = type2.getType("B");
		assertEquals("Types in default package should be equal", inner1, inner2);
	}	
	
	
	/**
	 * Tests equality of inner types in non-default package
	 */
	public void testInnerTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IReferenceTypeDescriptor inner1 = type1.getType("B");
		IReferenceTypeDescriptor inner2 = type2.getType("B");
		assertEquals("Types in default package should be equal", inner1, inner2);
	}	
	
	/**
	 * Tests package retrieval
	 */
	public void testInnerTypePackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor inner1 = type1.getType("B");
		assertEquals("Wrong package", pkg1, inner1.getPackage());
	}		
	
	/**
	 * Tests non-equality of inner types in non-default package
	 */
	public void testInnerTypeNonDefaultPackageNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IReferenceTypeDescriptor inner1 = type1.getType("B");
		IReferenceTypeDescriptor inner2 = type2.getType("B");
		assertFalse("Types in different package should not be equal", inner1.equals(inner2));
	}	
	
	/**
	 * Tests equality of inner types in non-default package
	 */
	public void testDeepInnerTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IReferenceTypeDescriptor i1 = type1.getType("B");
		IReferenceTypeDescriptor i2 = type2.getType("B");
		IReferenceTypeDescriptor inner1 = i1.getType("C");
		IReferenceTypeDescriptor inner2 = i2.getType("C");
		assertEquals("Types in default package should be equal", inner1, inner2);
	}	
	
	/**
	 * Tests non-equality of different types
	 */
	public void testTypeNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		assertFalse("Types in different package should not be equal", type1.equals(type2));
	}	
	
	/**
	 * Tests equality of types in non-default package
	 */
	public void testTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		assertEquals("Types in default package should be equal", type1, type2);
	}
	
	/**
	 * Tests package retrieval
	 */
	public void testTypePackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		assertEquals("Wrong package", pkg1, type1.getPackage());
	}
	
	/**
	 * Tests equality of fields
	 */
	public void testFieldEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IFieldDescriptor field1 = type1.getField("name");
		IFieldDescriptor field2 = type2.getField("name");
		assertEquals("Fields should be equal", field1, field2);
	}
	
	/**
	 * Tests package retrieval
	 */
	public void testFieldPackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IFieldDescriptor field1 = type1.getField("name");
		assertEquals("Wrong package", pkg1, field1.getPackage());
	}
	
	/**
	 * Tests non-equality of fields
	 */
	public void testFieldNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IFieldDescriptor field1 = type1.getField("name");
		IFieldDescriptor field2 = type2.getField("age");
		assertFalse("Fields should not be equal", field1.equals(field2));
	}	
	
	/**
	 * Tests equality of methods without parameters
	 */
	public void testMethodNoParamsEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IMethodDescriptor m1 = type1.getMethod("foo", "()V");
		IMethodDescriptor m2 = type2.getMethod("foo", "()V");
		assertEquals("Methods should be equal", m1, m2);
	}	
	
	/**
	 * Tests equality of methods with parameters
	 */
	public void testMethodParamsEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT, type1.getSignature()}, "V"));
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT, type2.getSignature()}, "V"));
		assertEquals("Methods should be equal", m1, m2);
	}	
	
	/**
	 * Tests non-equality of methods with parameters= types
	 */
	public void testMethodParamsNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT, type1.getSignature()}, "V"));
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT, Signature.SIG_BOOLEAN}, "V"));
		assertFalse("Methods should not be equal", m1.equals(m2));
	}
	
	/**
	 * Tests non-equality of methods with different number of parameters
	 */
	public void testMethodDiffParamsNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IReferenceTypeDescriptor type2 = pkg2.getType("A");
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT}, "V"));
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT, Signature.SIG_BOOLEAN}, "V"));
		assertFalse("Methods should not be equal", m1.equals(m2));
	}	
	
	/**
	 * Tests package retrieval
	 */
	public void testMethodPackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c");
		IReferenceTypeDescriptor type1 = pkg1.getType("A");
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature(
				new String[]{Signature.SIG_INT}, "V"));
		assertEquals("Wrong package", pkg1, m1.getPackage());
	}		
			
	/**
	 * Tests reference type signature generation
	 */
	public void testTypeSignature() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("java.lang");
		IReferenceTypeDescriptor type1 = pkg1.getType("Object");
		assertEquals("Wrong signature", "Ljava.lang.Object;", type1.getSignature());
	}
	
	public void testComponent() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent");
		assertEquals("Wrong id", "com.mycomponent", descriptor.getId());
		assertNull("Wrong value", descriptor.getPath());
		assertEquals("Wrong id", "com.mycomponent", descriptor.toString());
		assertEquals("Wrong element type", IElementDescriptor.COMPONENT, descriptor.getElementType());
	}
	
}
