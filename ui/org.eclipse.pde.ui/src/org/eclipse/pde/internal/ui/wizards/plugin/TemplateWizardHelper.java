/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

public class TemplateWizardHelper {

	public static final String FLAG_UI = "ui-content"; //$NON-NLS-1$
	public static final String FLAG_JAVA = "java"; //$NON-NLS-1$
	public static final String FLAG_RCP = "rcp"; //$NON-NLS-1$
	public static final String FLAG_OSGI = "pureOSGi"; //$NON-NLS-1$
	public static final String FLAG_BND = "bnd"; //$NON-NLS-1$
	public static final String FLAG_ACTIVATOR = "requiresActivator"; //$NON-NLS-1$

	public static boolean isActive(WizardElement element) {
		IConfigurationElement config = element.getConfigurationElement();
		final String pluginId = config.getNamespaceIdentifier();
		final String contributionId = config.getAttribute("id"); //$NON-NLS-1$
		IPluginContribution contribution = new IPluginContribution() {
			@Override
			public String getLocalId() {
				return contributionId;
			}

			@Override
			public String getPluginId() {
				return pluginId;
			}
		};
		return !WorkbenchActivityHelper.filterItem(contribution);
	}

}
