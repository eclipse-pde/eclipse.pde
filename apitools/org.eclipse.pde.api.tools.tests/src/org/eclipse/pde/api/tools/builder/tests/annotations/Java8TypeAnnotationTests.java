/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.annotations;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

/**
 * Tests classes with type annotations in them do not cause any problems.
 * <br><br>
 * This test class reuses the Java 8 project used for tag checking
 */
public class Java8TypeAnnotationTests extends AnnotationTest {

	public Java8TypeAnnotationTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java8TypeAnnotationTests.class);
	}
	
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
	
	public void test1I() throws Exception {
		x1(true);
	}
	
	public void test1F() throws Exception {
		x1(false);
	}
	/**
	 * Tests there are no problems with type annotations in method decls
	 * @param inc
	 * @throws Exception
	 */
	void x1(boolean inc) throws Exception{
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}
	
	public void test2I() throws Exception {
		x2(true);
	}
	
	public void test2F() throws Exception {
		x2(false);
	}
	/**
	 * Tests there are no problems with type annotations in field decls
	 * @param inc
	 * @throws Exception
	 */
	void x2(boolean inc) throws Exception{
		deployAnnotationTest("test2.java", inc, false); //$NON-NLS-1$
	}
	
	public void test3I() throws Exception {
		x3(true);
	}
	
	public void test3F() throws Exception {
		x3(false);
	}
	/**
	 * Tests there are no problems with type annotations in type decls
	 * @param inc
	 * @throws Exception
	 */
	void x3(boolean inc) throws Exception{
		deployAnnotationTest("test3.java", inc, false); //$NON-NLS-1$
	}
	
	public void test4I() throws Exception {
		x4(true);
	}
	
	public void test4F() throws Exception {
		x4(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in method decls
	 * @param inc
	 * @throws Exception
	 */
	void x4(boolean inc) throws Exception{
		deployAnnotationTestWithErrors("test4.java", inc, true); //$NON-NLS-1$
	}
	
	public void test5I() throws Exception {
		x5(true);
	}
	
	public void test5F() throws Exception {
		x5(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in field decls
	 * @param inc
	 * @throws Exception
	 */
	void x5(boolean inc) throws Exception{
		deployAnnotationTestWithErrors("test5.java", inc, true); //$NON-NLS-1$
	}
	
	public void test6I() throws Exception {
		x6(true);
	}
	
	public void test6F() throws Exception {
		x6(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in type decls
	 * @param inc
	 * @throws Exception
	 */
	void x6(boolean inc) throws Exception{
		deployAnnotationTestWithErrors("test6.java", inc, true); //$NON-NLS-1$
	}
}
