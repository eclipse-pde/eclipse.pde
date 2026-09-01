/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import org.eclipse.core.runtime.IPath;

/**
 * Tests that the builder correctly finds and reports method compatibility
 * problems
 *
 * @since 1.0
 */
public class MethodCompatibilityTests extends CompatibilityTest {

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("methods"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	@Override
	protected String getTestingProjectName() {
		return "methodcompat"; //$NON-NLS-1$
	}
}
