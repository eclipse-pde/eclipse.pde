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
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.IBundle;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.*;

public abstract class PluginBase
	extends AbstractExtensions
	implements IPluginBase {
	private Vector libraries = new Vector();
	private Vector imports = new Vector();
	protected Vector requiresComments;
	private String providerName;
	private String id;
	private String version;
	private String schemaVersion;
	private boolean valid;

	public PluginBase() {
	}
	public String getSchemaVersion() {
		return schemaVersion;
	}
	public void setSchemaVersion(String schemaVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = this.schemaVersion;
		this.schemaVersion = schemaVersion;
		firePropertyChanged(P_SCHEMA_VERSION, oldValue, schemaVersion);
	}
	public void add(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		libraries.addElement(library);
		((PluginLibrary) library).setInTheModel(true);
		((PluginLibrary) library).setParent(this);
		fireStructureChanged(library, IModelChangedEvent.INSERT);
	}
	public void add(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		((PluginImport) iimport).setInTheModel(true);
		((PluginImport) iimport).setParent(this);
		imports.addElement(iimport);
		fireStructureChanged(iimport, IModelChangedEvent.INSERT);
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
	public String getId() {
		return id;
	}
	void load(PluginModel pd) {
		this.id = pd.getId();
		this.name = pd.getName();
		this.providerName = pd.getProviderName();
		this.version = pd.getVersion();
		// TODO load schema version from plug-in model
		// when available
		// this.schemaVersion = pd.getSchemaVersion();
		//

		// add libraries
		loadRuntime(pd.getRuntime());
		// add extensions
		loadExtensions(pd.getDeclaredExtensions());
		// add extension points
		loadExtensionPoints(pd.getDeclaredExtensionPoints());
		// add imports
		loadImports(pd.getRequires());
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_PROVIDER)) {
			setProviderName(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_LIBRARY_ORDER)) {
			swap((IPluginLibrary) oldValue, (IPluginLibrary) newValue);
			return;
		}
		if (name.equals(P_SCHEMA_VERSION)) {
			setSchemaVersion(newValue!=null? newValue.toString():null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void load(IPluginBase srcPluginBase) {
		PluginBase base = (PluginBase)srcPluginBase;
		range= base.range;
		id= base.id;
		name= base.name;
		providerName= base.providerName;
		version= base.version;
		schemaVersion = base.schemaVersion;
		super.load(srcPluginBase);
		addArrayToVector(imports, srcPluginBase.getImports());
		addArrayToVector(libraries, srcPluginBase.getLibraries());
		valid = hasRequiredAttributes();
	}
	
	void load(Node node, Hashtable lineTable) {
	}

	void load(Node node, String schemaVersion, Hashtable lineTable) {
		bindSourceLocation(node, lineTable);
		this.schemaVersion = schemaVersion;
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
		valid = hasRequiredAttributes();
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
		if (name.equals("runtime")) {
			loadRuntime(child, lineTable);
			addComments(child);
		} else if (name.equals("requires")) {
			loadImports(child, lineTable);
			requiresComments = addComments(child, requiresComments);
		}
		else super.processChild(child, lineTable);
	}

	public void remove(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		libraries.removeElement(library);
		((PluginLibrary) library).setInTheModel(false);
		fireStructureChanged(library, ModelChangedEvent.REMOVE);
	}
	public void remove(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		imports.removeElement(iimport);
		((PluginImport) iimport).setInTheModel(false);
		fireStructureChanged(iimport, ModelChangedEvent.REMOVE);
	}
	public void reset() {
		libraries = new Vector();
		imports = new Vector();
		requiresComments = null;
		providerName = null;
		schemaVersion = null;
		version = "";
		this.name = "";
		this.id = "";
		if (getModel() != null && getModel().getUnderlyingResource() != null) {
			this.id = getModel().getUnderlyingResource().getProject().getName();
			this.name = this.id;
			this.version = "0.0.0";
		}
		super.reset();
		valid=false;
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
	
	public void setId(String newId) throws CoreException {
		ensureModelEditable();
		String oldValue = id;
		id = newId;
		firePropertyChanged(P_ID, oldValue, id);
	}

	public void internalSetVersion(String newVersion) {
		version = newVersion;
	}

	public void swap(IPluginLibrary l1, IPluginLibrary l2)
		throws CoreException {
		ensureModelEditable();
		int index1 = libraries.indexOf(l1);
		int index2 = libraries.indexOf(l2);
		if (index1 == -1 || index2 == -1)
			throwCoreException("Libraries not in the model");
		libraries.setElementAt(l1, index2);
		libraries.setElementAt(l2, index1);
		firePropertyChanged(this, P_LIBRARY_ORDER, l1, l2);
	}

	protected void writeChildren(
		String indent,
		String tag,
		Object[] children,
		PrintWriter writer) {
		if (tag.equals("runtime"))
			writeComments(writer);
		super.writeChildren(indent, tag, children, writer);
	}
	public boolean isValid() {
		return valid;
	}
	protected boolean hasRequiredAttributes(){
		if (name==null) return false;
		if (id==null) return false;
		if (version==null) return false;

		// validate libraries
		for (int i = 0; i < libraries.size(); i++) {
			IPluginLibrary library = (IPluginLibrary)libraries.get(i);
			if (!library.isValid()) return false;
		}
		// validate imports
		for (int i = 0; i < imports.size(); i++) {
			IPluginImport iimport = (IPluginImport)imports.get(i);
			if (!iimport.isValid()) return false;
		}
		return super.hasRequiredAttributes();
	}
	public void load(IBundle bundle, IExtensions extensions) {
		load(extensions);
		valid = hasRequiredAttributes();
	}
}
