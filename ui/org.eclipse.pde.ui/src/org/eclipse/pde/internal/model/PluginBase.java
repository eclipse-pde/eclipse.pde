package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IResource;
import java.io.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.PlatformObject;

public abstract class PluginBase extends IdentifiablePluginObject implements IPluginBase {
	private Vector extensions=new Vector();
	private Vector extensionPoints=new Vector();
	private Vector libraries=new Vector();
	private String providerName;
	private String version;

public PluginBase() {
}
public void add(IPluginExtension extension) throws CoreException {
	ensureModelEditable();
	extensions.addElement(extension);
	fireStructureChanged(extension, IModelChangedEvent.INSERT);
}
public void add(IPluginExtensionPoint extensionPoint) throws CoreException {
	ensureModelEditable();
	extensionPoints.addElement(extensionPoint);
	fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
}
public void add(IPluginLibrary library) throws CoreException {
	ensureModelEditable();
	libraries.addElement(library);
	fireStructureChanged(library, IModelChangedEvent.INSERT);
}
public IPluginExtensionPoint[] getExtensionPoints() {
	IPluginExtensionPoint [] result = new IPluginExtensionPoint[extensionPoints.size()];
	extensionPoints.copyInto(result);
	return result;
}
public IPluginExtension[] getExtensions() {
	IPluginExtension [] result = new IPluginExtension[extensions.size()];
	extensions.copyInto(result);
	return result;
}
public IPluginLibrary[] getLibraries() {
	IPluginLibrary [] result = new IPluginLibrary[libraries.size()];
	libraries.copyInto(result);
	return result;
}
public IPluginBase getPluginBase() {
	return this;
}
public String getProviderName() {
	return providerName;
}
public String getVersion() {
	return version;
}
void load(PluginModel pd) {
	this.id = pd.getId();
	this.name = pd.getName();
	this.providerName = pd.getProviderName();
	this.version = pd.getVersion();

	// add libraries
	loadRuntime(pd.getRuntime());
	// add extensions
	loadExtensions(pd.getDeclaredExtensions());
	// add extension points
	loadExtensionPoints(pd.getDeclaredExtensionPoints());
}
void load(Node node) {
	this.id = getNodeAttribute(node, "id");
	this.name = getNodeAttribute(node, "name");
	this.providerName = getNodeAttribute(node, "provider-name");
	if (providerName == null)
		this.providerName = getNodeAttribute(node, "vendor");
	this.version = getNodeAttribute(node, "version");

	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			processChild(child);
		}
	}
}
void loadExtensionPoints(ExtensionPointModel[] extensionPointModels) {
	if (extensionPointModels==null) return;
	for (int i = 0; i < extensionPointModels.length; i++) {
		ExtensionPointModel extensionPointModel = extensionPointModels[i];
		PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
		extensionPoint.setModel(getModel());
		extensionPoint.setParent(this);
		extensionPoints.add(extensionPoint);
		extensionPoint.load(extensionPointModel);
	}
}
void loadExtensions(ExtensionModel[] extensionModels) {
	if (extensionModels==null) return;
	for (int i = 0; i < extensionModels.length; i++) {
		ExtensionModel extensionModel = extensionModels[i];
		PluginExtension extension = new PluginExtension();
		extension.setModel(getModel());
		extension.setParent(this);
		extensions.add(extension);
		extension.load(extensionModel);
	}
}
void loadRuntime(LibraryModel[] libraryModels) {
	if (libraryModels == null)
		return;
	for (int i = 0; i < libraryModels.length; i++) {
		PluginLibrary library = new PluginLibrary();
		library.setModel(getModel());
		library.setParent(this);
		libraries.add(library);
		library.load(libraryModels[i]);
	}
}
void loadRuntime(Node node) {
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE
			&& child.getNodeName().toLowerCase().equals("library")) {
			PluginLibrary library = new PluginLibrary();
			library.setModel(getModel());
			library.setParent(this);
			libraries.add(library);
			library.load(child);
		}
	}
}
protected void processChild(Node child) {
	String name = child.getNodeName().toLowerCase();
	if (name.equals("extension")) {
		PluginExtension extension = new PluginExtension();
		extension.setModel(getModel());
		extension.setParent(this);
		extensions.add(extension);
		extension.load(child);
	} else
		if (name.equals("extension-point")) {
			PluginExtensionPoint point = new PluginExtensionPoint();
			point.setModel(getModel());
			point.setParent(this);
			extensionPoints.add(point);
			point.load(child);
		} else
			if (name.equals("runtime")) {
				loadRuntime(child);
				addComments(child);
			}
}
public void remove(IPluginExtension extension) throws CoreException {
	ensureModelEditable();
	extensions.removeElement(extension);
	fireStructureChanged(extension, ModelChangedEvent.REMOVE);
}
public void remove(IPluginExtensionPoint extensionPoint) throws CoreException {
	ensureModelEditable();
	extensionPoints.removeElement(extensionPoint);
	fireStructureChanged(extensionPoint, ModelChangedEvent.REMOVE);
}
public void remove(IPluginLibrary library) throws CoreException {
	ensureModelEditable();
	libraries.removeElement(library);
	fireStructureChanged(library, ModelChangedEvent.REMOVE);
}
public void reset() {
	extensions = new Vector();
	extensionPoints = new Vector();
	libraries = new Vector();
	providerName = null;
	version ="";
	this.name ="";
	this.id="";
	if (getModel()!=null && getModel().getUnderlyingResource()!=null) {
		this.id = getModel().getUnderlyingResource().getProject().getName();
		this.name = this.id;
		this.version = "0.0.0";
	}
}
public void setProviderName(String providerName) throws CoreException {
	ensureModelEditable();
	this.providerName = providerName;
	firePropertyChanged(P_PROVIDER);
}
public void setVersion(String newVersion) throws CoreException {
	ensureModelEditable();
	version = newVersion;
	firePropertyChanged(P_VERSION);
}

public void internalSetVersion(String newVersion) {
	version = newVersion;
}

public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
	ensureModelEditable();
	int index1 = libraries.indexOf(l1);
	int index2 = libraries.indexOf(l2);
	if (index1== -1 || index2 == -1)
	   throwCoreException("Libraries not in the model");
	libraries.setElementAt(l1, index2);
	libraries.setElementAt(l2, index1);
	firePropertyChanged(this, P_LIBRARY_ORDER);
}
void writeChildren(String tag, Object[] children, PrintWriter writer) {
	if (tag.equals("runtime")) 
	   writeComments(writer);
	writer.println("<" + tag + ">");
	for (int i = 0; i < children.length; i++) {
		IPluginObject obj = (IPluginObject) children[i];
		obj.write("   ", writer);
	}
	writer.println("</" + tag + ">");
}
}
