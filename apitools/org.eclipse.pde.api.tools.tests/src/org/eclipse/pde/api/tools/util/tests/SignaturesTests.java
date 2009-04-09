/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import junit.framework.TestCase;

import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.model.ApiField;
import org.eclipse.pde.api.tools.internal.model.ApiMethod;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Test class for the {@link Signatures} utility class
 * 
 * @since 1.0.0
 */
public class SignaturesTests extends TestCase {

	/**
	 * Constructor
	 * @param name
	 */
	public SignaturesTests(String name) {
		super(name);
	}

	/**
	 * Tests the {@link Signatures#dequalifySignature(String)} method
	 */
	public void testDequalifySignatures() {
		assertEquals("Wrong conversion", "(QObject;QException;)V", Signatures.dequalifySignature("(Ljava/lang/Object;Ljava/lang/Exception;)V"));
		assertEquals("Wrong conversion", "(QObject;QException;)QException;", Signatures.dequalifySignature("(Ljava/lang/Object;Ljava/lang/Exception;)Ljava/lang/Exception;"));
		assertEquals("Wrong conversion", "(IJCQObject;IJCQException;IJC)I", Signatures.dequalifySignature("(IJCLjava/lang/Object;IJCLjava/lang/Exception;IJC)I"));
		assertEquals("Wrong conversion", "([IJC[[[QObject;IJCQException;IJC)I", Signatures.dequalifySignature("([IJC[[[Ljava/lang/Object;IJCLjava/lang/Exception;IJC)I"));
		assertEquals("Wrong conversion", "(QObject;QException;)V", Signatures.dequalifySignature("(Ljava.lang.Object;Ljava.lang.Exception;)V"));
		assertEquals("Wrong conversion", "(QObject;QException;)QException;", Signatures.dequalifySignature("(Ljava.lang.Object;Ljava.lang.Exception;)Ljava.lang.Exception;"));
		assertEquals("Wrong conversion", "(IJCQObject;IJCQException;IJC)I", Signatures.dequalifySignature("(IJCLjava.lang.Object;IJCLjava.lang.Exception;IJC)I"));
		assertEquals("Wrong conversion", "([IJC[[[QObject;IJCQException;IJC)I", Signatures.dequalifySignature("([IJC[[[Ljava.lang.Object;IJCLjava.lang.Exception;IJC)I"));
		assertEquals("Wrong conversion", "(QList;)QList;", Signatures.dequalifySignature("(Ljava.util.List;)Ljava.util.List;"));
		assertEquals("wrong conversion", "(QList;)QList;", Signatures.dequalifySignature("(QList;)QList;"));
		assertEquals("wrong converstion", "(QLanguage;)V;", Signatures.dequalifySignature("(Lfoo.test.Language;)V;"));
		assertEquals("wrong converstion", "(QJokes;)V;", Signatures.dequalifySignature("(Lfoo.test.Jokes;)V;"));
		assertEquals("wrong conversion", "(QDiff;)Z", Signatures.dequalifySignature("(LDiff;)Z"));
		assertEquals("Wrong conversion", "(QList<QString;>;)QList;", Signatures.dequalifySignature("(Ljava.util.List<Ljava.lang.String;>;)Ljava.util.List;"));
	}
	
	/**
	 * Tests the {@link Signatures#isQualifiedSignature(String)} method
	 */
	public void testIsQualifiedSignature() {
		assertTrue("should return true", Signatures.isQualifiedSignature("(Ljava/lang/Object;Ljava/lang/Exception;)V"));
		assertTrue("should return false", !Signatures.isQualifiedSignature("(IJCQObject;IJCQException;IJC)I"));
		assertTrue("should return true", Signatures.isQualifiedSignature("(IJCLjava.lang.Object;IJCLjava.lang.Exception;IJC)I"));
		assertTrue("should return false", !Signatures.isQualifiedSignature("([IJC[[[QObject;IJCQException;IJC)I"));
	}
	
