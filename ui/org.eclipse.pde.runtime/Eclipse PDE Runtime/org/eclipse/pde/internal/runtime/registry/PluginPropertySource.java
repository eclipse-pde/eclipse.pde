package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.*;

public class PluginPropertySource extends RegistryPropertySource {
	private IPluginDescriptor pd;
	public static final String P_INSTALL_URL="installURL";
	public static final String P_NAME = "name";
	public static final String P_ID = "id";
	public static final String P_PROVIDER = "provider";
	public static final String P_VERSION = "version";
	public static final String P_ACTIVATED = "activated";
	public static final String KEY_ACTIVATED = "RegistryView.pluginPR.activated";
	public static final String KEY_INSTALL_URL = "RegistryView.pluginPR.installURL";
	public static final String KEY_NAME = "RegistryView.pluginPR.name";
	public static final String KEY_ID = "RegistryView.pluginPR.id";
	public static final String KEY_PROVIDER_NAME = "RegistryView.pluginPR.providerName";
	public static final String KEY_VERSION = "RegistryView.pluginPR.version";

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
		return pd.isPluginActivated() ? "true" : "false";
	return null;
}
}
