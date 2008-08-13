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
package org.eclipse.pde.api.tools.builder.tests.leak;

import junit.framework.Test;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that leaked members via extends for classes is properly detected
 * 
 * @since 1.0
 */
public class ClassExtendsLeak extends LeakTest {

	/**
	 * Constructor
	 * @param name
	 */
	public ClassExtendsLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.API_LEAK, IApiProblem.ILLEGAL_EXTEND);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.InvalidFieldTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassExtendsLeak.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}

	/**
	 * Tests that an API class that extends an internal type is properly flagged
	 * as leaking using a full build
	 */
	public void testClassExtendsLeak1F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test1", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test1"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is properly flagged
	 * as leaking using an incremental build
	 */
	public void testClassExtendsLeak1I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test1", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test1"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an outer type in an API class that extends an internal type is not flagged 
	 * as a leak using a full build
	 */
	public void testClassExtendsLeak2F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test2", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test2"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an outer type in an API class that extends an internal type is not flagged 
	 * as a leak using an incremental build
	 */
	public void testClassExtendsLeak2I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test2", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test2"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner type in an API class that extends an internal type is not flagged 
	 * as a leak using a full build
	 */
	public void testClassExtendsLeak3F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test3", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test3"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner type in an API class that extends an internal type is not flagged 
	 * as a leak using an incremental build
	 */
	public void testClassExtendsLeak3I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test3", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test3"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a static inner type in an API class that extends an internal type is not flagged 
	 * as a leak using a full build
	 */
	public void testClassExtendsLeak4F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test4", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test4"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a static inner type in an API class that extends an internal type is not flagged 
	 * as a leak using an incremental build
	 */
	public void testClassExtendsLeak4I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test4", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test4"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is flagged 
	 * as a leak even with an @noextend tag being used using a full build
	 */
	public void testClassExtendsLeak5F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test5", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test5"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is flagged 
	 * as a leak even with an @noextend tag being used using an incremental build
	 */
	public void testClassExtendsLeak5I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test5", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test5"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is flagged 
	 * as a leak even with an @noinstantiate tag being used using a full build
	 */
	public void testClassExtendsLeak6F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test6", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test6"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is flagged 
	 * as a leak even with an @noinstantiate tag being used using an incremental build
	 */
	public void testClassExtendsLeak6I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test6", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test6"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is flagged 
	 * as a leak even with @noextend and @noinstantiate tags being used using a full build
	 */
	public void testClassExtendsLeak7F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test7", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test7"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API class that extends an internal type is flagged 
	 * as a leak even with @noextend and @noinstantiate tags being used using an incremental build
	 */
	public void testClassExtendsLeak7I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test7", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test7"}, 
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
}
