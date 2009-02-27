/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.IncrementalProjectBuilder;
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
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final enum fields
	 * using a full build
	 */
	public void testValidEnumFieldTag1F() {
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an outer enum using an incremental build
	 */
	public void testValidEnumFieldTag2I() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an outer enum using a full build
	 */
	public void testValidEnumFieldTag2F() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an inner enum using an incremental build
	 */
	public void testValidEnumFieldTag4I() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an inner enum using a full build
	 */
	public void testValidEnumFieldTag4F() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an enum in the default package using an incremental build
	 */
	public void testValidEnumFieldTag3I() {
		deployTagTest("", "test3", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an enum in the default package using a full build
	 */
	public void testValidEnumFieldTag3F() {
		deployTagTest("", "test3", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in a variety of inner / outer enums using an incremental build
	 */
	public void testValidEnumFieldTag5I() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in a variety of inner / outer enums using a full build
	 */
	public void testValidEnumFieldTag5F() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
