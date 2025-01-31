/*******************************************************************************
 * Copyright (c) 2025 ArSysOp and others.
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

import junit.framework.Test;

/**
 * Tests that the builder accepts valid tags on records
 */
public class ValidRecordTagTests extends InvalidRecordTagTests {

	public ValidRecordTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidRecordTagTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	public void testValidRecordTag1I() {
		x1(true);
	}

	@Override
	public void testInvalidRecordTag1F() {
		x1(false);
	}

	/**
	 * Tests having an @noreference tag on a record in the default package
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, true); //$NON-NLS-1$
	}
}
