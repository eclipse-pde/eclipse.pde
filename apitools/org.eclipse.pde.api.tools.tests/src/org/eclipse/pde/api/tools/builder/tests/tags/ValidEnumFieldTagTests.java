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
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Tests valid use of @noreference tags in an enum
 * 
 * @since 1.0
 */
public class ValidEnumFieldTagTests extends ValidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidEnumFieldTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.ValidFieldTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidEnumFieldTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final enum fields
	 * using an incremental build
	 */
	public void testValidEnumFieldTag1I() {
		x1(true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final enum fields
	 * using a full build
	 */
	public void testValidEnumFieldTag1F() {
		x1(false);
	}
	
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an outer enum using an incremental build
	 */
	public void testValidEnumFieldTag2I() {
		x2(true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an outer enum using a full build
	 */
	public void testValidEnumFieldTag2F() {
		x2(false);
	}
	
	private void x2(boolean inc) {
		deployTagTest("test2.java", inc, false);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an inner enum using an incremental build
	 */
	public void testValidEnumFieldTag4I() {
		x4(true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an inner enum using a full build
	 */
	public void testValidEnumFieldTag4F() {
		x4(false);
	}
	
	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an enum in the default package using an incremental build
	 */
	public void testValidEnumFieldTag3I() {
		x3(true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an enum in the default package using a full build
	 */
	public void testValidEnumFieldTag3F() {
		x3(false);
	}
	
	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in a variety of inner / outer enums using an incremental build
	 */
	public void testValidEnumFieldTag5I() {
		x5(true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in a variety of inner / outer enums using a full build
	 */
	public void testValidEnumFieldTag5F() {
		x5(false);
	}
	
	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false);
	}
}
