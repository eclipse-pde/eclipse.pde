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

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests invalid javadoc tags on class methods
 * 
 * @since 1.0
 */
public class InvalidClassMethodTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassMethodTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassMethodTagTests.class);
	}
	
	public void testInvalidClassMethodTag1I() {
		x1(true);
	}
	
	
	public void testInvalidClassMethodTag1F() {
		x1(false);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_final_method},
				{"@noimplement", BuilderMessages.TagValidator_a_static_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_final_method},
				{"@noimplement", BuilderMessages.TagValidator_a_static_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_final_method},
				{"@noimplement", BuilderMessages.TagValidator_a_static_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_final_method},
				{"@noimplement", BuilderMessages.TagValidator_a_static_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test1", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassMethodTag2I() {
		x2(true);
	}

	public void testInvalidClassMethodTag2F() {
		x2(false);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a class in the default package 
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_method},
				{"@noimplement", BuilderMessages.TagValidator_a_final_method},
				{"@noimplement", BuilderMessages.TagValidator_a_static_method},
				{"@noimplement", BuilderMessages.TagValidator_a_method},
		});
		deployTagTest("", 
				"test2", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag3I() {
		x3(true);
	}
	
	public void testInvalidClassMethodTag3F() {
		x3(false);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_final_method},
				{"@noextend", BuilderMessages.TagValidator_a_static_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_final_method},
				{"@noextend", BuilderMessages.TagValidator_a_static_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_final_method},
				{"@noextend", BuilderMessages.TagValidator_a_static_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_final_method},
				{"@noextend", BuilderMessages.TagValidator_a_static_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test3", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag4I() {
		x4(true);
	}

	public void testInvalidClassMethodTag4F() {
		x4(false);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a class in the default package 
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_method},
				{"@noextend", BuilderMessages.TagValidator_a_final_method},
				{"@noextend", BuilderMessages.TagValidator_a_static_method},
				{"@noextend", BuilderMessages.TagValidator_a_method},
		});
		deployTagTest("", 
				"test4", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag5I() {
		x5(true);
	}
	
	public void testInvalidClassMethodTag5F() {
		x5(false);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_final_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_static_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_final_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_static_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_final_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_static_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_final_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_static_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test5", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag6I() {
		x6(true);
	}

	public void testInvalidClassMethodTag6F() {
		x6(false);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a class in the default package 
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_final_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_static_method},
				{"@noinstantiate", BuilderMessages.TagValidator_a_method},
		});
		deployTagTest("", 
				"test6", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag7I() {
		x7(true);
	}
	
	public void testInvalidClassMethodTag7F() {
		x7(false);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a variety of inner /outer classes
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_private_method},
				{"@nooverride", BuilderMessages.TagValidator_private_method},
				{"@nooverride", BuilderMessages.TagValidator_private_method},
				{"@nooverride", BuilderMessages.TagValidator_private_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test7", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassMethodTag8I() {
		x8(true);
	}

	public void testInvalidClassMethodTag8F() {
		x8(false);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a class in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_private_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test8", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassMethodTag9I() {
		x9(true);
	}
	
	public void testInvalidClassMethodTag9F() {
		x9(false);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a variety of inner /outer classes
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_final_method},
				{"@nooverride", BuilderMessages.TagValidator_a_final_method},
				{"@nooverride", BuilderMessages.TagValidator_a_final_method},
				{"@nooverride", BuilderMessages.TagValidator_a_final_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test9", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag10I() {
		x10(true);
	}

	public void testInvalidClassMethodTag10F() {
		x10(false);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a class in the default package
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_final_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test10", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag11I() {
		x11(true);
	}

	public void testInvalidClassMethodTag11F() {
		x11(false);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a variety of inner /outer classes
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_method},
				{"@noreference", BuilderMessages.TagValidator_private_method},
				{"@noreference", BuilderMessages.TagValidator_private_method},
				{"@noreference", BuilderMessages.TagValidator_private_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test11", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassMethodTag12I() {
		x12(true);
	}
	
	
	public void testInvalidClassMethodTag12F() {
		x12(false);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a class in the default package
	 */
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_method},
		});
		deployTagTest("", 
				"test12", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassMethodTag13I() {
		x13(true);
	}

	public void testInvalidClassMethodTag13F() {
		x13(false);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a static method
	 */
	private void x13(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_static_method},
				{"@nooverride", BuilderMessages.TagValidator_a_static_method},
				{"@nooverride", BuilderMessages.TagValidator_a_static_final_method},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test13", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassMethodTag14I() {
		x14(true);
	}
	
	public void testInvalidClassMethodTag14F() {
		x14(false);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a method in a final class
	 */
	private void x14(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class},
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class},
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class},
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test14", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
}
