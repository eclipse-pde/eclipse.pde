/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.osgi.framework.Version;
import org.w3c.dom.*;
import org.xml.sax.*;

public abstract class PluginBase
	extends AbstractExtensions
	implements IPluginBase {
	private static final Version maxVersion = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private Vector libraries = new Vector();
	private Vector imports = new Vector();
	private String providerName;
	private String id;
	private String version;
	private String schemaVersion;
	private boolean valid;

	private String fTargetVersion;

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

	void load(BundleDescription bundleDesc, PDEState state, boolean ignoreExtensions) {
		this.id = bundleDesc.getSymbolicName();
		this.version = bundleDesc.getVersion().toString();
		this.name = state.getPluginName(bundleDesc.getBundleId());
		this.providerName = state.getProviderName(bundleDesc.getBundleId());
		fTargetVersion = "3.1";
		loadRuntime(bundleDesc, state);
		loadImports(bundleDesc);		
		if (!ignoreExtensions) {
			loadExtensionPoints(state.getExtensionPoints(bundleDesc.getBundleId()));
			loadExtensions(state.getExtensions(bundleDesc.getBundleId()));
		}
	}
	
	void loadExtensions(NodeList list) {
		if (list != null) {
			extensions = new Vector();
			for (int i = 0; i < list.getLength(); i++) {
				PluginExtension extension = new PluginExtension();
				extension.setInTheModel(true);
				extension.setModel(getModel());
				extension.setParent(this);
				extension.load(list.item(i));
				extensions.add(extension);
			}
		}
	}
	
	void loadExtensionPoints(NodeList list) {
		if (list != null) {
			extensionPoints = new Vector();
			for (int i = 0; i < list.getLength(); i++) {
				PluginExtensionPoint extPoint = new PluginExtensionPoint();
				extPoint.setInTheModel(true);
				extPoint.setModel(getModel());
				extPoint.setParent(this);
				extPoint.load(list.item(i));
				extensionPoints.add(extPoint);
			}
		}	
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

	void load(Node node, String schemaVersion, Hashtable lineTable) {
		bindSourceLocation(node, lineTable);
		this.schemaVersion = schemaVersion;
		this.id = getNodeAttribute(node, "id"); //$NON-NLS-1$
		this.name = getNodeAttribute(node, "name"); //$NON-NLS-1$
		this.providerName = getNodeAttribute(node, "provider-name"); //$NON-NLS-1$
		if (providerName == null)
			this.providerName = getNodeAttribute(node, "vendor"); //$NON-NLS-1$
		this.version = getNodeAttribute(node, "version"); //$NON-NLS-1$

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				processChild(child, lineTable);
			}
		}
		valid = hasRequiredAttributes();
	}

	void loadRuntime(BundleDescription description, PDEState state) {
		String[] libraryNames = state.getLibraryNames(description.getBundleId());
		for (int i = 0; i < libraryNames.length; i++) {
			PluginLibrary library = new PluginLibrary();
			library.setModel(getModel());
			library.setInTheModel(true);
			library.setParent(this);
			library.load(libraryNames[i]);
			libraries.add(library);
		}
	}

	void loadRuntime(Node node, Hashtable lineTable) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& child.getNodeName().toLowerCase().equals("library")) { //$NON-NLS-1$
				PluginLibrary library = new PluginLibrary();
				library.setModel(getModel());
				library.setInTheModel(true);
				library.setParent(this);
				libraries.add(library);
				library.load(child, lineTable);
			}
		}
	}

	void loadImports(BundleDescription description) {
		BundleSpecification[] required = description.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			PluginImport importElement = new PluginImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			imports.add(importElement);
			importElement.load(required[i]);
		}
		BundleDescription[] imported = PDEStateHelper.getImportedBundles(description);
		for (int i = 0; i < imported.length; i++) {
			PluginImport importElement = new PluginImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			imports.add(importElement);
			importElement.load(imported[i]);
		}		
	}
	
	void loadImports(Node node, Hashtable lineTable) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& child.getNodeName().toLowerCase().equals("import")) { //$NON-NLS-1$
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
		if (name.equals("runtime")) { //$NON-NLS-1$
			loadRuntime(child, lineTable);
		} else if (name.equals("requires")) { //$NON-NLS-1$
			loadImports(child, lineTable);
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
		providerName = null;
		schemaVersion = null;
		version = ""; //$NON-NLS-1$
		this.name = ""; //$NON-NLS-1$
		this.id = ""; //$NON-NLS-1$
		if (getModel() != null && getModel().getUnderlyingResource() != null) {
			this.id = getModel().getUnderlyingResource().getProject().getName();
			this.name = this.id;
			this.version = "0.0.0"; //$NON-NLS-1$
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
			throwCoreException(PDECore.getResourceString("PluginBase.librariesNotFoundException")); //$NON-NLS-1$
		libraries.setElementAt(l1, index2);
		libraries.setElementAt(l2, index1);
		firePropertyChanged(this, P_LIBRARY_ORDER, l1, l2);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginImport, org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void swap(IPluginImport import1, IPluginImport import2)
			throws CoreException {
		ensureModelEditable();
		int index1 = imports.indexOf(import1);
		int index2 = imports.indexOf(import2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(PDECore.getResourceString("PluginBase.importsNotFoundException")); //$NON-NLS-1$
		imports.setElementAt(import1, index2);
		imports.setElementAt(import2, index1);
		firePropertyChanged(this, P_IMPORT_ORDER, import1, import2);
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
	
	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError  {
		return SAXParserFactory.newInstance().newSAXParser();
	}

	public static int getMatchRule(VersionRange versionRange) {
		if (versionRange == null || versionRange.getMinimum() == null)
			return IMatchRules.NONE;

		Version minimum = versionRange.getMinimum();
		Version maximum = versionRange.getMaximum() == null ? maxVersion : versionRange.getMaximum();

		if (maximum.compareTo(maxVersion) >= 0)
			return IMatchRules.GREATER_OR_EQUAL;
		else if (minimum.equals(maximum))
			return IMatchRules.PERFECT;
		else if (!versionRange.isIncluded(minimum) || versionRange.isIncluded(maximum))
			return IMatchRules.NONE; // no real match rule for this
		else if (minimum.getMajor() == maximum.getMajor() - 1)
			return IMatchRules.COMPATIBLE;
		else if (minimum.getMajor() != maximum.getMajor())
			return IMatchRules.NONE; // no real match rule for this
		else if (minimum.getMinor() == maximum.getMinor() - 1)
			return IMatchRules.EQUIVALENT;
		else if (minimum.getMinor() != maximum.getMinor())
			return IMatchRules.NONE; // no real match rule for this
		else if (minimum.getMicro() == maximum.getMicro() - 1)
			return IMatchRules.PERFECT; // this is as close as we got

		return IMatchRules.NONE;  // no real match rule for this
	}
	
	public String getTargetVersion() {
		return fTargetVersion;
	}
	
	public void setTargetVersion(String version) {
		fTargetVersion = version;
	}
}
