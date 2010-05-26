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
 * Tests the tags that are valid on a class
 * 
 * @since 1.0
 */
public class ValidClassTagTests extends InvalidClassTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidClassTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.InvalidJavadocTagClassTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidClassTagTests.class);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag1I() {
		x1(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a class in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag1F() {
		x1(false);
	}
	
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an outer class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag2I() {
		x2(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an outer class in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag2F() {
		x2(false);
	}
	
	private void x2(boolean inc) {
		deployTagTest("test2.java", inc, false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag3I() {
		x3(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag3F() {
		x3(false);
	}
	
	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a variety of inner / outer / top-level classes in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag4I() {
		x4(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a variety of inner / outer / top-level classes in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag4F() {
		x4(false);
	}
	
	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag5I() {
		x5(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag5F() {
		x5(false);
	}
	
	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false);
	}
}
