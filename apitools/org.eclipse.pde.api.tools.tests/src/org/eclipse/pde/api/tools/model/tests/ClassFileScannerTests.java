/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
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

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.test.OrderedTestSuite;

import junit.framework.Test;

/**
 * This class tests the class file scanner and the class file visitor
 *
 * @since 1.0.0
 */
public class ClassFileScannerTests extends ScannerTest {

	private static IPath WORKSPACE_ROOT = TestSuiteHelper.getPluginDirectoryPath().append("test_classes_workspace"); //$NON-NLS-1$
	private static IPath ROOT_PATH = TestSuiteHelper.getPluginDirectoryPath().append("test-source").append("classes"); //$NON-NLS-1$ //$NON-NLS-2$;

	/**
	 * The ordered {@link Test} suite to run
	 *
	 * @return the {@link Test} suite
	 */
	public static Test suite() {
		return new OrderedTestSuite(ClassFileScannerTests.class, new String[] { "testScanEmptyClass", //$NON-NLS-1$
				"testScanEmptyGenericClass", //$NON-NLS-1$
				"testScanInnerClass", //$NON-NLS-1$
				"testScanInnerStaticClass", //$NON-NLS-1$
				"testScanInnerStaticInnerClass", //$NON-NLS-1$
				"testScanOuterClass", //$NON-NLS-1$
				"testScanInnerOuterClass", //$NON-NLS-1$
				"testScanInnerGenericClass", //$NON-NLS-1$
				"testScanInnerStaticInnerGenericClass", //$NON-NLS-1$
				"testScanClassExtendsImplements", //$NON-NLS-1$
				"testScanGenericClassExtendsImplements", //$NON-NLS-1$
				"testScanMethodDecl", //$NON-NLS-1$
				"testScanMethodDeclArrayTypes", //$NON-NLS-1$
				"testScanMethodDeclGenerics", //$NON-NLS-1$
				"testScanFieldDecl", //$NON-NLS-1$
				"testScanLocalVariableArrays", //$NON-NLS-1$
				"testScanConstantPoolAccess", //$NON-NLS-1$
				"testScanConstantPoolAccess1_4", //$NON-NLS-1$
				"testScanMethodCalls", //$NON-NLS-1$
				"testCleanup", //$NON-NLS-1$
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.api.tools.model.tests.ScannerTest#getWorkspaceRoot()
	 */
	@Override
	protected IPath getWorkspaceRoot() {
		return WORKSPACE_ROOT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.api.tools.model.tests.ScannerTest#getPackageName()
	 */
	@Override
	protected String getPackageName() {
		return "classes"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.api.tools.model.tests.ScannerTest#getSourcePath()
	 */
	@Override
	protected IPath getSourcePath() {
		return ROOT_PATH;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.api.tools.model.tests.ScannerTest#doCompile()
	 */
	@Override
	protected boolean doCompile() {
		boolean result = true;
		String[] sourceFilePaths = new String[] { ROOT_PATH.toOSString() };
		result &= TestSuiteHelper.compile(sourceFilePaths, WORKSPACE_ROOT.toOSString(), TestSuiteHelper.getCompilerOptions());
		assertTrue("working directory should compile", result); //$NON-NLS-1$
		result &= TestSuiteHelper.compile(ROOT_PATH.append("Test12.java").toOSString(), WORKSPACE_ROOT.toOSString(), new String[] {"-1.4", "-preserveAllLocals", "-nowarn" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("Test12 should compile to 1.4", result); //$NON-NLS-1$
		return result;
	}

	/**
	 * Tests scanning a simple class file that extends nothing, implements
	 * nothing and has no members
	 */
	public void testScanEmptyClass() throws CoreException {
		List<IReference> refs = getRefSet("Test1"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test1", null, "java.lang.Object", null, IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test1", "<init>", "java.lang.Object", null, IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a ref to java.lang.Object in the default constructor", ref != null); //$NON-NLS-1$
	}

	/**
	 * Test scanning a simple generic class file that extends nothing,
	 * implements nothing and has no members
	 */
	public void testScanEmptyGenericClass() throws CoreException {
		List<IReference> refs = getRefSet("Test2"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test2", null, "java.lang.Object", null, IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("There should be an extends ref to java.lang.Object for an empty class", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test2", "<init>", "java.lang.Object", null, IReference.REF_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a ref to java.lang.Object in the default constructor", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test2", null, "java.lang.Object", null, IReference.REF_PARAMETERIZED_TYPEDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("There should be a parameterized type ref to java.lang.Object", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests scanning an empty inner class
	 */
	public void testScanInnerClass() throws CoreException {
		List<IReference> refs = getRefSet("Test3$Inner"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test3$Inner", null, "java.lang.Object", null, IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test3$Inner", "<init>", "java.lang.Object", null, IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning a empty static inner class
	 */
	public void testScanInnerStaticClass() throws CoreException {
		List<IReference> refs = getRefSet("Test3$Inner2"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test3$Inner2", "java.lang.Object", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test3$Inner2", "java.lang.Object", IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning an empty inner class of an empty inner static class
	 */
	public void testScanInnerStaticInnerClass() throws CoreException {
		List<IReference> refs = getRefSet("Test3$Inner2$Inner3"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test3$Inner2$Inner3", "java.lang.Object", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test3$Inner2$Inner3", "java.lang.Object", IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning an empty outer class
	 */
	public void testScanOuterClass() throws CoreException {
		List<IReference> refs = getRefSet("Test3Outer"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test3Outer", "java.lang.Object", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test3Outer", "java.lang.Object", IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning an empty class of the inner class of an outer class
	 */
	public void testScanInnerOuterClass() throws CoreException {
		List<IReference> refs = getRefSet("Test3Outer$Inner"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test3Outer$Inner", "java.lang.Object", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test3Outer$Inner", "java.lang.Object", IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning an inner static generic type
	 */
	public void testScanInnerGenericClass() throws CoreException {
		List<IReference> refs = getRefSet("Test4$Inner"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test4$Inner", "java.lang.Object", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("There should be an extends ref to java.lang.Object for an inner empty class", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test4$Inner", "java.lang.Object", IReference.REF_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
		ref = findReference("classes.Test4$Inner", "java.lang.Object", IReference.REF_PARAMETERIZED_TYPEDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning an inner class of a static class of a generic type
	 */
	public void testScanInnerStaticInnerGenericClass() throws CoreException {
		List<IReference> refs = getRefSet("Test4$Inner$Inner2"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test4$Inner$Inner2", "java.lang.Object", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("There should be an extends ref to java.lang.Object for an inner empty class", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test4$Inner$Inner2", "java.lang.Object", IReference.REF_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
		ref = findReference("classes.Test4$Inner$Inner2", "java.lang.Object", IReference.REF_PARAMETERIZED_TYPEDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning a non-generic class that extends something and implements
	 * interfaces
	 */
	public void testScanClassExtendsImplements() throws CoreException {
		List<IReference> refs = getRefSet("Test5"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test5", "java.util.ArrayList", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an extends reference to java.util.ArrayList", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test5", "java.lang.Iterable", IReference.REF_IMPLEMENTS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an implements reference to java.lang.Iterable", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test5", "classes.ITest5", IReference.REF_IMPLEMENTS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be an implements reference to classes.ITest5", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test5", "java.util.ArrayList", IReference.REF_SUPER_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", //$NON-NLS-1$
				ref != null);
	}

	/**
	 * Tests scanning a generic class that extends something and implements
	 * interfaces
	 */
	public void testScanGenericClassExtendsImplements() throws CoreException {
		List<IReference> refs = getRefSet("Test6"); //$NON-NLS-1$
		IReference ref = findReference("classes.Test6", "classes.Test6Abstract", IReference.REF_CONSTRUCTORMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTRUCTORMETHOD ref to classes.Test6Abstract", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.lang.Iterable", IReference.REF_IMPLEMENTS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_IMPLEMENTS ref to java.lang.Iterable", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.util.ArrayList", IReference.REF_PARAMETERIZED_TYPEDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_PARAMETERIZED_TYPEDECL ref to java.util.ArrayList", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.util.Map", IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref to java.util.Map", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.lang.String", IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.lang.String", IReference.REF_PARAMETERIZED_TYPEDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_PARAMETERIZED_TYPEDECL ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.util.Iterator", IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_RETURNTYPE ref to java.util.Iterator", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "java.util.Map", IReference.REF_PARAMETERIZED_TYPEDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_PARAMETERIZED_TYPEDECL ref to java.util.Map", ref != null); //$NON-NLS-1$
		ref = findReference("classes.Test6", "classes.Test6Abstract", IReference.REF_EXTENDS, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_EXTENDS ref to classes.Test6Abstract", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests a variety of method declarations
	 */
	public void testScanMethodDecl() throws CoreException {
		List<IReference> refs = getRefSet("Test7"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test7", "m1", "java.lang.String", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m1 should have a REF_RETURNTYPE ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m1", "java.lang.String", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m1 should have a REF_PARAMETER ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m2", "java.lang.String", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m2 should have a REF_RETURNTYPE ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m2", "java.lang.String", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m2 should have a REF_PARAMETER ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m3", "java.lang.String", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m3 should have a REF_RETURNTYPE ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m3", "java.lang.String", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m3 should have a REF_PARAMETER ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m7", "java.lang.String", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m7 should have a REF_RETURNTYPE ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m7", "java.lang.String", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m7 should have a REF_PARAMETER ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m8", "java.lang.String", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m8 should have a REF_RETURNTYPE ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m8", "java.lang.String", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m8 should have a REF_PARAMETER ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m9", "java.lang.String", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m9 should have a REF_RETURNTYPE ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m9", "java.lang.String", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m9 should have a REF_PARAMETER ref to java.lang.String", ref != null); //$NON-NLS-1$

	}

	/**
	 * Tests a variety of method declarations with array types in them
	 */
	public void testScanMethodDeclArrayTypes() throws CoreException {
		List<IReference> refs = getRefSet("Test7"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test7", "m4", "java.lang.Integer", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m4 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m4", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m4 should have a REF_PARAMETER ref to java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m5", "java.lang.Integer", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m5 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m5", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m5 should have a REF_PARAMETER ref to java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m6", "java.lang.Integer", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m6 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m6", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m6 should have a REF_PARAMETER ref to java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m10", "java.lang.Integer", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m10 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m10", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m10 should have a REF_PARAMETER ref to java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m11", "java.lang.Integer", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m11 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m11", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m11 should have a REF_PARAMETER ref to java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m12", "java.lang.Integer", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m12 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test7", "m12", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("m12 should have a REF_PARAMETER ref to java.lang.Double", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests a variety of method declarations with generic types
	 */
	public void testScanMethodDeclGenerics() throws CoreException {
		List<IReference> refs = getRefSet("Test8"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test8", "m1", "java.util.ArrayList", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_RETURNTYPE ref for m1", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m1", "java.util.Map", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETER ref for m1", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m1", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETER ref for m1 for java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m1", "java.lang.Integer", null, IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m1 for java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m1", "java.lang.String", null, IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m1 for java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m1", "classes.Test8Outer", null, IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m1 for classes.Test8Outer", ref != null); //$NON-NLS-1$

		ref = findMemberReference("classes.Test8", "m2", "java.util.ArrayList", null, IReference.REF_RETURNTYPE, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_RETURNTYPE ref for m2", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m2", "java.util.Map", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETER ref for m2", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m2", "java.lang.Double", null, IReference.REF_PARAMETER, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETER ref for m2 for java.lang.Double", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m2", "java.lang.Integer", null, IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m2 for java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m2", "java.lang.String", null, IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m2 for java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test8", "m2", "classes.Test8Outer", null, IReference.REF_PARAMETERIZED_METHODDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m2 for classes.Test8Outer", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests a variety of field declarations
	 */
	public void testScanFieldDecl() throws CoreException {
		List<IReference> refs = getRefSet("Test9"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test9", "strs", "java.lang.String", null, IReference.REF_FIELDDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_FIELDDECL ref for java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test9", "list", "java.util.ArrayList", null, IReference.REF_FIELDDECL, refs); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_FIELDDECL ref for java.util.ArrayList", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test9", "object", "java.lang.Object", null, IReference.REF_FIELDDECL, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("there should be a REF_FIELDDECL ref for java.lang.Object", ref != null); //$NON-NLS-1$
		// TODO does not collect ref to Runnable in Test9 as there is no direct
		// ref to Runnable in that classfile

	}

	/**
	 * Tests a variety of arrays that have been declared as local variables in
	 * methods
	 */
	public void testScanLocalVariableArrays() throws CoreException {
		List<IReference> refs = getRefSet("Test10"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test10", null, "java.lang.String", null, IReference.REF_ARRAYALLOC, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.String", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test10", null, "java.lang.Object", null, IReference.REF_ARRAYALLOC, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.Object", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test10", null, "java.lang.Integer", null, IReference.REF_ARRAYALLOC, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test10", null, "java.lang.Double", null, IReference.REF_ARRAYALLOC, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.Double", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests a variety of LDC ops that load things like Integer.class onto the
	 * stack
	 */
	public void testScanConstantPoolAccess() throws CoreException {
		List<IReference> refs = getRefSet("Test11"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test11", null, "java.lang.Integer", null, IReference.REF_CONSTANTPOOL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test11", null, "java.lang.Double", null, IReference.REF_CONSTANTPOOL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test11", null, "java.lang.String", null, IReference.REF_CONSTANTPOOL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests a variety of LDC ops that load things like Integer.class onto the
	 * stack. This method uses a 1.4 code level class, and checks that the LDC
	 * ref is actually processed via a Class.forName static method call
	 */
	public void testScanConstantPoolAccess1_4() throws CoreException {
		List<IReference> refs = getRefSet("Test12"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test12", null, "java.lang.Integer", null, IReference.REF_CONSTANTPOOL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test12", null, "java.lang.Double", null, IReference.REF_CONSTANTPOOL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test12", null, "java.lang.String", null, IReference.REF_CONSTANTPOOL, refs); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests a variety of method calls
	 */
	public void testScanMethodCalls() throws CoreException {
		List<IReference> refs = getRefSet("Test13"); //$NON-NLS-1$
		IReference ref = findMemberReference("classes.Test13", "m1", "classes.Test13", "m2", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("the should be a REF_VIRTUALMETHOD ref to m2 from classes.Test13", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test13", "m1", "classes.Test13", "m3", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("the should be a REF_VIRTUALMETHOD ref to m3 from classes.Test13", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test13", "m4", "classes.Test13A", "getInteger", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("the should be a REF_VIRTUALMETHOD ref to getInteger from classes.Test13A", ref != null); //$NON-NLS-1$
		ref = findMemberReference("classes.Test13", "m3", "classes.Test13A", "doSomething", IReference.REF_STATICMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("the should be a REF_STATICMETHOD ref to doSomething from classes.Test13A", ref != null); //$NON-NLS-1$
	}

	/**
	 * Cleans up after the tests are done. This must be the last test run
	 *
	 * @throws Exception
	 */
	public void testCleanup() throws Exception {
		cleanUp();
		// remove workspace root
		assertTrue(TestSuiteHelper.delete(new File(WORKSPACE_ROOT.toOSString())));

	}
}
