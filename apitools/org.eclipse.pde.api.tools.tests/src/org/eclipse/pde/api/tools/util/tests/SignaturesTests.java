/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.model.ApiField;
import org.eclipse.pde.api.tools.internal.model.ApiMethod;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.junit.jupiter.api.Test;

/**
 * Test class for the {@link Signatures} utility class
 *
 * @since 1.0.0
 */
public class SignaturesTests {

	/**
	 * Tests the {@link Signatures#dequalifySignature(String)} method
	 */
	@Test
	public void testDequalifySignatures() {
		assertEquals("(QObject;QException;)V", Signatures.dequalifySignature("(Ljava/lang/Object;Ljava/lang/Exception;)V"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QObject;QException;)QException;", Signatures.dequalifySignature("(Ljava/lang/Object;Ljava/lang/Exception;)Ljava/lang/Exception;"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(IJCQObject;IJCQException;IJC)I", Signatures.dequalifySignature("(IJCLjava/lang/Object;IJCLjava/lang/Exception;IJC)I"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("([IJC[[[QObject;IJCQException;IJC)I", Signatures.dequalifySignature("([IJC[[[Ljava/lang/Object;IJCLjava/lang/Exception;IJC)I"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QObject;QException;)V", Signatures.dequalifySignature("(Ljava.lang.Object;Ljava.lang.Exception;)V"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QObject;QException;)QException;", Signatures.dequalifySignature("(Ljava.lang.Object;Ljava.lang.Exception;)Ljava.lang.Exception;"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(IJCQObject;IJCQException;IJC)I", Signatures.dequalifySignature("(IJCLjava.lang.Object;IJCLjava.lang.Exception;IJC)I"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("([IJC[[[QObject;IJCQException;IJC)I", Signatures.dequalifySignature("([IJC[[[Ljava.lang.Object;IJCLjava.lang.Exception;IJC)I"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QList;)QList;", Signatures.dequalifySignature("(Ljava.util.List;)Ljava.util.List;"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QList;)QList;", Signatures.dequalifySignature("(QList;)QList;"), "wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QLanguage;)V;", Signatures.dequalifySignature("(Lfoo.test.Language;)V;"), "wrong converstion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QJokes;)V;", Signatures.dequalifySignature("(Lfoo.test.Jokes;)V;"), "wrong converstion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QDiff;)Z", Signatures.dequalifySignature("(LDiff;)Z"), "wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QList<QString;>;)QList;", Signatures.dequalifySignature("(Ljava.util.List<Ljava.lang.String;>;)Ljava.util.List;"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(QList<+QCharSequence;>;)QList;", Signatures.dequalifySignature("(Ljava.util.List<+Ljava.lang.CharSequence;>;)Ljava.util.List;"), "Wrong conversion"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Tests the {@link Signatures#isQualifiedSignature(String)} method
	 */
	@Test
	public void testIsQualifiedSignature() {
		assertTrue(Signatures.isQualifiedSignature("(Ljava/lang/Object;Ljava/lang/Exception;)V"), "should return true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(Signatures.isQualifiedSignature("(IJCQObject;IJCQException;IJC)I"), "should return false"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(Signatures.isQualifiedSignature("(IJCLjava.lang.Object;IJCLjava.lang.Exception;IJC)I"), "should return true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(Signatures.isQualifiedSignature("([IJC[[[QObject;IJCQException;IJC)I"), "should return false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#processMethodSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 */
	@Test
	public void testProcessMethodSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("()V;", Signatures.processMethodSignature(method), "Signature processed incorrectly"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m2", "(Ljava.lang.String;)Ljava.util.List;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("(QString;)QList;", Signatures.processMethodSignature(method), "Signature processed incorrectly"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m3", "(I[Ljava.lang.String;J)[Ljava.lang.Integer;", null, Flags.AccPublic, new String[] {"Ljava.lang.Throwable"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(I[QString;J)[QInteger;", Signatures.processMethodSignature(method), "Signature processed incorrectly"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m4", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<Ljava.lang.String;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(IQList<QString;>;J)[QInteger;", Signatures.processMethodSignature(method), "Signature procesed incorrectly"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m5", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<+Ljava.lang.CharSequence;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("(IQList<+QCharSequence;>;J)[QInteger;", Signatures.processMethodSignature(method), "Signature procesed incorrectly"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getMethodName(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 */
	@Test
	public void testGetMethodName() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("m1", Signatures.getMethodName(method), "Wrong method name"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Parent", Signatures.getMethodName(method), "Wrong method name"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getMethodSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 */
	@Test
	public void testGetMethodSignature() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("m1()", Signatures.getMethodSignature(method), "Wrong method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Parent()", Signatures.getMethodSignature(method), "Wrong method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m3", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("m3(int, String[], long)", Signatures.getMethodSignature(method), "Wrong method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m4", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<Ljava.lang.String;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("m4(int, List<String>, long)", Signatures.getMethodSignature(method), "Wrong method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m5", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<+Ljava.lang.CharSequence;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("m5(int, List<? extends CharSequence>, long)", Signatures.getMethodSignature(method), "Wrong method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getQualifiedMethodSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 */
	@Test
	public void testGetQualifiedMethodSignature() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);  //$NON-NLS-1$//$NON-NLS-2$
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent.m1()", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent.Parent()", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent.m2(int, String[], long)", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m3", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<Ljava.lang.String;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("x.y.z.Parent.m3(int, List<String>, long)", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent2<T>.Parent2()", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent2<T>.m2(int, String[], long)", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent3<T, E>.Parent3()", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent3<T, E>.m2(int, String[], long)", Signatures.getQualifiedMethodSignature(method), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getQualifiedMethodSignature(IMethodDescriptor, boolean, boolean)} method
	 */
	@Test
	public void testGetQualifiedMethodSignature2() throws Exception {
		ApiType type = new ApiType(null, "x.y.z.Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent.m1() : void", Signatures.getQualifiedMethodSignature((IMethodDescriptor) method.getHandle(), false, true), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent.m2(int, String[], long) : java.util.List", Signatures.getQualifiedMethodSignature((IMethodDescriptor) method.getHandle(), false, true), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "x.y.z.Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent2.Parent2() : void", Signatures.getQualifiedMethodSignature((IMethodDescriptor) method.getHandle(), false, true), "Wrong qualified method signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getTypeSignature(String, String, boolean)} method
	 */
	@Test
	public void testGetTypeSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Parent", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), false), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), true), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Parent2<T>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), false), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent2<T>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), true), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Parent3<T, E>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), false), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent3<T, E>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), true), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getTypeSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiType)} method
	 */
	@Test
	public void testGetTypeSignature2() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Parent", Signatures.getTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Parent2<T>", Signatures.getTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Parent3<T, E>", Signatures.getTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent4", "Lx.y.z.Parent4$inner;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Parent4.inner<T, E>", Signatures.getTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getQualifiedTypeSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiType)} method
	 */
	@Test
	public void testGetQualifiedTypeSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("x.y.z.Parent", Signatures.getQualifiedTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("x.y.z.Parent2<T>", Signatures.getQualifiedTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("x.y.z.Parent3<T, E>", Signatures.getQualifiedTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent4", "Lx.y.z.Parent4$inner;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("x.y.z.Parent4.inner<T, E>", Signatures.getQualifiedTypeSignature(type), "Wrong type signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getAnonymousTypeName(String)} method
	 */
	public void getAnonymousTypeName() {
		assertEquals(null, Signatures.getAnonymousTypeName("Test$3"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(null, Signatures.getAnonymousTypeName("x.y.z.Test$3"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(null, Signatures.getAnonymousTypeName("x.y.z.Test$3$4local$5"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("local", Signatures.getAnonymousTypeName("Test$1local"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("local", Signatures.getAnonymousTypeName("x.y.z.Test$1local"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("local2", Signatures.getAnonymousTypeName("x.y.z.Test$1local$2$5local2"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(null, Signatures.getAnonymousTypeName("x.y.z.Test$local"), "Wrong anonymous name returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#appendTypeParameters(StringBuilder, String[])}
	 * method
	 */
	@Test
	public void testAppendTypeParameters() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Type"); //$NON-NLS-1$
		Signatures.appendTypeParameters(buffer, null);
		assertEquals("Type", buffer.toString(), "Should be no type parameters appended"); //$NON-NLS-1$ //$NON-NLS-2$
		Signatures.appendTypeParameters(buffer, new String[] {});
		assertEquals("Type", buffer.toString(), "Should be no type parameters appended"); //$NON-NLS-1$ //$NON-NLS-2$
		Signatures.appendTypeParameters(buffer, new String[] {"T:Ljava.lang.Object;"}); //$NON-NLS-1$
		assertEquals("Type<T>", buffer.toString(), "Should be type parameters appended"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getComma()} method
	 */
	@Test
	public void testGetComma() {
		assertEquals(", ", Signatures.getComma(), "Incorrect comma returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getGT()} method
	 */
	@Test
	public void testGetGT() {
		assertEquals(">", Signatures.getGT(), "Incorrect '>' returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getLT()} method
	 */
	@Test
	public void testGetLT() {
		assertEquals("<", Signatures.getLT(), "Incorrect '<' returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getTypeName(String)} method
	 */
	@Test
	public void testGetTypeName() {
		assertEquals("Clazz", Signatures.getTypeName("Clazz"), "Wrong type name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Clazz", Signatures.getTypeName("a.Clazz"), "Wrong type name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Clazz", Signatures.getTypeName("a.b.c.Clazz"), "Wrong type name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Clazz<T>", Signatures.getTypeName("Clazz<T>"), "Wrong type name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Clazz$Inner", Signatures.getTypeName("Clazz$Inner"), "Wrong type name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Clazz$Inner", Signatures.getTypeName("a.b.c.Clazz$Inner"), "Wrong type name returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Tests the {@link Signatures#matchesSignatures(String, String)} method
	 */
	@Test
	public void testMatchesSignatures() {
		assertTrue(Signatures.matchesSignatures("()V;", "()V;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matchesSignatures("(ILjava.lang.String;)V;", "(ILjava.lang.String;)V;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matchesSignatures("(ILjava.util.List<Ljava.lang.String;>;)V;", "(ILjava.util.List<Ljava.lang.String;>;)V;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matchesSignatures("(ILjava.lang.String;)V;", "(IQString;)V;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matchesSignatures("(ILjava.util.List<Ljava.lang.String;>;)V;", "(ILjava.util.List<QString;>;)V;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matchesSignatures("(ILjava.lang.String;)V;", "(Ljava.lang.String;I)V;"), "Signatures should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matchesSignatures("(ILjava.util.List<Ljava.lang.String;>;)V;", "(ILjava.util.List;)V;"), "Signatures should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMatchesInnerClasses() {
		assertTrue(Signatures.matchesSignatures("(QInnerMost;)V", "(Lmy.package.Class$InnerMost;)V"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matchesSignatures("(QInnerMost;)V", "(Lmy.package.Class$Inner$InnerMost;)V"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMatchesArgumentPackages() {
		assertTrue(Signatures.matchesSignatures("(Lmy.package.Class;)V", "(Lmy.package.Class;)V"), "Same packages should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matchesSignatures("(Lmy.package.Class;)V", "(Lother.package.Class;)V"), "Differing packages should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matchesSignatures("(Lmy.package.Class$InnerMost;)V", "(Lother.package.Class$InnerMost;)V"), "Differing packages should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMatchesGenericsBug456945_ex1() {
		// This is the example from bug 456945
		assertTrue(Signatures.matchesSignatures("(QIterable<+QCharSequence;>;QIAcceptor<QResult;>;)V", "(Ljava.lang.Iterable<+Ljava.lang.CharSequence;>;Lorg.eclipse.xtext.util.IAcceptor<Lorg.eclipse.xtext.xbase.compiler.CompilationTestHelper$Result;>;)V"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMatchesGenericsBug456945_ex2() {
		// Modified example that included parameterized return type
		assertTrue(Signatures.matchesSignatures("(QIterable<+QCharSequence;>;QIAcceptor<QResult;>;)QT;", "(Ljava.lang.Iterable<+Ljava.lang.CharSequence;>;Lorg.eclipse.xtext.util.IAcceptor<Lorg.eclipse.xtext.xbase.compiler.CompilationTestHelper$Result;>;)TT;"), "Wildcard signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMatchesGenericsBug334281_c1() {
		// Example from bug 334281 comment 1
		assertTrue(Signatures.matchesSignatures("(QClass<+QITmfEvent;>;QTmfTimeRange;JII)V", "(Ljava.lang.Class<+Lorg.eclipse.linuxtools.tmf.core.event.ITmfEvent;>;Lorg.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;JII)V"), "Wildcard signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// and heck, let's ensure arrays work too
		assertTrue(Signatures.matchesSignatures("(QClass<[+QITmfEvent;>;QTmfTimeRange;JII)V", "(Ljava.lang.Class<[+Lorg.eclipse.linuxtools.tmf.core.event.ITmfEvent;>;Lorg.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;JII)V"), "Array wildcard signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMatchesMismatchedGenericTypes() {
		assertFalse(Signatures.matchesSignatures("(QIterable<QCharSequence;>;)V", "(QList<QCharSequence;>;)V"), "Different generic type names should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/** Tests the {@link Signatures#matches(String, String)} method */
	@Test
	public void testTypeMatches() {
		assertTrue(Signatures.matches("I", "I"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QClass;", "Ljava.lang.Class;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("Ljava.lang.Class;", "Ljava.lang.Class;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QClass;", "QClass;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QClass<QITmfEvent;>;", "Ljava.lang.Class<Lorg.eclipse.linuxtools.tmf.core.event.ITmfEvent;>;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matches("QIterable<QCharSequence;>;", "QList<QCharSequence;>;"), "Signatures should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matches("QList<QCharSequence;>;", "Ljava.lang.Iterable<QCharSequence;>;"), "Signatures should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matches("Lmy.package.Class;", "Lother.package.Class;"), "Differing packages should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matches("Lmy.package.Class$InnerMost;", "Lother.package.Class$InnerMost;"), "Differing packages should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QInnerMost;", "Lmy.package.Class$InnerMost;"), "Inner class should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QInnerMost;", "Lmy.package.Class$Inner$InnerMost;"), "Signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QClass<-QITmfEvent;>;", "Ljava.lang.Class<-Lorg.eclipse.linuxtools.tmf.core.event.ITmfEvent;>;"), "Super signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QClass<+QITmfEvent;>;", "Ljava.lang.Class<+Lorg.eclipse.linuxtools.tmf.core.event.ITmfEvent;>;"), "Extends signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(Signatures.matches("QClass<*>;", "Ljava.lang.Class<*>;"), "Unbound wildcard signatures should match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matches("QClass<+QITmfEvent;>;", "Ljava.lang.Class<-Lorg.eclipse.linuxtools.tmf.core.event.ITmfEvent;>;"), "Different wildcard signatures should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(Signatures.matches("QClass<+QITmfEvent;>;", "Ljava.lang.Class<*>;"), "Different wildcard signatures should not match"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Tests the {@link Signatures#getPackageName(String)} method
	 */
	@Test
	public void testGetPackageName() {
		String pname = Signatures.getPackageName("a.b.c.Type"); //$NON-NLS-1$
		assertEquals("a.b.c", pname, "The package name should be 'a.b.c'"); //$NON-NLS-1$ //$NON-NLS-2$
		pname = Signatures.getPackageName("Type"); //$NON-NLS-1$
		assertEquals("", pname, "the default package should be returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getFieldSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiField)} method
	 */
	@Test
	public void testGetFieldSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ApiField field = type.addField("f1", "f1", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Signatures.getFieldSignature(field), "f1", "Wrong field signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getQualifiedFieldSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiField)} method
	 */
	@Test
	public void testGetQualifiedFieldSignature() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ApiField field = type.addField("f1", "f1", null, Flags.AccPublic, null);  //$NON-NLS-1$//$NON-NLS-2$
		assertEquals(Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent.f1", "Wrong field signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		field = type.addField("f1", "f1", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent2<T>.f1", "Wrong field signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		field = type.addField("f1", "f1", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent3<T, E>.f1", "Wrong field signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
		type = new ApiType(null, "Parent4", "Lx.y.z.Parent4$inner;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		field = type.addField("f1", "f1", null, Flags.AccPublic, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent4.inner<T, E>.f1", "Wrong field signature returned"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the {@link Signatures#getPrimaryTypeName(String)} method
	 */
	@Test
	public void testGetPrimaryTypeName() {
		assertEquals("x.y.z.Type", Signatures.getPrimaryTypeName("x.y.z.Type$Member"), "the type name x.y.z should have been returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("x.y.z.Type", Signatures.getPrimaryTypeName("x.y.z.Type"), "the type name x.y.z should have been returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("x.y.z.Type", Signatures.getPrimaryTypeName("x.y.z.Type$Member$Member"), "the type name x.y.z should have been returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Tests the {@link Signatures#getSimpleTypeName(String)} method
	 */
	@Test
	public void testGetSimpleTypeName() {
		assertEquals("Type", Signatures.getSimpleTypeName("a.b.c.Type"), "the type name Type should have been returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Type$Member", Signatures.getSimpleTypeName("a.b.c.Type$Member"), "the type name Type$Member should have been returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Type", Signatures.getSimpleTypeName("Type"), "the type name Type should have been returned"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
