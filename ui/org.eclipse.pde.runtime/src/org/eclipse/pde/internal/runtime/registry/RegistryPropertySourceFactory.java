package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.ui.views.properties.*;

public class RegistryPropertySourceFactory implements IAdapterFactory {

public Object getAdapter(Object adaptableObject, Class adapterType) {
	if (adapterType.equals(IPropertySource.class)) return getPropertySource(adaptableObject);
	return null;
}
public java.lang.Class[] getAdapterList() {
	return new Class[] { IPropertySource.class };
}
protected IPropertySource getPropertySource(Object sourceObject) {
	if (sourceObject instanceof PluginObjectAdapter)
		sourceObject = ((PluginObjectAdapter) sourceObject).getObject();
	if (sourceObject instanceof IPluginDescriptor) {
		return new PluginPropertySource((IPluginDescriptor) sourceObject);
	}
	if (sourceObject instanceof IExtension) {
		return new ExtensionPropertySource((IExtension) sourceObject);
	}
	if (sourceObject instanceof IExtensionPoint) {
		return new ExtensionPointPropertySource((IExtensionPoint) sourceObject);
	}
	if (sourceObject instanceof ILibrary) {
		return new LibraryPropertySource((ILibrary) sourceObject);
	}
	if (sourceObject instanceof IConfigurationElement) {
		return new ConfigurationElementPropertySource((IConfigurationElement) sourceObject);
	}
	if (sourceObject instanceof IPluginPrerequisite) {
		return new PrerequisitePropertySource((IPluginPrerequisite) sourceObject);
	}
	return null;
}
}
