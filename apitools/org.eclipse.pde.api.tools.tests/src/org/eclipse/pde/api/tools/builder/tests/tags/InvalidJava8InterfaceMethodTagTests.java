/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests invalid tags in Java 8 interface methods
 */
public class InvalidJava8InterfaceMethodTagTests extends InvalidInterfaceMethodTagTests {

	public InvalidJava8InterfaceMethodTagTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("java8").append("tags").append("interface");
	}
	
	@Override
	protected String getTestingProjectName() {
		return "java8tags";
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidJava8InterfaceMethodTagTests.class);
	}
	
	public void testInvalidJava8InterfaceMethodTag1I() {
		x1(true);
	}
	
	public void testInvalidJava8InterfaceMethodTag1F() {
		x1(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on non-default interface methods in Java 8 interfaces
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method},
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}
		});
		deployTagTest("test1.java", inc, false);
	}
}
