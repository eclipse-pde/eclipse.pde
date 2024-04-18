/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.unittest.junit.launcher;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.ui.launcher.TestTab;
import org.eclipse.pde.unittest.junit.JUnitPluginTestPlugin;
import org.eclipse.unittest.ui.ConfigureViewerSupport;

public class JUnitPluginTestTab extends TestTab {

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		super.performApply(config);

		new ConfigureViewerSupport(JUnitPluginTestPlugin.UNIT_TEST_VIEW_SUPPORT_ID).apply(config);

	}
}
