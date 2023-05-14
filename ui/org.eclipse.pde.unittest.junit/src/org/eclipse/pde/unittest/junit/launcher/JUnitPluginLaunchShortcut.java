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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.pde.ui.launcher.JUnitWorkbenchLaunchShortcut;
import org.eclipse.pde.unittest.junit.JUnitPluginTestPlugin;
import org.eclipse.unittest.ui.ConfigureViewerSupport;

/**
 * The launch shortcut to launch JUnit Plug-in tests.
 *
 * <p>
 * This class may be instantiated and subclassed.
 * </p>
 */
public class JUnitPluginLaunchShortcut extends JUnitWorkbenchLaunchShortcut {

	@Override
	protected String getLaunchConfigurationTypeId() {
		return JUnitPluginTestPlugin.PLUGIN_ID + ".launchConfiguration"; //$NON-NLS-1$
	}

	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);
		new ConfigureViewerSupport(JUnitPluginTestPlugin.UNIT_TEST_VIEW_SUPPORT_ID).apply(wc);
		return wc;
	}
}
