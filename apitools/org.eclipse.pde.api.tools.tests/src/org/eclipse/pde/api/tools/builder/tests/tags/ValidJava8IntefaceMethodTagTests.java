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

/**
 * Tests valid tags on Java 8 interface methods
 */
public class ValidJava8IntefaceMethodTagTests extends
		ValidInterfaceMethodTagTests {

	public ValidJava8IntefaceMethodTagTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("java8").append("tags").append("interface").append("valid");
	}
	
	@Override
	protected String getTestingProjectName() {
		return "java8tags";
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidJava8IntefaceMethodTagTests.class);
	}

	public void testNoOverrideOnDefaultI() {
		x1(true);
	}

	public void testNoOverrideOnDefaultF() {
		x1(false);
	}
	
	/**
	 * Tests the @nooverride tag on a default method
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false);
	}
}
