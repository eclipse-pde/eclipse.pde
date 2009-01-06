/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	public static final String FLAG_ACTIVATOR = "requiresActivator"; //$NON-NLS-1$

	public static Boolean getFlag(WizardElement element, String name) {
		IConfigurationElement config = element.getConfigurationElement();
		String value = config.getAttribute(name);
		if (value == null)
			return null;
		return new Boolean(value.equalsIgnoreCase("true")); //$NON-NLS-1$
	}

	public static boolean getFlag(WizardElement element, String name, boolean defaultValue) {
		IConfigurationElement config = element.getConfigurationElement();
		String value = config.getAttribute(name);
		if (value == null)
			return defaultValue;
		return value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}

	public static boolean isActive(WizardElement element) {
		IConfigurationElement config = element.getConfigurationElement();
		final String pluginId = config.getNamespaceIdentifier();
		final String contributionId = config.getAttribute("id"); //$NON-NLS-1$
		IPluginContribution contribution = new IPluginContribution() {
			public String getLocalId() {
				return contributionId;
			}

			public String getPluginId() {
				return pluginId;
			}
		};
		return !WorkbenchActivityHelper.filterItem(contribution);
	}

}
