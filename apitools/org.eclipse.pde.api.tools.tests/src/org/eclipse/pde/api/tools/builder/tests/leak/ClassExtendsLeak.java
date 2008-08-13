/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.leak;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that leaked members via extends for classes is properly detected
 * 
 * @since 1.0
 */
public class ClassExtendsLeak extends LeakTest {

	/**
	 * Constructor
	 * @param name
	 */
	public ClassExtendsLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.API_LEAK, IApiProblem.ILLEGAL_EXTEND);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.InvalidFieldTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassExtendsLeak.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}

	/**
	 * Tests that an API class that extends an internal type is properly flagged
	 * as leaking using a full build
	 */
	public void testClassExtendsLeak1F() {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		deployFullBuildLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {"test1", TESTING_INTERNAL_SOURCE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+".test1"}, 
				true);
	}
	
	public void testClassExtendsLeak1I() {
		
	}
	
}
