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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;

/**
 * Tests using restricted enums
 *
 * @since 1.0.400
 */
public class EnumUsageTests extends UsageTest {

	public EnumUsageTests(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	public static Test suite() {
		return buildTestSuite(EnumUsageTests.class);
	}
}