	/**
	 * Tests the {@link Signatures#processMethodSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 */
	public void testProcessMethodSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null);
		assertEquals("Signature processed incorrectly", "()V;", Signatures.processMethodSignature(method));
		method = type.addMethod("m2", "(Ljava.lang.String;)Ljava.util.List;", null, Flags.AccPublic, null);
		assertEquals("Signature processed incorrectly", "(QString;)QList;", Signatures.processMethodSignature(method));
		method = type.addMethod("m3", "(I[Ljava.lang.String;J)[Ljava.lang.Integer;", null, Flags.AccPublic, new String[] {"Ljava.lang.Throwable"});
		assertEquals("Signature processed incorrectly", "(I[QString;J)[QInteger;", Signatures.processMethodSignature(method));
		method = type.addMethod("m4", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<Ljava.lang.String;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null);
		assertEquals("Signature procesed incorrectly", "(IQList<QString;>;J)[QInteger;", Signatures.processMethodSignature(method));
	}
	
	/**
	 * Tests the {@link Signatures#getMethodName(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 * @throws Exception
	 */
	public void testGetMethodName() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong method name", "m1", Signatures.getMethodName(method));
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong method name", "Parent", Signatures.getMethodName(method));
	}
	
	/**
	 * Tests the {@link Signatures#getMethodSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 * @throws Exception
	 */
	public void testGetMethodSignature() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong method signature returned", "m1()", Signatures.getMethodSignature(method));
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong method signature returned", "Parent()", Signatures.getMethodSignature(method));
		method = type.addMethod("m3", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null);
		assertEquals("Wrong method signature returned", "m3(int, String[], long)", Signatures.getMethodSignature(method));
		method = type.addMethod("m4", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<Ljava.lang.String;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null);
		assertEquals("Wrong method signature returned", "m4(int, List<String>, long)", Signatures.getMethodSignature(method));
	}
	
	/**
	 * Tests the {@link Signatures#getQualifiedMethodSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod)} method
	 * @throws Exception
	 */
	public void testGetQualifiedMethodSignature() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		ApiMethod method = type.addMethod("m1", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent.m1()", Signatures.getQualifiedMethodSignature(method));
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent.Parent()", Signatures.getQualifiedMethodSignature(method));
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent.m2(int, String[], long)", Signatures.getQualifiedMethodSignature(method));
		method = type.addMethod("m3", "(ILjava.util.List;J)[Ljava.lang.Integer;", "(ILjava.util.List<Ljava.lang.String;>;J)[Ljava.lang.Integer;", Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent.m3(int, List<String>, long)", Signatures.getQualifiedMethodSignature(method));
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null);
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent2<T>.Parent2()", Signatures.getQualifiedMethodSignature(method));
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent2<T>.m2(int, String[], long)", Signatures.getQualifiedMethodSignature(method));
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		method = type.addMethod("<init>", "()V;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent3<T, E>.Parent3()", Signatures.getQualifiedMethodSignature(method));
		method = type.addMethod("m2", "(I[Ljava.lang.String;J)Ljava.util.List;", null, Flags.AccPublic, null);
		assertEquals("Wrong qualified method signature returned", "x.y.z.Parent3<T, E>.m2(int, String[], long)", Signatures.getQualifiedMethodSignature(method));
	}
	
	/**
	 * Tests the {@link Signatures#getTypeSignature(String, String, boolean)} method
	 */
	public void testGetTypeSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), false));
		assertEquals("Wrong type signature returned", "x.y.z.Parent", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), true));
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent2<T>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), false));
		assertEquals("Wrong type signature returned", "x.y.z.Parent2<T>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), true));
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent3<T, E>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), false));
		assertEquals("Wrong type signature returned", "x.y.z.Parent3<T, E>", Signatures.getTypeSignature(type.getSignature(), type.getGenericSignature(), true));
	}
	
	/**
	 * Tests the {@link Signatures#getTypeSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiType)} method
	 */
	public void testGetTypeSignature2() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent", Signatures.getTypeSignature(type));
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent2<T>", Signatures.getTypeSignature(type));
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent3<T, E>", Signatures.getTypeSignature(type));
		type = new ApiType(null, "Parent4", "Lx.y.z.Parent4$inner;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "Parent4.inner<T, E>", Signatures.getTypeSignature(type));
	}
	
	/**
	 * Tests the {@link Signatures#getQualifiedTypeSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiType)} method
	 */
	public void testGetQualifiedTypeSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "x.y.z.Parent", Signatures.getQualifiedTypeSignature(type));
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "x.y.z.Parent2<T>", Signatures.getQualifiedTypeSignature(type));
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "x.y.z.Parent3<T, E>", Signatures.getQualifiedTypeSignature(type));
		type = new ApiType(null, "Parent4", "Lx.y.z.Parent4$inner;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		assertEquals("Wrong type signature returned", "x.y.z.Parent4.inner<T, E>", Signatures.getQualifiedTypeSignature(type));
	}
	
	/**
	 * Tests the {@link Signatures#getAnonymousTypeName(String)} method
	 */
	public void getAnonymousTypeName() {
		assertEquals("Wrong anonymous name returned", null, Signatures.getAnonymousTypeName("Test$3"));
		assertEquals("Wrong anonymous name returned", null, Signatures.getAnonymousTypeName("x.y.z.Test$3"));
		assertEquals("Wrong anonymous name returned", null, Signatures.getAnonymousTypeName("x.y.z.Test$3$4local$5"));
		assertEquals("Wrong anonymous name returned", "local", Signatures.getAnonymousTypeName("Test$1local"));
		assertEquals("Wrong anonymous name returned", "local", Signatures.getAnonymousTypeName("x.y.z.Test$1local"));
		assertEquals("Wrong anonymous name returned", "local2", Signatures.getAnonymousTypeName("x.y.z.Test$1local$2$5local2"));
		assertEquals("Wrong anonymous name returned", null, Signatures.getAnonymousTypeName("x.y.z.Test$local"));
	}
	
	/**
	 * Tests the {@link Signatures#appendTypeParameters(StringBuffer, String[])} method
	 */
	public void testAppendTypeParameters() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Type");
		Signatures.appendTypeParameters(buffer, null);
		assertEquals("Should be no type parameters appended", "Type", buffer.toString());
		Signatures.appendTypeParameters(buffer, new String[] {});
		assertEquals("Should be no type parameters appended", "Type", buffer.toString());
		Signatures.appendTypeParameters(buffer, new String[] {"T:Ljava.lang.Object;"});
		assertEquals("Should be type parameters appended", "Type<T>", buffer.toString());
	}
	
	/**
	 * Tests the {@link Signatures#getComma()} method
	 */
	public void testGetComma() {
		assertEquals("Incorrect comma returned", ", ", Signatures.getComma());
	}
	
	/**
	 * Tests the {@link Signatures#getGT()} method
	 */
	public void testGetGT() {
		assertEquals("Incorrect '>' returned", ">", Signatures.getGT());
	}
	
	/**
	 * Tests the {@link Signatures#getLT()} method
	 */
	public void testGetLT() {
		assertEquals("Incorrect '<' returned", "<", Signatures.getLT());
	}
	
	/**
	 * Tests the {@link Signatures#getTypeName(String)} method
	 */
	public void testGetTypeName() {
		assertEquals("Wrong type name returned", "Clazz", Signatures.getTypeName("Clazz"));
		assertEquals("Wrong type name returned", "Clazz", Signatures.getTypeName("a.Clazz"));
		assertEquals("Wrong type name returned", "Clazz", Signatures.getTypeName("a.b.c.Clazz"));
		assertEquals("Wrong type name returned", "Clazz<T>", Signatures.getTypeName("Clazz<T>"));
		assertEquals("Wrong type name returned", "Clazz$Inner", Signatures.getTypeName("Clazz$Inner"));
		assertEquals("Wrong type name returned", "Clazz$Inner", Signatures.getTypeName("a.b.c.Clazz$Inner"));
	}
	
	/**
	 * Tests the {@link Signatures#matchesSignatures(String, String)} method
	 */
	public void testMatchesSignatures() {
		assertTrue("Signatures should match", Signatures.matchesSignatures("()V;", "()V;"));
		assertTrue("Signatures should match", Signatures.matchesSignatures("(ILjava.lang.String;)V;", "(ILjava.lang.String;)V;"));
		assertTrue("Signatures should match", Signatures.matchesSignatures("(ILjava.util.List<Ljava.lang.String;>;)V;", "(ILjava.util.List<Ljava.lang.String;>;)V;"));
		assertTrue("Signatures should match", Signatures.matchesSignatures("(ILjava.lang.String;)V;", "(IQString;)V;"));
		assertTrue("Signatures should match", Signatures.matchesSignatures("(ILjava.util.List<Ljava.lang.String;>;)V;", "(ILjava.util.List<QString;>;)V;"));
		assertTrue("Signatures should not match", !Signatures.matchesSignatures("(ILjava.lang.String;)V;", "(Ljava.lang.String;I)V;"));
		assertTrue("Signatures should not match", !Signatures.matchesSignatures("(ILjava.util.List<Ljava.lang.String;>;)V;", "(ILjava.util.List;)V;"));
	}
	
	/**
	 * Tests the {@link Signatures#getPackageName(String)} method
	 */
	public void testGetPackageName() {
		String pname = Signatures.getPackageName("a.b.c.Type");
		assertEquals("The package name should be 'a.b.c'", "a.b.c", pname);
		pname = Signatures.getPackageName("Type");
		assertEquals("the default package should be returned", "", pname);
	}
	
	/**
	 * Tests the {@link Signatures#getFieldSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiField)} method
	 */
	public void testGetFieldSignature() {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		ApiField field = type.addField("f1", "f1", null, Flags.AccPublic, null);
		assertEquals("Wrong field signature returned", Signatures.getFieldSignature(field), "f1");
	}
	
	/**
	 * Tests the {@link Signatures#getQualifiedFieldSignature(org.eclipse.pde.api.tools.internal.provisional.model.IApiField)} method
	 */
	public void testGetQualifiedFieldSignature() throws Exception {
		ApiType type = new ApiType(null, "Parent", "Lx.y.z.Parent;", null, Flags.AccPublic, null, null);
		ApiField field = type.addField("f1", "f1", null, Flags.AccPublic, null);
		assertEquals("Wrong field signature returned", Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent.f1");
		type = new ApiType(null, "Parent2", "Lx.y.z.Parent2;", "<T:Ljava/lang/Object;>", Flags.AccPublic, null, null);
		field = type.addField("f1", "f1", null, Flags.AccPublic, null);
		assertEquals("Wrong field signature returned", Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent2<T>.f1");
		type = new ApiType(null, "Parent3", "Lx.y.z.Parent3;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		field = type.addField("f1", "f1", null, Flags.AccPublic, null);
		assertEquals("Wrong field signature returned", Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent3<T, E>.f1");
		type = new ApiType(null, "Parent4", "Lx.y.z.Parent4$inner;", "<T:Ljava/lang/Object;E::Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;>", Flags.AccPublic, null, null);
		field = type.addField("f1", "f1", null, Flags.AccPublic, null);
		assertEquals("Wrong field signature returned", Signatures.getQualifiedFieldSignature(field), "x.y.z.Parent4.inner<T, E>.f1");
	}
	
	/**
	 * Tests the {@link Signatures#getPrimaryTypeName(String)} method
	 */
	public void testGetPrimaryTypeName() {
		assertEquals("the type name x.y.z should have been returned", "x.y.z.Type", Signatures.getPrimaryTypeName("x.y.z.Type$Member"));
		assertEquals("the type name x.y.z should have been returned", "x.y.z.Type", Signatures.getPrimaryTypeName("x.y.z.Type"));
		assertEquals("the type name x.y.z should have been returned", "x.y.z.Type", Signatures.getPrimaryTypeName("x.y.z.Type$Member$Member"));
	}
	
	/**
	 * Tests the {@link Signatures#getSimpleTypeName(String)} method
	 */
	public void testGetSimpleTypeName() {
		assertEquals("the type name Type should have been returned", "Type", Signatures.getSimpleTypeName("a.b.c.Type"));
		assertEquals("the type name Type$Member should have been returned", "Type$Member", Signatures.getSimpleTypeName("a.b.c.Type$Member"));
		assertEquals("the type name Type should have been returned", "Type", Signatures.getSimpleTypeName("Type"));
	}
}
