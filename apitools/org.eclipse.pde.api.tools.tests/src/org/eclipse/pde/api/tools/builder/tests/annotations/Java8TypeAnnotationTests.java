/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.annotations;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.junit.jupiter.api.Test;

/**
 * Tests classes with type annotations in them do not cause any problems.
 * <br><br>
 * This test class reuses the Java 8 project used for tag checking
 */
public class Java8TypeAnnotationTests extends AnnotationTest {

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("java8").append("types"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected int getDefaultProblemId() {
		return -1;
	}

	@Override
	protected String getTestingProjectName() {
		return "java8tags"; //$NON-NLS-1$
	}

	@Test

	public void test1I() throws Exception {
		x1(true);
	}

	@Test

	public void test1F() throws Exception {
		x1(false);
	}
	/**
	 * Tests there are no problems with type annotations in method decls
	 */
	void x1(boolean inc) throws Exception{
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void test2I() throws Exception {
		x2(true);
	}

	@Test

	public void test2F() throws Exception {
		x2(false);
	}
	/**
	 * Tests there are no problems with type annotations in field decls
	 */
	void x2(boolean inc) throws Exception{
		deployAnnotationTest("test2.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void test3I() throws Exception {
		x3(true);
	}

	@Test

	public void test3F() throws Exception {
		x3(false);
	}
	/**
	 * Tests there are no problems with type annotations in type decls
	 */
	void x3(boolean inc) throws Exception{
		deployAnnotationTest("test3.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void test4I() throws Exception {
		x4(true);
	}

	@Test

	public void test4F() throws Exception {
		x4(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in method decls
	 */
	void x4(boolean inc) throws Exception{
		deployAnnotationTestWithErrors("test4.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void test5I() throws Exception {
		x5(true);
	}

	@Test

	public void test5F() throws Exception {
		x5(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in field decls
	 */
	void x5(boolean inc) throws Exception{
		deployAnnotationTestWithErrors("test5.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void test6I() throws Exception {
		x6(true);
	}

	@Test

	public void test6F() throws Exception {
		x6(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in type decls
	 */
	void x6(boolean inc) throws Exception{
		deployAnnotationTestWithErrors("test6.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void test7I() throws Exception {
		x7(true);
	}

	@Test

	public void test7F() throws Exception {
		x7(false);
	}

	/**
	 * Tests there are no problems with multiple type annotations at locations
	 * class, interface, class field, meth param,method,local var
	 */
	void x7(boolean inc) throws Exception {
		deployAnnotationTest("test7.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void test8I() throws Exception {
		x8(true);
	}

	@Test

	public void test8F() throws Exception {
		x8(false);
	}

	/**
	 * Tests there are problems with type annotation whose location is different
	 * than defined in the target
	 */
	void x8(boolean inc) throws Exception {
		deployAnnotationTestWithErrors("test8.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void test9I() throws Exception {
		x9(true);
	}

	@Test

	public void test9F() throws Exception {
		x9(false);
	}

	/**
	 * Tests there are problems with type annotation whose attribute is not set
	 * or if it doesnt have a default value
	 */
	void x9(boolean inc) throws Exception {
		deployAnnotationTestWithErrors("test9.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void test10I() throws Exception {
		x10(true);
	}

	@Test

	public void test10F() throws Exception {
		x10(false);
	}

	/**
	 * Tests there are no errors when attribute is not set for type annotation
	 */
	void x10(boolean inc) throws Exception {
		deployAnnotationTest("test10.java", inc, false); //$NON-NLS-1$
	}

}
