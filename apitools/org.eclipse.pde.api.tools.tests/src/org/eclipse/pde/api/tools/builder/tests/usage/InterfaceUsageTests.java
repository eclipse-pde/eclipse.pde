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
package org.eclipse.pde.api.tools.builder.tests.usage;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests a variety of interface uses, where the callee has API restrictions
 * 
 * @since 1.0
 */
public class InterfaceUsageTests extends UsageTest {

	protected static final String INTERFACE_NAME = "InterfaceUsageInterface";
	protected static final String INNER_I_NAME = "Iinner";
	private int pid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public InterfaceUsageTests(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE, 
					IElementDescriptor.T_REFERENCE_TYPE, 
					IApiProblem.ILLEGAL_IMPLEMENT, 
					IApiProblem.NO_FLAGS);
		}
		return pid;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface");
	}
	
	public static Test suite() {
		return buildTestSuite(InterfaceUsageTests.class);
	}

	/**
	 * Tests that extending an @noimplement interface properly reports the usage problems
	 * using a full build 
	 */
	public void testInterfaceUsageTests1F() {
		x1(false);
	}
	
	/**
	 * Tests that extending an @noimplement interface properly reports the usage problems
	 * using an incremental build 
	 */
	public void testInterfaceUsageTests1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(5));
		String typename = "testI1";
		setExpectedMessageArgs(new String[][] {
				{INNER_I_NAME, INNER_NAME1},
				{INNER_I_NAME, INNER_NAME1},
				{INTERFACE_NAME, typename},
				{INNER_I_NAME, OUTER_INAME},
				{INNER_I_NAME, OUTER_INAME}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that implementing an @noimplement interface properly reports the usage problems
	 * using a full build 
	 */
	public void testInterfaceUsageTests2F() {
		x2(false);
	}
	
	/**
	 * Tests that implementing an @noimplement interface properly reports the usage problems
	 * using an incremental build 
	 */
	public void testInterfaceUsageTests2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testI2";
		setExpectedMessageArgs(new String[][] {
				{INNER_I_NAME, INNER_NAME1},
				{INNER_I_NAME, INNER_NAME1},
				{INTERFACE_NAME, OUTER_NAME},
				{INTERFACE_NAME, typename}
		});
		deployTest(typename, inc);
	}
}
