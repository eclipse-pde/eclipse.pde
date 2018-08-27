/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.annotations;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests invalid annotation in Java 8 interfaces
 */
public class InvalidJava8InterfaceAnnotationTests extends InvalidInterfaceAnnotationTests {

	public InvalidJava8InterfaceAnnotationTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("annotations").append("java8").append("interface"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected String getTestingProjectName() {
		return "java8tags"; //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidJava8InterfaceAnnotationTests.class);
	}

	public void testInvalidTagOnNonDefaultInterfaceMethodI() {
		x1(true);
	}

	public void testInvalidTagOnNonDefaultInterfaceMethodF() {
		x1(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on non-default interface methods in Java 8 interfaces
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{
						"@NoOverride", BuilderMessages.TagValidator_nondefault_interface_method }, //$NON-NLS-1$
				{
						"@NoOverride", BuilderMessages.TagValidator_nondefault_interface_method } //$NON-NLS-1$
		});
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidTagsOnFunctionalInterfaceI() {
		x2(true);
	}

	public void testInvalidTagsOnFunctionalInterfaceF() {
		x2(false);
	}

	/**
	 * Tests that a variety of tags are unsupported on a functional interface
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{
						"@NoOverride", BuilderMessages.TagValidator_nondefault_interface_method }, //$NON-NLS-1$
				{
						"@NoOverride", BuilderMessages.TagValidator_nondefault_interface_method } //$NON-NLS-1$
		});
		deployAnnotationTest("test2.java", inc, false); //$NON-NLS-1$
	}
}
