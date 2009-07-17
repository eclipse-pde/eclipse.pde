/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Tests that usage from fragment -&gt; host is not reported as a problem
 * 
 * @since 1.0.1
 */
public class FragmentUsageTests extends UsageTest {

	/**
	 * Constructor
	 * @param name
	 */
	public FragmentUsageTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * @return the test suite for this class
	 */
	public static Test suite() {
		return buildTestSuite(FragmentUsageTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("fragments");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestingProjectName()
	 */
	@Override
	protected String getTestingProjectName() {
		return "fragmenttests";
	}
	
	public void testClassExtendsI() {
		x1(true);
	}
	
	public void testClassExtendsF() {
		x1(false);
	}
	
	/**
	 * Tests that extending an @noextend class from the host bundle is not a problem
	 * @param inc
	 */
	private void x1(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test1", inc);
	}
	
	public void testImplementsI() {
		x2(true);
	}
	
	public void testImplementsF() {
		x2(false);
	}
	
	/**
	 * Tests that implementing an @noimplement interface from the host bundle is not a problem
	 * @param inc
	 */
	private void x2(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test2", inc);
	}
	
	public void testInstantiateI() {
		x3(true);
	}
	
	public void testInstantiateF() {
		x3(false);
	}
	
	/**
	 * Tests that instantiating an @noinstantiate class from the host bundle is not a problem
	 * @param inc
	 */
	private void x3(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test3", inc);
	}
	
	public void testConstNoRefI() {
		x4(true);
	}
	
	public void testConstNoRefF() {
		x4(false);
	}
	
	/**
	 * Tests that referencing a constructor marked as @noreference is not a problem from the host bundle
	 * @param inc
	 */
	private void x4(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test4", inc);
	}
	
	public void testFieldNoRefI() {
		x5(true);
	}
	
	public void testFieldNoRefF() {
		x5(false);
	}
	
	/**
	 * Tests that referencing an @noreference field from the host if not a problem
	 * @param inc
	 */
	private void x5(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test5", inc);
	}
	
	public void testOverrideI() {
		x6(true);
	}
	
	public void testOverrideF() {
		x6(false);
	}
	
	/**
	 * Tests the overriding an @nooverride method form the host is not a problem
	 * @param inc
	 */
	private void x6(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test6", inc);
	}
	
	public void testIExtendsI() {
		x7(true);
	}
	
	public void testIExtendsF() {
		x7(false);
	}
	
	/**
	 * Tests that extending an @noextend interface from the host is not a problem
	 * @param inc
	 */
	private void x7(boolean inc) {
		expectingNoProblems();
		deployUsageTest("test7", inc);
	}
}
