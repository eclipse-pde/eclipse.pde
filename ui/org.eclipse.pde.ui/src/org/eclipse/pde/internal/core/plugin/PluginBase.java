package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.*;

public abstract class PluginBase
	extends IdentifiablePluginObject
	implements IPluginBase {
	private Vector extensions = new Vector();
	private Vector extensionPoints = new Vector();
	private Vector libraries = new Vector();
	private Vector imports = new Vector();
	protected Vector requiresComments;
	private String providerName;
	private String version;

	public PluginBase() {
	}
	public void add(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		extensions.addElement(extension);
		((PluginExtension)extension).setInTheModel(true);
		fireStructureChanged(extension, IModelChangedEvent.INSERT);
	}
	public void add(IPluginExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		extensionPoints.addElement(extensionPoint);
		((PluginExtensionPoint)extensionPoint).setInTheModel(true);
		fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
	}
	public void add(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		libraries.addElement(library);
		((PluginLibrary)library).setInTheModel(true);
		fireStructureChanged(library, IModelChangedEvent.INSERT);
	}
	public void add(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		((PluginImport)iimport).setInTheModel(true);
		imports.addElement(iimport);
		fireStructureChanged(iimport, IModelChangedEvent.INSERT);
	}
	public IPluginExtensionPoint[] getExtensionPoints() {
		IPluginExtensionPoint[] result =
			new IPluginExtensionPoint[extensionPoints.size()];
		extensionPoints.copyInto(result);
		return result;
	}
	public IPluginExtension[] getExtensions() {
		IPluginExtension[] result = new IPluginExtension[extensions.size()];
		extensions.copyInto(result);
		return result;
	}
	public IPluginLibrary[] getLibraries() {
		IPluginLibrary[] result = new IPluginLibrary[libraries.size()];
		libraries.copyInto(result);
		return result;
	}
	public IPluginImport[] getImports() {
		IPluginImport[] result = new IPluginImport[imports.size()];
		imports.copyInto(result);
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
		// add imports
		loadImports(pd.getRequires());
	}
	
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion(newValue!=null ? newValue.toString():null);
			return;
		}
		if (name.equals(P_PROVIDER)) {
			setProviderName(newValue!=null ? newValue.toString():null);
			return;
		}
		if (name.equals(P_LIBRARY_ORDER)) {
			swap((IPluginLibrary)oldValue, (IPluginLibrary)newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}
	
	void load(Node node, Hashtable lineTable) {
		bindSourceLocation(node, lineTable);
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
				processChild(child, lineTable);
			}
		}
	}
	void loadExtensionPoints(ExtensionPointModel[] extensionPointModels) {
		if (extensionPointModels == null)
			return;
		for (int i = 0; i < extensionPointModels.length; i++) {
			ExtensionPointModel extensionPointModel = extensionPointModels[i];
			PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
			extensionPoint.setModel(getModel());
			extensionPoint.setInTheModel(true);
			extensionPoint.setParent(this);
			extensionPoints.add(extensionPoint);
			extensionPoint.load(extensionPointModel);
		}
	}
	void loadExtensions(ExtensionModel[] extensionModels) {
		if (extensionModels == null)
			return;
		for (int i = 0; i < extensionModels.length; i++) {
			ExtensionModel extensionModel = extensionModels[i];
			PluginExtension extension = new PluginExtension();
			extension.setModel(getModel());
			extension.setInTheModel(true);
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
			library.setInTheModel(true);
			library.setParent(this);
			libraries.add(library);
			library.load(libraryModels[i]);
		}
	}
	void loadRuntime(Node node, Hashtable lineTable) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& child.getNodeName().toLowerCase().equals("library")) {
				PluginLibrary library = new PluginLibrary();
				library.setModel(getModel());
				library.setInTheModel(true);
				library.setParent(this);
				libraries.add(library);
				library.load(child, lineTable);
			}
		}
	}
	void loadImports(PluginPrerequisiteModel[] importModels) {
		if (importModels == null)
			return;

		for (int i = 0; i < importModels.length; i++) {
			PluginPrerequisiteModel importModel = importModels[i];
			PluginImport importElement = new PluginImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			imports.add(importElement);
			importElement.load(importModel);
		}
	}
	void loadImports(Node node, Hashtable lineTable) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& child.getNodeName().toLowerCase().equals("import")) {
				PluginImport importElement = new PluginImport();
				importElement.setModel(getModel());
				importElement.setInTheModel(true);
				importElement.setParent(this);
				imports.add(importElement);
				importElement.load(child, lineTable);
			}
		}
	}
	protected void processChild(Node child, Hashtable lineTable) {
		String name = child.getNodeName().toLowerCase();
		if (name.equals("extension")) {
			PluginExtension extension = new PluginExtension();
			extension.setModel(getModel());
			extension.setParent(this);
			extensions.add(extension);
			extension.setInTheModel(true);
			extension.load(child, lineTable);
		} else if (name.equals("extension-point")) {
			PluginExtensionPoint point = new PluginExtensionPoint();
			point.setModel(getModel());
			point.setParent(this);
			point.setInTheModel(true);
			extensionPoints.add(point);
			point.load(child, lineTable);
		} else if (name.equals("runtime")) {
			loadRuntime(child, lineTable);
			addComments(child);
		} else if (name.equals("requires")) {
			loadImports(child, lineTable);
			requiresComments = addComments(child, requiresComments);
		}
	}
	public void remove(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		extensions.removeElement(extension);
		((PluginExtension)extension).setInTheModel(false);
		fireStructureChanged(extension, ModelChangedEvent.REMOVE);
	}
	public void remove(IPluginExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		extensionPoints.removeElement(extensionPoint);
		((PluginExtensionPoint)extensionPoint).setInTheModel(false);
		fireStructureChanged(extensionPoint, ModelChangedEvent.REMOVE);
	}
	public void remove(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		libraries.removeElement(library);
		((PluginLibrary)library).setInTheModel(false);
		fireStructureChanged(library, ModelChangedEvent.REMOVE);
	}
	public void remove(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		imports.removeElement(iimport);
		((PluginImport)iimport).setInTheModel(false);
		fireStructureChanged(iimport, ModelChangedEvent.REMOVE);
	}
	public void reset() {
		extensions = new Vector();
		extensionPoints = new Vector();
		libraries = new Vector();
		imports = new Vector();
		requiresComments = null;
		providerName = null;
		version = "";
		this.name = "";
		this.id = "";
		if (getModel() != null && getModel().getUnderlyingResource() != null) {
			this.id = getModel().getUnderlyingResource().getProject().getName();
			this.name = this.id;
			this.version = "0.0.0";
		}
	}
	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		String oldValue = this.providerName;
		this.providerName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, providerName);
	}
	public void setVersion(String newVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = version;
		version = newVersion;
		firePropertyChanged(P_VERSION, oldValue, version);
	}

	public void internalSetVersion(String newVersion) {
		version = newVersion;
	}

	public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
		ensureModelEditable();
		int index1 = libraries.indexOf(l1);
		int index2 = libraries.indexOf(l2);
		if (index1 == -1 || index2 == -1)
			throwCoreException("Libraries not in the model");
		libraries.setElementAt(l1, index2);
		libraries.setElementAt(l2, index1);
		firePropertyChanged(this, P_LIBRARY_ORDER, l1, l2);
	}
	
	public int getExtensionCount() {
		return extensions.size();
	}
	
	public int getIndexOf(IPluginExtension e) {
		return extensions.indexOf(e);
	}
	public void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException {
		ensureModelEditable();
		int index1 = extensions.indexOf(e1);
		int index2 = extensions.indexOf(e2);
		if (index1 == -1 || index2 == -1)
			throwCoreException("Extensions not in the model");
		extensions.setElementAt(e1, index2);
		extensions.setElementAt(e2, index1);
		firePropertyChanged(this, P_EXTENSION_ORDER, e1, e2);
	}
	void writeChildren(String indent, String tag, Object[] children, PrintWriter writer) {
		if (tag.equals("runtime"))
			writeComments(writer);
		writer.println(indent + "<" + tag + ">");
		for (int i = 0; i < children.length; i++) {
			IPluginObject obj = (IPluginObject) children[i];
			obj.write(indent + "   ", writer);
		}
		writer.println(indent + "</" + tag + ">");
	}
}