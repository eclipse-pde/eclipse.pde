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

import org.eclipse.core.runtime.*;
import org.eclipse.ui.views.properties.IPropertySource;

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
