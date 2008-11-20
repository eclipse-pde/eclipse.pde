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

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests a variety of class usages where the callee has API restrictions
 * 
 * @since 1.0
 */
public class ClassUsageTests extends UsageTest {

	protected static final String CLASS_NAME = "ClassUsageClass";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassUsageTests(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	/**
	 * Returns the problem id with the given kind
	 * @param kind
	 * @return the problem id
	 */
	private int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_USAGE, 
				IElementDescriptor.TYPE, 
				kind, 
				flags);
	}
	
	public static Test suite() {
		return buildTestSuite(ClassUsageTests.class);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * Tests that classes the extend a restricted class are properly flagged 
	 * using a full build
	 */
	public void testClassUsageTests1F() {
		x1(false);
	}
	
	/**
	 * Tests the classes the extend a restricted class are properly flagged
	 * using an incremental build
	 */
	public void testClassUsageTests1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS)
		});
		String typename = "testC1";
		setExpectedMessageArgs(new String[][] {
				{CLASS_NAME, typename},
				{CLASS_NAME, INNER_NAME1},
				{CLASS_NAME, INNER_NAME2},
				{CLASS_NAME, OUTER_NAME}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that classes the instantiate a restricted class are properly flagged 
	 * using a full build
	 */
	public void testClassUsageTests2F() {
		x2(false);
	}
	
	/**
	 * Tests the classes the instantiate a restricted class are properly flagged
	 * using an incremental build
	 */
	public void testClassUsageTests2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		String typename = "testC2";
		setExpectedMessageArgs(new String[][] {
				{CLASS_NAME, typename},
				{CLASS_NAME, INNER_NAME1},
				{CLASS_NAME, INNER_NAME2},
				{CLASS_NAME, OUTER_NAME}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that indirect illegal implementing is properly 
	 * detected for one class and an extension interface using a full build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements1F() {
		x3(false);
	}
	
	/**
	 * Tests that indirect illegal implementing is properly 
	 * detected for one class and an extension interface using an incremental build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements1I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_REFERENCE)
		});
		String typename = "testC3";
		setExpectedMessageArgs(new String[][] {
				{"IExtInterface1", "INoImpl1", typename},
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that an indirect illegal implement is ignored when there is a
	 * parent class that implements the @noimplement interface using 
	 * a full build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements2F() {
		x4(false);
	}
	
	/**
	 * Tests that an indirect illegal implement is ignored when there is a
	 * parent class that implements the @noimplement interface using 
	 * an incremental build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements2I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		expectingNoProblems();
		deployTest("testC4", inc);
	}
	
	/**
	 * Tests that multiple indirect illegal implements are detected when there is no
	 * parent class that implements the @noimplement interfaces using 
	 * a full build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements3F() {
		x5(false);
	}
	
	/**
	 * Tests that multiple indirect illegal implements are detected when there is no
	 * parent class that implements the @noimplement interfaces using 
	 * an incremental build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements3I() {
		x5(true);
	}
	
	private void x5(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_REFERENCE),
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_REFERENCE),
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_REFERENCE),
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_REFERENCE)
		});
		String typename = "testC5";
		setExpectedMessageArgs(new String[][] {
				{"IExtInterface1", "INoImpl1", typename},
				{"IExtInterface2", "INoImpl1", typename},
				{"IExtInterface3", "INoImpl1", typename},
				{"IExtInterface4", "INoImpl4", typename}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that multiple indirect illegal implements are detected when there is a
	 * parent class that implements the @noimplement interfaces using 
	 * a full build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements4F() {
		x6(false);
	}
	
	/**
	 * Tests that multiple indirect illegal implements are detected when there is a
	 * parent class that implements the @noimplement interfaces using 
	 * an incremental build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements4I() {
		x6(true);
	}
	
	private void x6(boolean inc) {
		expectingNoProblems();
		deployTest("testC6", inc);
	}
	
	/**
	 * Tests that an indirect illegal implements is detected when there is a
	 * parent class N levels indirected that implements the @noimplement interfaces using 
	 * a full build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements5F() {
		x7(false);
	}
	
	/**
	 * Tests that an indirect illegal implements is detected when there is a
	 * parent class N levels indirected that implements the @noimplement interfaces using 
	 * an incremental build
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230189
	 */
	public void testClassIndirectImplements5I() {
		x7(true);
	}
	
	private void x7(boolean inc) {
		expectingNoProblems();
		deployTest("testC7", inc);
	}
	
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=255804
	 */
	public void testClassIndirectImplements6F() {
		x8(false);
	}
	
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=255804
	 */
	public void testClassIndirectImplements6I() {
		x8(true);
	}
	
	private void x8(boolean inc) {
		expectingNoProblems();
		deployTest("testC8", inc);
	}
	
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=255804
	 */
	public void testClassIndirectImplements7F() {
		x9(false);
	}
	
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=255804
	 */
	public void testClassIndirectImplements7I() {
		x9(true);
	}
	
	private void x9(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS)
		});
		String typename = "testC9";
		setExpectedMessageArgs(new String[][] {
				{"INoImpl1", typename}
		});
		deployTest(typename, inc);
	}
}
