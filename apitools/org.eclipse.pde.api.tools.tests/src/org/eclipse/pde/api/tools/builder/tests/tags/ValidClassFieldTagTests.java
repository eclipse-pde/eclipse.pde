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

import org.eclipse.core.runtime.IPath;

/**
 * Tests valid use of the @noreference tag on fields in classes
 * 
 * @since 3.4
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
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag1F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in a class
	 * using an incremental build
	 */
	public void testValidClassFieldTag2I() {
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test2", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag2F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test2", false);
	}
	
	
	/**
	 * Tests a valid @noreference tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testValidClassFieldTag3I() {
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an outer class
	 * using a full build
	 */
	public void testValidClassFieldTag3F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an outer class
	 * using an incremental build
	 */
	public void testValidClassFieldTag4I() {
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test4", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an outer class
	 * using a full build
	 */
	public void testValidClassFieldTag4F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test4", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag5I() {
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test5", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag5F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test5", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag6I() {
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test6", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three static fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag6F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test6", false);
	}
	
	/**
	 * Tests a valid @noreference tag on a variety of fields in inner and outer classes
	 * using an incremental build
	 */
	public void testValidClassFieldTag7I() {
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test7", false);
	}
	
	/**
	 * Tests a valid @noreference tag on a variety of fields in inner and outer classes
	 * using a full build
	 */
	public void testValidClassFieldTag7F() {
		deployFullBuildTagTest(TESTING_PACKAGE, "test7", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testValidClassFieldTag8I() {
		deployIncrementalBuildTagTest("", "test8", false);
	}
	
	/**
	 * Tests a valid @noreference tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testValidClassFieldTag8F() {
		deployFullBuildTagTest("", "test8", false);
	}
}
