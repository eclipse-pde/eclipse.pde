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
package org.eclipse.pde.internal.ui.editor.manifest;

import java.util.Hashtable;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.PluginElement;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.ui.views.properties.IPropertySource;

public class PluginAdapterFactory implements IAdapterFactory {
	private ExtensionPropertySource extensionProperties;
	private ExtensionPointPropertySource extensionPointProperties;
	private Hashtable elementProperties=new Hashtable();
	private ExtensionPropertySource readOnlyExtensionProperties;
	private ExtensionPointPropertySource readOnlyExtensionPointProperties;
	private Hashtable readOnlyElementProperties=new Hashtable();

protected ExtensionElementPropertySource createCustomPropertySource(
	String key,
	IPluginElement element) {
	ExtensionElementPropertySource source =
		new ExtensionElementPropertySource(element);
	Hashtable table = getElementTable(element);
	table.put(key, source);
	return source;
}
public Object getAdapter(Object adaptableObject, Class adapterType) {
	if (adapterType.equals(IPropertySource.class)) return getProperties(adaptableObject);
	return null;
}
public java.lang.Class[] getAdapterList() {
	return new Class[] { IPropertySource.class };
}
private Hashtable getElementTable(IPluginElement element) {
	return element.getModel().getUnderlyingResource() != null
		? elementProperties
		: readOnlyElementProperties;
}
private IPropertySource getExtensionElementProperties(IPluginElement element) {
	ISchemaElement elementInfo = ((PluginElement)element).getElementInfo();
	if (elementInfo == null) {
		// Use standard page for unknown elements
		return new UnknownElementPropertySource(element);
	} else {
		// Use or build custom property page for this class
		String key = elementInfo.getSchema().getQualifiedPointId() + "." + elementInfo.getName();
		Hashtable table = getElementTable(element);
		ExtensionElementPropertySource customSource = (ExtensionElementPropertySource) table.get(key);
		if (customSource == null) {
			customSource = createCustomPropertySource(key, element);
		}
		else customSource.setElement(element);
		return customSource;
	}
}
private IPropertySource getExtensionPointProperties(IPluginExtensionPoint point) {
	boolean readOnly = !point.getModel().isEditable();

	if (readOnly) {
		if (readOnlyExtensionPointProperties != null) {
			readOnlyExtensionPointProperties.setPoint(point);
		} else
			readOnlyExtensionPointProperties = new ExtensionPointPropertySource(point);
		return readOnlyExtensionPointProperties;
	} else {
		if (extensionPointProperties != null) {
			extensionPointProperties.setPoint(point);
		} else
			extensionPointProperties = new ExtensionPointPropertySource(point);
		return extensionPointProperties;
	}
}
private IPropertySource getExtensionProperties(IPluginExtension extension) {
	boolean readOnly = !extension.getModel().isEditable();

	if (readOnly) {
		if (readOnlyExtensionProperties != null)
			readOnlyExtensionProperties.setExtension(extension);
		else
			readOnlyExtensionProperties = new ExtensionPropertySource(extension);
		return readOnlyExtensionProperties;
	} else {
		if (extensionProperties != null)
			extensionProperties.setExtension(extension);
		else
			extensionProperties = new ExtensionPropertySource(extension);
		return extensionProperties;
	}
}
private IPropertySource getProperties(Object object) {
	if (object instanceof IPluginExtension)
		return getExtensionProperties((IPluginExtension)object);
	if (object instanceof IPluginExtensionPoint)
		return getExtensionPointProperties((IPluginExtensionPoint)object);
	if (object instanceof IPluginElement)
		return getExtensionElementProperties((IPluginElement)object);
	return null;
}
}
