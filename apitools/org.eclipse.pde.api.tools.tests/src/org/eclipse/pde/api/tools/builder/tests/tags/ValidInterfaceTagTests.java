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
package org.eclipse.pde.api.tools.builder.tests.tags;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;

/**
 * Tests the tags that are valid on an interface
 * 
 * @since 1.0
 */
public class ValidInterfaceTagTests extends InvalidInterfaceTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidInterfaceTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidInterfaceTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.InvalidJavadocTagClassTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid");
	}
	
	public void testValidInterfaceTag1I() {
		x1(true);
	}

	public void testValidInterfaceTag1F() {
		x1(false);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false);
	}
	
	public void testValidInterfaceTag2I() {
		x2(true);
	}

	public void testValidInterfaceTag2F() {
		x2(false);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c 
	 */
	private void x2(boolean inc) {
		deployTagTest("test2.java", inc, false);
	}
	
	public void testValidInterfaceTag3I() {
		x3(true);
	}
	
	public void testValidInterfaceTag3F() {
		x3(false);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c 
	 */
	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false);
	}

	public void testValidInterfaceTag4I() {
		x4(true);
	}

	public void testValidInterfaceTag4F() {
		x4(false);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on a variety of inner / outer / top-level interfaces in the 
	 * the testing package a.b.c
	 */
	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false);
	}

	public void testValidInterfaceTag5I() {
		x5(true);
	}

	public void testValidInterfaceTag5F() {
		x5(false);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c
	 */
	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false);
	}
	
	public void testInvalidInterfaceTag6I() {
		x6(true);
	}
	
	public void testInvalidInterfaceTag6F() {
		x6(false);
	}

	/**
	 * Tests having an @noextend tag on an interface in package a.b.c
	 */
	private void x6(boolean inc) {
		deployTagTest("test6.java", inc, false);
	}

	public void testInvalidInterfaceTag7I() {
		x7(true);
	}
	
	public void testInvalidInterfaceTag7F() {
		x7(false);
	}
	
	/**
	 * Tests having an @noextend tag on an outer interface in package a.b.c
	 */
	private void x7(boolean inc) {
		deployTagTest("test7.java", inc, false);
	}
}
