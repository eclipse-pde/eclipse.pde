/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 * Tests that the builder accepts valid tags on enums
 */
public class ValidEnumTagTests extends InvalidEnumTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidEnumTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidEnumTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}
	
	public void testValidEnumTag1I() {
		x1(true);
	}
	
	@Override
	public void testInvalidEnumTag1F() {
		x1(false);
	}
	
	/**
	 * Tests having an @noreference tag on an enum in the default package
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, true); //$NON-NLS-1$
	}
}
