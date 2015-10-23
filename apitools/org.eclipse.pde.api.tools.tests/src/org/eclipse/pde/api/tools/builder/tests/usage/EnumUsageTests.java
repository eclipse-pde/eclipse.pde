/*******************************************************************************
 * Copyright (c) Aug 22, 2013 IBM Corporation and others.
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
import org.eclipse.jdt.core.JavaCore;

/**
 * Tests using restricted enums
 *
 * @since 1.0.400
 */
public class EnumUsageTests extends UsageTest {

	/**
	 * @param name
	 */
	public EnumUsageTests(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId
	 * ()
	 */
	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath
	 * ()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance
	 * ()
	 */
	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	public static Test suite() {
		return buildTestSuite(EnumUsageTests.class);
	}
}
