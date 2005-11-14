/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.Vector;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class PluginPropertySource extends RegistryPropertySource {
	private IPluginDescriptor pd;
	public static final String P_INSTALL_URL="installURL"; //$NON-NLS-1$
	public static final String P_NAME = "name"; //$NON-NLS-1$
	public static final String P_ID = "id"; //$NON-NLS-1$
	public static final String P_PROVIDER = "provider"; //$NON-NLS-1$
	public static final String P_VERSION = "version"; //$NON-NLS-1$
	public static final String P_ACTIVATED = "activated"; //$NON-NLS-1$
	public PluginPropertySource(IPluginDescriptor pd) {
	this.pd = pd;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_INSTALL_URL, PDERuntimeMessages.RegistryView_pluginPR_installURL));
	result.addElement(new PropertyDescriptor(P_NAME, PDERuntimeMessages.RegistryView_pluginPR_name));
	result.addElement(new PropertyDescriptor(P_ID, PDERuntimeMessages.RegistryView_pluginPR_id));
	result.addElement(new PropertyDescriptor(P_PROVIDER, PDERuntimeMessages.RegistryView_pluginPR_providerName));
	result.addElement(new PropertyDescriptor(P_VERSION, PDERuntimeMessages.RegistryView_pluginPR_version));
	result.addElement(new PropertyDescriptor(P_ACTIVATED, PDERuntimeMessages.RegistryView_pluginPR_activated));
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
