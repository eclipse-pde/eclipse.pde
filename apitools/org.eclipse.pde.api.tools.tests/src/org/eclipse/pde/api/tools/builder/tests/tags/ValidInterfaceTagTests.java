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
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidInterfaceTag1I() {
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidInterfaceTag1F() {
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidInterfaceTag2I() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidInterfaceTag2F() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidInterfaceTag3I() {
		deployTagTest(TESTING_PACKAGE, "test3", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidInterfaceTag3F() {
		deployTagTest(TESTING_PACKAGE, "test3", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on a variety of inner / outer / top-level interfaces in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidInterfaceTag4I() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}

	/**
	 * Tests that @noimplement is a valid tag on a variety of inner / outer / top-level interfaces in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidInterfaceTag4F() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidInterfaceTag5I() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the 
	 * the testing package a.b.c using a full build
	 */
	public void testValidInterfaceTag5F() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests having an @noextend tag on an interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag6I() {
		deployTagTest(TESTING_PACKAGE, "test6", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests having an @noextend tag on an interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag6F() {
		deployTagTest(TESTING_PACKAGE, "test6", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}

	/**
	 * Tests having an @noextend tag on an outer interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag7I() {
		deployTagTest(TESTING_PACKAGE, "test7", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests having an @noextend tag on an outer interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag7F() {
		deployTagTest(TESTING_PACKAGE, "test7", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
