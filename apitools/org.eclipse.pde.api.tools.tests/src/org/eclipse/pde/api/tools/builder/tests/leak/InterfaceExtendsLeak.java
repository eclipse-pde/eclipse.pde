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
package org.eclipse.pde.api.tools.builder.tests.leak;

import junit.framework.Test;

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

	private int pid = -1;
	
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
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE, 
					IElementDescriptor.TYPE, 
					IApiProblem.API_LEAK, 
					IApiProblem.LEAK_EXTENDS);
		}
		return pid;
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
		x1(false);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "Etest1";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that an outer API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak2F() {
		x2(false);
	}
	
	/**
	 * Tests that an outer API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "Etest2";
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that an inner API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak3F() {
		x3(false);
	}
	
	/**
	 * Tests that an inner API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak3I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "Etest3";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that a static inner API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak4F() {
		x4(false);
	}
	
	/**
	 * Tests that a static inner API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak4I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "Etest4";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that an inner interface in an outer interface in an API interface that extends an internal interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak5F() {
		x5(false);
	}
	
	/**
	 * Tests that an inner interface in an outer interface in an API interface that extends an internal interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak5I() {
		x5(true);
	}
	
	private void x5(boolean inc) {
		expectingNoProblems();
		String typename = "Etest5";
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using a full build even with an @noimplement tag on it
	 */
	public void testInterfaceExtendsLeak6F() {
		x6(false);
	}
	
	/**
	 * Tests that an API interface that extends an internal interface is properly flagged
	 * using an incremental build even with an @noimplement tag on it
	 */
	public void testInterfaceExtendsLeak6I() {
		x6(true);
	}
	
	private void x6(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "Etest6";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that an N-nested internal interface in an API interface is properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak7F() {
		x7(false);
	}
	
	/**
	 * Tests that an N-nested internal interface in an API interface is properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak7I() {
		x7(true);
	}
	
	private void x7(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "Etest7";
		String innertype = "inner2";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that a variety of N-nested internal / outer interfaces in an API interface are properly flagged
	 * using a full build
	 */
	public void testInterfaceExtendsLeak8F() {
		x8(false);
	}
	
	/**
	 * Tests that a variety of N-nested internal / outer interfaces in an API interface are properly flagged
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak8I() {
		x8(true);
	}
	
	private void x8(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId(), getDefaultProblemId(), getDefaultProblemId()});
		String typename = "Etest8";
		String inner = "inner";
		String innertype3 = "inner3";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype3},
				{TESTING_INTERNAL_INTERFACE_NAME, inner}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests that having an @noimplement tag on interfaces does not affect problems being detected for extends leaks
	 * using a full build
	 */
	public void testInterfaceExtendsLeak9F() {
		x9(false);
	}
	
	/**
	 * Tests that having an @noimplement tag on interfaces does not affect problems being detected for extends leaks
	 * using an incremental build
	 */
	public void testInterfaceExtendsLeak9I() {
		x9(true);
	}
	
	private void x9(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId(), getDefaultProblemId(), getDefaultProblemId()});
		String typename = "Etest9";
		String innertype1 = "inner";
		String innertype2 = "inner2";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename},	
				{TESTING_INTERNAL_INTERFACE_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2}});
		deployLeakTest(typename+".java", inc);
	}
	
	/**
	 * Tests extending a non public top level interface is a leak of a non-API type.
	 */
	public void testInterfaceExtendsLeak10F() {
		x10(false);
	}
	
	/**
	 * Tests extending a non public top level interface is a leak of a non-API type.
	 */
	public void testInterfaceExtendsLeak10I() {
		x10(true);
	}
	
	private void x10(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "Etest10";
		setExpectedMessageArgs(new String[][] {{"Iouter", typename}});
		deployLeakTest(typename+".java", inc);
	}	
}
