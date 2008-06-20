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
 * Tests valid uses of @noreference on interface fields
 * 
 * @since 3.4
 */
public class ValidInterfaceFieldTagTests extends ValidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidInterfaceFieldTagTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.ValidJavadocTagFieldTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidInterfaceFieldTagTests.class);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on a field in an interface
	 * using an incremental build
	 */
	public void test1I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on a field in an interface
	 * using a full build
	 */
	public void test1F() {
		deployFullBuildTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on a field in an outer interface
	 * using an incremental build
	 */
	public void test2I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on a field in an outer interface
	 * using a full build
	 */
	public void test2F() {
		deployFullBuildTest(TESTING_PACKAGE, "test2", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on a field in an inner interface
	 * using an incremental build
	 */
	public void test3I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on a field in an inner interface
	 * using a full build
	 */
	public void test3F() {
		deployFullBuildTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on fields in inner / outer interfaces
	 * using an incremental build
	 */
	public void test4I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", false);
	}
	
	/**
	 * Tests the valid use of an @noreference tag on fields in inner / outer interfaces
	 * using a full build
	 */
	public void test4F() {
		deployFullBuildTest(TESTING_PACKAGE, "test4", false);
	}
}
