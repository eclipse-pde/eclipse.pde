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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that an API type that implements an internal type
 * is properly detected
 * 
 * @since 1.0
 */
public class ClassImplementsLeak extends LeakTest {

	/**
	 * Constructor
	 * @param name
	 */
	public ClassImplementsLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.API_LEAK, IApiProblem.LEAK_IMPLEMENTS);
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
		return buildTestSuite(ClassImplementsLeak.class);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using a full build
	 */
	public void testClassImplementsLeak1F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test8", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test8"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testClassImplementsLeak1I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test8", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test8"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an outer class that implements an internal interface is not flagged
	 * using a full build
	 */
	public void testClassImplementsLeak2F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test9", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test9"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an outer class that implements an internal interface is not flagged
	 * using an incremental build
	 */
	public void testClassImplementsLeak2I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test9", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test9"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner class that implements an internal interface is not flagged
	 * using a full build
	 */
	public void testClassImplementsLeak3F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test10", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test10"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner class that implements an internal interface is not flagged
	 * using an incremental build
	 */
	public void testClassImplementsLeak3I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test10", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test10"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a static inner class that implements an internal interface is not flagged
	 * using a full build
	 */
	public void testClassImplementsLeak4F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test11", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test11"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a static inner class that implements an internal interface is not flagged
	 * using an incremental build
	 */
	public void testClassImplementsLeak4I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test11", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test11"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using a full build even with an @noextend tag
	 */
	public void testClassImplementsLeak5F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test12", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test12"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using an incremental build even with an @noextend tag
	 */
	public void testClassImplementsLeak5I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test12", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test12"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using a full build even with an @noinstantiate tag
	 */
	public void testClassImplementsLeak6F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test13", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test13"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using an incremental build even with an @noinstantiate tag
	 */
	public void testClassImplementsLeak6I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test13", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test13"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using a full build even with an @noinstantiate and an @noextend tag
	 */
	public void testClassImplementsLeak7F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test14", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test14"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a class that implements an internal interface is properly flagged
	 * using an incremental build even with an @noinstantiate and an @noextend tag
	 */
	public void testClassImplementsLeak7I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test14", TESTING_INTERNAL_INTERFACE_NAME},
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test14"}, 
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
}
