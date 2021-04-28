/*******************************************************************************
 * Copyright (c) 2021 Kichwa Coders Canada Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.ui.tests.target.LocalTargetDefinitionTests;
import org.junit.Test;

public class LaunchConfigurationSetupTests {

	/**
	 * Tests that {@link ICoreConstants#ADD_SWT_NON_DISPOSAL_REPORTING} has an
	 * effect. See {@link LocalTargetDefinitionTests#testArguments()} for tests
	 * on the interaction of the preference with the target platform.
	 */
	@Test
	public void testAddSwtNonDisposalReporting() throws Exception {
		PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
		try {
			prefs.setValue(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING, true);
			assertThat(LaunchArgumentsHelper.getInitialVMArguments())
					.contains("-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true");
			prefs.setValue(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING, false);
			assertThat(LaunchArgumentsHelper.getInitialVMArguments())
					.doesNotContain("-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=");
		} finally {
			prefs.setToDefault(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING);
		}

	}

}
