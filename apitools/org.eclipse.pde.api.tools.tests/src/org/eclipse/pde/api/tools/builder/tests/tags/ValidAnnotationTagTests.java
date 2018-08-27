/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.tags;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;

/**
 * Tests that the builder accepts valid tags on annotations
 */
public class ValidAnnotationTagTests extends InvalidAnnotationTagTests {

	public ValidAnnotationTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidAnnotationTagTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}


	public void testInvalidAnnotationTag1I() {
		x1(true);
	}

	public void testInvalidAnnotationTag1F() {
		x1(false);
	}

	/**
	 * Tests having an @noreference tag on a variety of annotations in package a.b.c
	 */
	private void x1(boolean inc) {
		String typename = "test1.java";  //$NON-NLS-1$
		deployTagTest(typename, inc, false);
	}


	public void testInvalidAnnotationTag2I() {
		x2(true);
	}

	public void testInvalidAnnotationTag2F() {
		x2(false);
	}

	/**
	 * Tests having an @noreference tag on an annotation in the default package
	 */
	private void x2(boolean inc) {
		String typename = "test2.java";  //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	@Override
	public void testInvalidAnnotationTag3I() {
		x3(true);
	}

	@Override
	public void testInvalidAnnotationTag3F() {
		x3(false);
	}

	/**
	 * Tests having a bunch tags on member annotation elements
	 */
	private void x3(boolean inc) {
		String typename = "test2.java";  //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

}
