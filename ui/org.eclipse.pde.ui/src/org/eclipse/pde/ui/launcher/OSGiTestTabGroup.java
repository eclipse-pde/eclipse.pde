/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;


import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.PrototypeTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaDependenciesTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * @since 3.16
 */
public class OSGiTestTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfiguration configuration = DebugUITools.getLaunchConfiguration(dialog);
		boolean isModularConfiguration = configuration != null && JavaRuntime.isModularConfiguration(configuration);
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new JUnitLaunchConfigurationTab(), //
				new JavaArgumentsTab(), //
				new JavaJRETab(true), //
				isModularConfiguration ? new JavaDependenciesTab() : new JavaClasspathTab(), //
				new SourceLookupTab(), //
				new EnvironmentTab(), //
				new CommonTab(), //
				new PrototypeTab()
		};
		setTabs(tabs);
	}

	@SuppressWarnings("restriction")
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		JUnitTabGroup.enableAssertions(configuration);
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND,
				TestKindRegistry.JUNIT5_TEST_KIND_ID);
	}

}
