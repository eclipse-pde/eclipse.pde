/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

/**
 * Tests valid javadoc tags on interface methods
 * 
 * @since 1.0
 */
public class ValidInterfaceMethodTagTests extends ValidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidInterfaceMethodTagTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidInterfaceMethodTagTests.class);
	}

	public void testValidInterfaceMethodTag1I() {
		x1(true);
	}

	public void testValidInterfaceMethodTag1F() {
		x1(false);
	}
	
	/**
	 * Tests the supported @noreference tag on interface methods
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testValidInterfaceMethodTag4I() {
		x4(true);
	}
	
	public void testValidInterfaceMethodTag4F() {
		x4(false);
	}

	/**
	 * Tests the supported @noreference tag on a variety of inner / outer interface methods
	 */
	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}
	
	public void testValidInterfaceMethodTag5I() {
		x5(true);
	}
	
	public void testValidInterfaceMethodTag5F() {
		x5(false);
	}
	
	/**
	 * Tests the supported @noreference tag on interface methods in the default package
	 */
	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, true); //$NON-NLS-1$
	}
}
