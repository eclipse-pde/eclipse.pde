/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Test class usage for Java 7 code snippets
 *
 * @since 1.0.100
 */
public class Java7FieldUsageTests extends Java7UsageTest {

	private int pid = -1;

	public Java7FieldUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java7FieldUsageTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		if (pid == -1) {
			pid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.FIELD);
		}
		return pid;
	}

	/**
	 * Tests illegal use of classes inside a string switch block
	 * (full)
	 */
	public void testStringSwitchF() {
		x1(false);
	}

	/**
	 * Tests illegal use of classes inside a string switch block
	 * (incremental)
	 */
	public void testStringSwitchI() {
		x1(true);
	}


	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(3));
		String typename = "testFStringSwitch"; //$NON-NLS-1$
		// Note that since constants are inlined, we do not get markers for illegal use
		setExpectedMessageArgs(new String[][] {
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f1" }, //$NON-NLS-1$
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f1" }, //$NON-NLS-1$
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f1" } //$NON-NLS-1$

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of classes inside a multi catch block
	 * (full)
	 */
	public void testMultiCatchF() {
		x2(false);
	}

	/**
	 * Tests illegal use of classes inside a multi catch block
	 * (incremental)
	 */
	public void testMultiCatchI() {
		x2(true);
	}


	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(2));
		String typename = "testFMultiCatch"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{"MultipleThrowableClass", typename, "f1"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"MultipleThrowableClass", typename, "f1"} //$NON-NLS-1$ //$NON-NLS-2$
		});
		deployUsageTest(typename, inc);
	}
}
