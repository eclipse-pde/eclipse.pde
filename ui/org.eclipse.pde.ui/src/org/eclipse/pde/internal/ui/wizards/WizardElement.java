/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IPluginContribution;
import org.osgi.framework.Bundle;

/**
 * Handle to a configuration element representing a wizard class.
 */
public class WizardElement extends NamedElement implements IPluginContribution {

	public static final String ATT_NAME = "name"; //$NON-NLS-1$
	public static final String TAG_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String ATT_ICON = "icon"; //$NON-NLS-1$
	public static final String ATT_ID = "id"; //$NON-NLS-1$
	public static final String ATT_CLASS = "class"; //$NON-NLS-1$
	public static final String ATT_TEMPLATE = "template"; //$NON-NLS-1$
	public static final String ATT_POINT = "point"; //$NON-NLS-1$

	private String description;
	protected IConfigurationElement configurationElement;
	private IConfigurationElement template;

	WizardElement(IConfigurationElement config) {
		super(config.getAttribute(ATT_NAME));
		this.configurationElement = config;
	}

	public Object createExecutableExtension() throws CoreException {
		return configurationElement.createExecutableExtension(ATT_CLASS);
	}

	public IConfigurationElement getConfigurationElement() {
		return configurationElement;
	}

	public String getDescription() {
		if (description == null) {
			IConfigurationElement[] children = configurationElement.getChildren(TAG_DESCRIPTION);
			if (children.length > 0) {
				description = expandDescription(children[0].getValue());
			}
		}
		return description;
	}

	public Boolean getFlag(String name) {
		if (configurationElement != null) {
			String value = configurationElement.getAttribute(name);
			if (value != null) {
				return Boolean.valueOf(value.equalsIgnoreCase("true")); //$NON-NLS-1$
			}
		}
		return null;
	}

	public boolean getFlag(String name, boolean defaultValue) {
		if (configurationElement != null) {
			String value = configurationElement.getAttribute(name);
			if (value != null) {
				return value.equalsIgnoreCase("true"); //$NON-NLS-1$
			}
		}
		return defaultValue;
	}

	/**
	 * We allow replacement variables in description values as well. This is to
	 * allow extension template description reuse in project template wizards.
	 * Tokens in form '%token%' will be evaluated against the contributing
	 * plug-in's resource bundle. As before, to have '%' in the description, one
	 * need to add '%%'.
	 */
	private String expandDescription(String source) {
		if (source == null || source.length() == 0)
			return source;
		if (source.indexOf('%') == -1)
			return source;

		Bundle bundle = Platform.getBundle(configurationElement.getNamespaceIdentifier());
		if (bundle == null)
			return source;

		ResourceBundle resourceBundle = Platform.getResourceBundle(bundle);
		if (resourceBundle == null)
			return source;
		StringBuilder buf = new StringBuilder();
		boolean keyMode = false;
		int keyStartIndex = -1;
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '%') {
				char c2 = source.charAt(i + 1);
				if (c2 == '%') {
					i++;
					buf.append('%');
					continue;
				}
				if (keyMode) {
					keyMode = false;
					String key = source.substring(keyStartIndex, i);
					String value = key;
					try {
						value = resourceBundle.getString(key);
					} catch (MissingResourceException e) {
					}
					buf.append(value);
				} else {
					keyStartIndex = i + 1;
					keyMode = true;
				}
			} else if (!keyMode) {
				buf.append(c);
			}
		}
		return buf.toString();
	}

	public String getID() {
		return configurationElement.getAttribute(ATT_ID);
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public String getTemplateId() {
		return configurationElement.getAttribute(ATT_TEMPLATE);
	}

	public boolean isTemplate() {
		return getTemplateId() != null;
	}

	public IConfigurationElement getTemplateElement() {
		if (template == null)
			template = findTemplateElement();
		return template;
	}

	private IConfigurationElement findTemplateElement() {
		String templateId = getTemplateId();
		if (templateId == null)
			return null;
		IConfigurationElement[] templates = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.templates"); //$NON-NLS-1$
		for (IConfigurationElement template : templates) {
			String id = template.getAttribute("id"); //$NON-NLS-1$
			if (id != null && id.equals(templateId))
				return template;
		}
		return null;
	}

	public String getContributingId() {
		IConfigurationElement tel = getTemplateElement();
		return (tel == null) ? null : tel.getAttribute("contributingId"); //$NON-NLS-1$
	}

	@Override
	public String getLocalId() {
		return getID();
	}

	@Override
	public String getPluginId() {
		return null;
	}

	public static WizardElement create(IConfigurationElement config, String... requiredAttributes) {
		for (String required : requiredAttributes) {
			if (config.getAttribute(required) == null) {
				return null;
			}
		}
		WizardElement element = new WizardElement(config);
		String imageName = config.getAttribute(ATT_ICON);
		if (imageName != null) {
			String pluginID = config.getNamespaceIdentifier();
			Image image = PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(pluginID, imageName);
			element.setImage(image);
		}
		return element;
	}

}
