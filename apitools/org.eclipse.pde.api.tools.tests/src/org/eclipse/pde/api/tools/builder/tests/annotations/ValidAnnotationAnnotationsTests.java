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
package org.eclipse.pde.api.tools.builder.tests.annotations;

import org.eclipse.core.runtime.IPath;

import junit.framework.Test;

/**
 * Tests tags that are valid on an annotation
 *
 * @since 1.0.400
 */
public class ValidAnnotationAnnotationsTests extends InvalidAnnotationAnnotationsTests {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public ValidAnnotationAnnotationsTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidAnnotationAnnotationsTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	/**
	 * Tests the @NoReference annotation on annotations
	 *
	 * @throws Exception
	 */
	public void testNoReference1I() throws Exception {
		String typename = "test1.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, false);
	}

	/**
	 * Tests the @NoReference annotation on annotations
	 *
	 * @throws Exception
	 */
	public void testNoReference1F() throws Exception {
		String typename = "test1.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, false);
	}

	/**
	 * Tests the @NoReference annotation on annotations in the default package
	 *
	 * @throws Exception
	 */
	public void testNoReference2I() throws Exception {
		String typename = "test2.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, true);
	}

	/**
	 * Tests the @NoReference annotation on annotations in the default package
	 *
	 * @throws Exception
	 */
	public void testNoReference2F() throws Exception {
		String typename = "test2.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, true);
	}

	/**
	 * Tests the @NoReference annotation on annotations and member types
	 *
	 * @throws Exception
	 */
	public void testNoReference3I() throws Exception {
		String typename = "test3.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, true);
	}

	/**
	 * Tests the @NoReference annotation on annotations and member types
	 *
	 * @throws Exception
	 */
	public void testNoReference3F() throws Exception {
		String typename = "test3.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, true);
	}
}
