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
 * Tests that an API interface leaking an internal type via extends is
 * flagged properly
 * 
 * @since 1.0
 */
public class InterfaceExtendsLeak extends LeakTest {

	/**
	 * Constructor
	 */
	public InterfaceExtendsLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.API_LEAK, IApiProblem.LEAK_EXTENDS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.leak.LeakTest#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InterfaceExtendsLeak.class);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak1F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test1", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test1"},
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak1I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test1", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test1"},
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an outer API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak2F() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test2", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test2"},
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an outer API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak2I() {
		expectingNoProblems();
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test2", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test2"},
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak3F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test3", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test3"},
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak3I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test3", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test3"},
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a static inner API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak4F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test4", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test4"},
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that a static inner API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak4I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test4", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test4"},
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner interface in an outer interface in an API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak5F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test5", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test5"},
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an inner interface in an outer interface in an API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak5I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test5", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test5"},
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using a full build even with an @noimplement tag on it
	 */
	public void testInterfaceExtendsLeak6F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test6", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test6"},
				true, 
				IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using an incremental build even with an @noimplement tag on it
	 */
	public void testInterfaceExtendsLeak6I() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test6", TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test6"},
				true, 
				IncrementalProjectBuilder.INCREMENTAL_BUILD, 
				true);
	}
}
