package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;

public class PluginModelFactory implements IPluginModelFactory {
	private IPluginModelBase model;

public PluginModelFactory(IPluginModelBase model) {
	this.model = model;
}
public IPluginAttribute createAttribute(IPluginElement element) {
	PluginAttribute attribute = new PluginAttribute();
	attribute.setModel(model);
	attribute.setParent(element);
	return attribute;
}
public IPluginElement createElement(IPluginObject parent) {
	PluginElement element = new PluginElement();
	element.setModel(model);
	element.setParent(parent);
	return element;
}
public IPluginExtension createExtension() {
	PluginExtension extension = new PluginExtension();
	extension.setParent(model.getPluginBase());
	extension.setModel(model);
	return extension;
}
public IPluginExtensionPoint createExtensionPoint() {
	PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
	extensionPoint.setModel(model);
	extensionPoint.setParent(model.getPluginBase());
	return extensionPoint;
}
public IPluginImport createImport() {
	PluginImport iimport = new PluginImport();
	iimport.setModel(model);
	iimport.setParent(model.getPluginBase());
	return iimport;
}
public IPluginLibrary createLibrary() {
	PluginLibrary library = new PluginLibrary();
	library.setModel(model);
	library.setParent(model.getPluginBase());
	return library;
}
}
