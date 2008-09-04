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
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag1F() {
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in a class
	 * using an incremental build
	 */
	public void testValidClassFieldTag2I() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag2F() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	
	/**
	 * Tests a valid @noreference tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testValidClassFieldTag3I() {
		deployTagTest(TESTING_PACKAGE, "test3", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an outer class
	 * using a full build
	 */
	public void testValidClassFieldTag3F() {
		deployTagTest(TESTING_PACKAGE, "test3", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an outer class
	 * using an incremental build
	 */
	public void testValidClassFieldTag4I() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an outer class
	 * using a full build
	 */
	public void testValidClassFieldTag4F() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag5I() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag5F() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag6I() {
		deployTagTest(TESTING_PACKAGE, "test6", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag6F() {
		deployTagTest(TESTING_PACKAGE, "test6", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on a variety of fields in inner and outer classes
	 * using an incremental build
	 */
	public void testValidClassFieldTag7I() {
		deployTagTest(TESTING_PACKAGE, "test7", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on a variety of fields in inner and outer classes
	 * using a full build
	 */
	public void testValidClassFieldTag7F() {
		deployTagTest(TESTING_PACKAGE, "test7", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testValidClassFieldTag8I() {
		deployTagTest("", "test8", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testValidClassFieldTag8F() {
		deployTagTest("", "test8", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
