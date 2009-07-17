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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests invalid duplicate tags placed on members
 * 
 * @since 1.0.0
 */
public class InvalidDuplicateTagsTests extends TagTest {

	private int fPid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public InvalidDuplicateTagsTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return fPid;
	}

	/**
	 * Must be called before a call {@link #getDefaultProblemId()}
	 * @param element
	 * @param kind
	 */
	private void setProblemId(int element, int kind) {
		fPid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, 
				element, 
				kind, 
				IApiProblem.NO_FLAGS);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidDuplicateTagsTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("duplicates");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * Tests a class that has duplicate tags is properly detected using an incremental build 
	 */
	public void testClassWithDuplicateTagsI() {
		x1(true);
	}
	
	/**
	 * Tests a class that has duplicate tags is properly detected using a full build
	 */
	public void testClassWithDuplicateTagsF() {
		x1(false);
	}
	
	private void x1(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noextend"},
				{"@noinstantiate"},
				{"@noextend"},
				{"@noinstantiate"}
		});
		deployTagTest("test1.java", inc, false);
	}
	
	/**
	 * Tests that an interface with duplicate tags is properly using an incremental build
	 */
	public void testInterfaceWithDuplicateTagsI() {
		x2(true);
	}
	
	/**
	 * Tests that an interface with duplicate tags is properly detected using a full build
	 */
	public void testInterfaceWithDuplicateTagsF() {
		x2(false);
	}
	
	private void x2(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement"},
				{"@noimplement"},
				{"@noimplement"}
		});
		deployTagTest("test2.java", inc, false);
	}
	
	/**
	 * Tests that a class field with duplicate tags is properly detected using an incremental build
	 */
	public void testClassFieldWithDuplicateTagsI() {
		x3(true);
	}
	
	/**
	 * Tests that a class field with duplicate tags is properly detected using a full build
	 */
	public void testClassFieldWithDuplicateTagsF() {
		x3(false);
	}
	
	private void x3(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noreference"},
				{"@noreference"},
				{"@noreference"}
		});
		deployTagTest("test3.java", inc, false);
	}
	
	/**
	 * Tests that an interface field with duplicate tags is properly detected using an incremental build
	 */
	public void testInterfaceFieldWithDuplicateTagsI() {
		x4(true);
	}
	
	/**
	 * Tests that an interface field with duplicate tags is properly detected using a full build
	 */
	public void testInterfaceFieldWithDuplicateTagsF() {
		x4(false);
	}
	
	private void x4(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noreference"},
				{"@noreference"},
				{"@noreference"}	
		});
		deployTagTest("test4.java", inc, false);
	}
	
	/**
	 * Tests that an enum field with duplicate tags is properly detected using an incremental build
	 */
	public void testEnumFieldWithDuplicateTagsI() {
		x5(true);
	}
	
	/**
	 * Tests that an enum field with duplicate tags is properly detected using a full build
	 */
	public void testEnumFieldWithDuplicateTagsF() {
		x5(false);
	}
	
	private void x5(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"}
		});
		deployTagTest("test5.java", inc, false);
	}
	
	/**
	 * Tests that a class method with duplicate tags is properly detected using an incremental build
	 */
	public void testClassMethoddWithDuplicateTagsI() {
		x6(true);
	}
	
	/**
	 * Tests that a class method with duplicate tags is properly detected using a full build
	 */
	public void testClassMethodWithDuplicateTagsF() {
		x6(false);
	}
	
	private void x6(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@nooverride"},
				{"@nooverride"},
				{"@nooverride"}
		});
		deployTagTest("test6.java", inc, false);
	}
	
	/**
	 * Tests that an interface method with duplicate tags is properly detected using an incremental build
	 */
	public void testInterfaceMethoddWithDuplicateTagsI() {
		x7(true);
	}
	
	/**
	 * Tests that an interface method with duplicate tags is properly detected using a full build
	 */
	public void testInterfaceMethodWithDuplicateTagsF() {
		x7(false);
	}
	
	private void x7(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
		});
		deployTagTest("test7.java", inc, false);
	}
	
	/**
	 * Tests that an enum method with duplicate tags is properly detected using an incremental build
	 */
	public void testEnumMethodWithDuplicateTagsI() {
		x8(true);
	}
	
	/**
	 * Tests that an interface method with duplicate tags is properly detected using a full build
	 */
	public void testEnumMethodWithDuplicateTagsF() {
		x8(false);
	}
	
	private void x8(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
				{"@noreference"},
		});
		deployTagTest("test8.java", inc, false);
	}
}
