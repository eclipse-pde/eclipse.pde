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

import org.eclipse.core.runtime.IPath;

import junit.framework.Test;

/**
 * Tests the tags that are valid on a class
 * 
 * @since 3.4
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
	public void test1I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", false);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a class in the 
	 * the testing package a.b.c using a full build
	 */
	public void test1F() {
		deployFullBuildTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an outer class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void test2I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", false);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an outer class in the 
	 * the testing package a.b.c using a full build
	 */
	public void test2F() {
		deployFullBuildTest(TESTING_PACKAGE, "test2", false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void test3I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", false);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using a full build
	 */
	public void test3F() {
		deployFullBuildTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a variety of inner / outer / top-level classes in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void test4I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", false);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a variety of inner / outer / top-level classes in the 
	 * the testing package a.b.c using a full build
	 */
	public void test4F() {
		deployFullBuildTest(TESTING_PACKAGE, "test4", false);
	}
	
	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void test5I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test5", false);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the 
	 * the testing package a.b.c using a full build
	 */
	public void test5F() {
		deployFullBuildTest(TESTING_PACKAGE, "test5", false);
	}
}
