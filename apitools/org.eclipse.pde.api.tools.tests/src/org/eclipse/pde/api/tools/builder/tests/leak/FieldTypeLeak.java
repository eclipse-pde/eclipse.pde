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
 * Tests that API fields leaking an internal type as their type
 * are properly detected
 * 
 * @since 1.0
 */
public class FieldTypeLeak extends LeakTest {

	private int pid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public FieldTypeLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		if(pid == -1){
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE, 
					IElementDescriptor.FIELD, 
					IApiProblem.API_LEAK, 
					IApiProblem.LEAK_FIELD);
		}
		return pid;
	}
	
	/**
	 * Currently empty.
	 */
	public static Test suite() {
		return buildTestSuite(FieldTypeLeak.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.leak.LeakTest#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("field");
	}
	
	/**
	 * Tests that fields leaking API types as the field type are properly detected
	 * using a full build
	 */
	public void testFieldTypeLeak1F() {
		x1(false);
	}
	
	/**
	 * Tests that fields leaking API types as the field type are properly detected
	 * using an incremental build
	 */
	public void testFieldTypeLeak1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testFTL1";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f4"}
		});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that fields leaking API types as the field type are properly detected in inner classes
	 * using a full build
	 */
	public void testFieldTypeLeak2F() {
		x2(false);
	}
	
	/**
	 * Tests that fields leaking API types as the field type are properly detected in inner classes
	 * using an incremental build
	 */
	public void testFieldTypeLeak2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testFTL2";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f4"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "f4"}
		});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that API type leak problems are suppressed on fields with an @noreference tag
	 * using a full build
	 */
	public void testFieldTypeLeak3F() {
		x3(false);
	}
	
	/**
	 * Tests that API type leak problems are suppressed on field with an @noreference tag
	 * using an incremental build
	 */
	public void testFieldTypeLeak3I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		expectingNoProblems();
		String typename = "testFTL3";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that API type leaks are properly reported on static and final fields
	 * using a full build
	 */
	public void testFieldTypeLeak4F() {
		x4(false);
	}
	
	/**
	 * Tests that API type leaks are properly reported on static and final fields
	 * using an incremental build
	 */
	public void testFieldTypeLeak4I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testFTL4";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f4"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "f4"}
		});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that having an @noreference tag on final fields does not removed API leak problems with the field
	 * using a full build
	 */
	public void testFieldTypeLeak5F() {
		x5(false);
	}
	
	/**
	 * Tests that having an @noreference tag on final fields does not removed API leak problems with the field
	 * using an incremental build
	 */
	public void testFieldTypeLeak5I() {
		x5(true);
	}
	
	private void x5(boolean inc) {
		//TODO uncomment once bug 246139 is fixed
		/*setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testFTL5";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "f4"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "f1"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "f2"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "f3"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "f4"}
		});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);*/
	}
	
	/**
	 * Tests a field of an internal type does not create a leak when the type
	 * is not visible - a private inner type.
	 */
	public void testFieldTypeLeak6F() {
		x6(false);
	}
	
	/**
	 * Tests a field of an internal type does not create a leak when the type
	 * is not visible - a private inner type.
	 */
	public void testFieldTypeLeak6I() {
		x6(true);
	}
	
	private void x6(boolean inc) {
		String typename = "testFTL6";
		// no problems expected
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}	
}
