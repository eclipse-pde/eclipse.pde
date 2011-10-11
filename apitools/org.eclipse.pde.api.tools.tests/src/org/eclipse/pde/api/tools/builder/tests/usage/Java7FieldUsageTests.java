/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
 * Test class usage for Java 7 code snippets
 * 
 * @since 1.0.100
 */
public class Java7FieldUsageTests extends FieldUsageTests {

	/**
	 * Constructor
	 * @param name
	 */
	public Java7FieldUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java7FieldUsageTests.class);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_7;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().removeLastSegments(1).append("java7");
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
		String typename = "testFStringSwitch";
		// Note that since constants are inlined, we do not get markers for illegal use
		setExpectedMessageArgs(new String[][] {
				{FIELD_CLASS_NAME, typename, "f1"},
				{FIELD_CLASS_NAME, typename, "f1"},
				{FIELD_CLASS_NAME, typename, "f1"}
			
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
		String typename = "testFMultiCatch";
		setExpectedMessageArgs(new String[][] {
				{"MultipleThrowableClass", typename, "f1"},
				{"MultipleThrowableClass", typename, "f1"}
		});
		deployUsageTest(typename, inc);
	}
	
}
