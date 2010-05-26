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
 * Tests valid use of the @noreference tag on fields in classes
 * 
 * @since 1.0
 */
public class ValidClassFieldTagTests extends ValidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidClassFieldTagTests(String name) {
		super(name);
	}

	/**
	 * @return the test suite for class fields
	 */
	public static Test suite() {
		return buildTestSuite(ValidClassFieldTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.ValidJavadocTagFieldTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class
	 * using an incremental build
	 */
	public void testValidClassFieldTag1I() {
		x1(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag1F() {
		x1(false);
	}
	
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in a class
	 * using an incremental build
	 */
	public void testValidClassFieldTag2I() {
		x2(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag2F() {
		x2(false);
	}
	
	private void x2(boolean inc) {
		deployTagTest("test2.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testValidClassFieldTag3I() {
		x3(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an outer class
	 * using a full build
	 */
	public void testValidClassFieldTag3F() {
		x3(false);
	}
	
	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an outer class
	 * using an incremental build
	 */
	public void testValidClassFieldTag4I() {
		x4(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an outer class
	 * using a full build
	 */
	public void testValidClassFieldTag4F() {
		x4(false);
	}
	
	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag5I() {
		x5(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag5F() {
		x5(false);
	}
	
	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag6I() {
		x6(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag6F() {
		x6(false);
	}
	
	private void x6(boolean inc) {
		deployTagTest("test6.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on a variety of fields in inner and outer classes
	 * using an incremental build
	 */
	public void testValidClassFieldTag7I() {
		x7(true);
	}
	
	/**
	 * Tests a valid @noreference tag on a variety of fields in inner and outer classes
	 * using a full build
	 */
	public void testValidClassFieldTag7F() {
		x7(false);
	}
	
	private void x7(boolean inc) {
		deployTagTest("test7.java", inc, false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testValidClassFieldTag8I() {
		x8(true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testValidClassFieldTag8F() {
		x8(false);
	}
	
	private void x8(boolean inc) {
		deployTagTest("test8.java", inc, true);
	}
}
