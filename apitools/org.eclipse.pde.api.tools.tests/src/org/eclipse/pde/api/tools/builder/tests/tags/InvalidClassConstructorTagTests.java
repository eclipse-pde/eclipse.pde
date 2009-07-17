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
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests unsupported javadoc tags on class constructors
 * 
 * @since 1.0
 */
public class InvalidClassConstructorTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassConstructorTagTests(String name) {
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
		return buildTestSuite(InvalidClassConstructorTagTests.class);
	}

	public void testInvalidClassMethodTag1I() {
		x1(true);
	}
	
	public void testInvalidClassMethodTag1F() {
		x1(false);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a variety of inner / outer classes 
	 * is detected properly
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor}
		});
		deployTagTest("test15.java", inc, false);
	}

	public void testInvalidClassMethodTag2I() {
		x2(true);
	}
	
	public void testInvalidClassMethodTag2F() {
		x2(false);
	}

	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a class in the default package 
	 * is detected properly
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_constructor},
				{"@noreference", BuilderMessages.TagValidator_private_constructor}
		});
		deployTagTest("test16.java", inc, true);
	}

	public void testInvalidClassMethodTag3I() {
		x3(true);
	}
	
	public void testInvalidClassMethodTag3F() {
		x3(false);
	}

	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test17.java", inc, false);
	}
	
	public void testInvalidClassMethodTag4I() {
		x4(true);
	}
	
	public void testInvalidClassMethodTag4F() {
		x4(false);
	}

	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a class in the default package 
	 * is detected properly
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor},
				{"@noinstantiate", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test18.java", inc, true);
	}
	
	public void testInvalidClassMethodTag5I() {
		x5(true);
	}
	
	public void testInvalidClassMethodTag5F() {
		x5(false);
	}

	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor},
				{"@noextend", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test19.java", inc, false);
	}

	public void testInvalidClassMethodTag6I() {
		x6(true);
	}
	
	public void testInvalidClassMethodTag6F() {
		x6(false);
	}

	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a class in the default package 
	 * is detected properly
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_constructor},	
				{"@noextend", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test20.java", inc, true);
	}

	public void testInvalidClassMethodTag7I() {
		x7(true);
	}
	
	public void testInvalidClassMethodTag7F() {
		x7(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test21.java", inc, false);
	}

	public void testInvalidClassMethodTag8I() {
		x8(true);
	}
	
	
	public void testInvalidClassMethodTag8F() {
		x8(false);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a class in the default package 
	 * is detected properly
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_constructor},
				{"@nooverride", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test22.java", inc, true);
	}
	
	public void testInvalidClassMethodTag9I() {
		x9(true);
	}
	
	public void testInvalidClassMethodTag9F() {
		x9(false);
	}

	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test23.java", inc, false);
	}

	public void testInvalidClassMethodTag10I() {
		x10(true);
	}
	
	public void testInvalidClassMethodTag10F() {
		x10(false);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a class in the default package 
	 * is detected properly
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_constructor},
				{"@noimplement", BuilderMessages.TagValidator_a_constructor}
		});
		deployTagTest("test24.java", inc, true);
	}
}
