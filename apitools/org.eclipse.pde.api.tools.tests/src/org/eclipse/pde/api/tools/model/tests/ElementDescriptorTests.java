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
package org.eclipse.pde.api.tools.model.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Tests for element descriptors.
 *
 * @since 1.0.0
 */
public class ElementDescriptorTests {

	/**
	 * Tests equality of default package
	 */
	@Test
	public void testDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor(""); //$NON-NLS-1$
		assertEquals(pkg1, pkg2, "Default packages should be equal"); //$NON-NLS-1$
		assertEquals("<default package>", String.valueOf(pkg1), "wrong value"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests non-equality of different packages
	 */
	@Test
	public void testPackageNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f"); //$NON-NLS-1$
		assertFalse(pkg1.equals(pkg2), "packages should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of non-default package
	 */
	@Test
	public void testNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		assertEquals(pkg1, pkg2, "a.b.c packages should be equal"); //$NON-NLS-1$
		assertEquals("a.b.c", String.valueOf(pkg1), "wrong value"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests equality of types in the default package
	 */
	@Test
	public void testTypeDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		assertEquals(type1, type2, "Types in default package should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of inner types in the default package
	 */
	@Test
	public void testInnerTypeDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor(""); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = type2.getType("B"); //$NON-NLS-1$
		assertEquals(inner1, inner2, "Types in default package should be equal"); //$NON-NLS-1$
	}


	/**
	 * Tests equality of inner types in non-default package
	 */
	@Test
	public void testInnerTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = type2.getType("B"); //$NON-NLS-1$
		assertEquals(inner1, inner2, "Types in default package should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests package retrieval
	 */
	@Test
	public void testInnerTypePackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		assertEquals(pkg1, inner1.getPackage(), "Wrong package"); //$NON-NLS-1$
	}

	/**
	 * Tests non-equality of inner types in non-default package
	 */
	@Test
	public void testInnerTypeNonDefaultPackageNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = type2.getType("B"); //$NON-NLS-1$
		assertFalse(inner1.equals(inner2), "Types in different package should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of inner types in non-default package
	 */
	@Test
	public void testDeepInnerTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor i1 = type1.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor i2 = type2.getType("B"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner1 = i1.getType("C"); //$NON-NLS-1$
		IReferenceTypeDescriptor inner2 = i2.getType("C"); //$NON-NLS-1$
		assertEquals(inner1, inner2, "Types in default package should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests non-equality of different types
	 */
	@Test
	public void testTypeNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("d.e.f"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		assertFalse(type1.equals(type2), "Types in different package should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of types in non-default package
	 */
	@Test
	public void testTypeNonDefaultPackageEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		assertEquals(type1, type2, "Types in default package should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests package retrieval
	 */
	@Test
	public void testTypePackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		assertEquals(pkg1, type1.getPackage(), "Wrong package"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of fields
	 */
	@Test
	public void testFieldEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IFieldDescriptor field1 = type1.getField("name"); //$NON-NLS-1$
		IFieldDescriptor field2 = type2.getField("name"); //$NON-NLS-1$
		assertEquals(field1, field2, "Fields should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests package retrieval
	 */
	@Test
	public void testFieldPackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IFieldDescriptor field1 = type1.getField("name"); //$NON-NLS-1$
		assertEquals(pkg1, field1.getPackage(), "Wrong package"); //$NON-NLS-1$
	}

	/**
	 * Tests non-equality of fields
	 */
	@Test
	public void testFieldNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IFieldDescriptor field1 = type1.getField("name"); //$NON-NLS-1$
		IFieldDescriptor field2 = type2.getField("age"); //$NON-NLS-1$
		assertFalse(field1.equals(field2), "Fields should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of methods without parameters
	 */
	@Test
	public void testMethodNoParamsEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", "()V"); //$NON-NLS-1$ //$NON-NLS-2$
		IMethodDescriptor m2 = type2.getMethod("foo", "()V"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(m1, m2, "Methods should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests equality of methods with parameters
	 */
	@Test
	public void testMethodParamsEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, type1.getSignature()}, "V")); //$NON-NLS-1$
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, type2.getSignature()}, "V")); //$NON-NLS-1$
		assertEquals(m1, m2, "Methods should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests non-equality of methods with parameters= types
	 */
	@Test
	public void testMethodParamsNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, type1.getSignature()}, "V")); //$NON-NLS-1$
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, Signature.SIG_BOOLEAN}, "V")); //$NON-NLS-1$
		assertFalse(m1.equals(m2), "Methods should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests non-equality of methods with different number of parameters
	 */
	@Test
	public void testMethodDiffParamsNonEq() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IPackageDescriptor pkg2 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = pkg2.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT}, "V")); //$NON-NLS-1$
		IMethodDescriptor m2 = type2.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT, Signature.SIG_BOOLEAN}, "V")); //$NON-NLS-1$
		assertFalse(m1.equals(m2), "Methods should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests package retrieval
	 */
	@Test
	public void testMethodPackage() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("A"); //$NON-NLS-1$
		IMethodDescriptor m1 = type1.getMethod("foo", Signature.createMethodSignature( //$NON-NLS-1$
				new String[]{Signature.SIG_INT}, "V")); //$NON-NLS-1$
		assertEquals(pkg1, m1.getPackage(), "Wrong package"); //$NON-NLS-1$
	}

	/**
	 * Tests reference type signature generation
	 */
	@Test
	public void testTypeSignature() {
		IPackageDescriptor pkg1 = Factory.packageDescriptor("java.lang"); //$NON-NLS-1$
		IReferenceTypeDescriptor type1 = pkg1.getType("Object"); //$NON-NLS-1$
		assertEquals("Ljava.lang.Object;", type1.getSignature(), "Wrong signature"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testComponent() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent"); //$NON-NLS-1$
		assertEquals("com.mycomponent", descriptor.getId(), "Wrong id"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(descriptor.getPath(), "Wrong value"); //$NON-NLS-1$
		assertEquals("com.mycomponent", descriptor.toString(), "Wrong id"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(IElementDescriptor.COMPONENT, descriptor.getElementType(), "Wrong element type"); //$NON-NLS-1$
	}

	@Test
	public void testComponentVersion() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2.3", descriptor.getVersion(), "Wrong version"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testComponentsEqual() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		IComponentDescriptor descriptor2 = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(descriptor, descriptor2);
	}

	@Test
	public void testComponentsNotEqual() {
		IComponentDescriptor descriptor = Factory.componentDescriptor("com.mycomponent", "1.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		IComponentDescriptor descriptor2 = Factory.componentDescriptor("com.mycomponent", "2.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(descriptor.equals(descriptor2));
	}

}
