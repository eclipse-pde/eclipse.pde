/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.Vector;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.ui.views.properties.*;

public class PluginPropertySource extends RegistryPropertySource {
	private IPluginDescriptor pd;
	public static final String P_INSTALL_URL="installURL"; //$NON-NLS-1$
	public static final String P_NAME = "name"; //$NON-NLS-1$
	public static final String P_ID = "id"; //$NON-NLS-1$
	public static final String P_PROVIDER = "provider"; //$NON-NLS-1$
	public static final String P_VERSION = "version"; //$NON-NLS-1$
	public static final String P_ACTIVATED = "activated"; //$NON-NLS-1$
	public static final String KEY_ACTIVATED = "RegistryView.pluginPR.activated"; //$NON-NLS-1$
	public static final String KEY_INSTALL_URL = "RegistryView.pluginPR.installURL"; //$NON-NLS-1$
	public static final String KEY_NAME = "RegistryView.pluginPR.name"; //$NON-NLS-1$
	public static final String KEY_ID = "RegistryView.pluginPR.id"; //$NON-NLS-1$
	public static final String KEY_PROVIDER_NAME = "RegistryView.pluginPR.providerName"; //$NON-NLS-1$
	public static final String KEY_VERSION = "RegistryView.pluginPR.version"; //$NON-NLS-1$

public PluginPropertySource(IPluginDescriptor pd) {
	this.pd = pd;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_INSTALL_URL, PDERuntimePlugin.getResourceString(KEY_INSTALL_URL)));
	result.addElement(new PropertyDescriptor(P_NAME, PDERuntimePlugin.getResourceString(KEY_NAME)));
	result.addElement(new PropertyDescriptor(P_ID, PDERuntimePlugin.getResourceString(KEY_ID)));
	result.addElement(new PropertyDescriptor(P_PROVIDER, PDERuntimePlugin.getResourceString(KEY_PROVIDER_NAME)));
	result.addElement(new PropertyDescriptor(P_VERSION, PDERuntimePlugin.getResourceString(KEY_VERSION)));
	result.addElement(new PropertyDescriptor(P_ACTIVATED, PDERuntimePlugin.getResourceString(KEY_ACTIVATED)));
	return toDescriptorArray(result);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_INSTALL_URL))
		return pd.getInstallURL();
	if (name.equals(P_NAME))
		return pd.getLabel();
	if (name.equals(P_ID))
		return pd.getUniqueIdentifier();
	if (name.equals(P_PROVIDER))
		return pd.getProviderName();
	if (name.equals(P_VERSION))
		return pd.getVersionIdentifier();
	if (name.equals(P_ACTIVATED))
		return pd.isPluginActivated() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
	return null;
}
}
