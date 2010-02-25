/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.PDEStateHelper;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.AbstractExtensions;
import org.eclipse.pde.internal.core.plugin.PluginImport;
import org.eclipse.pde.internal.core.plugin.PluginLibrary;
import org.eclipse.pde.internal.core.text.bundle.BundleClasspathHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleNameHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleVendorHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleVersionHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundlePluginBase extends PlatformObject implements IBundlePluginBase, Serializable {

	private static final long serialVersionUID = 1L;
	protected IBundlePluginModelBase model;
	private ArrayList libraries;
	private ArrayList imports;
	private String fTarget;

	public void reset() {
		libraries = null;
		imports = null;
	}

	public String getSchemaVersion() {
		IExtensions root = getExtensionsRoot();
		if (root instanceof AbstractExtensions)
			return ((AbstractExtensions) root).getSchemaVersion();
		return (root instanceof IPluginBase) ? ((IPluginBase) root).getSchemaVersion() : null;
	}

	public void setSchemaVersion(String value) throws CoreException {
		IExtensions root = getExtensionsRoot();
		if (root == null)
			return;
		if (root instanceof AbstractExtensions)
			((AbstractExtensions) root).setSchemaVersion(value);
		if (root instanceof IPluginBase)
			((IPluginBase) root).setSchemaVersion(value);
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			if (event.getChangeProvider().equals(model.getBundleModel())) {
				reset();
			}
			getModel().fireModelChanged(event);
		} else if (!event.getChangeProvider().equals(model.getBundleModel())) {
			getModel().fireModelChanged(event);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getBundle()
	 */
	public IBundle getBundle() {
		if (model != null) {
			IBundleModel bmodel = model.getBundleModel();
			return bmodel != null ? bmodel.getBundle() : null;
		}
		return null;
	}

	protected IManifestHeader getManifestHeader(String key) {
		IBundle bundle = getBundle();
		return (bundle != null) ? bundle.getManifestHeader(key) : null;
	}

	public ISharedPluginModel getModel() {
		return model;
	}

	void setModel(IBundlePluginModelBase model) {
		this.model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getExtensionsRoot()
	 */
	public IExtensions getExtensionsRoot() {
		if (model != null) {
			ISharedExtensionsModel emodel = model.getExtensionsModel();
			return emodel != null ? emodel.getExtensions() : null;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void add(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		if (libraries == null)
			// if libraries == null, initialize the libraries varible by calling getLibraries()
			getLibraries();
		libraries.add(library);
		IManifestHeader header = getManifestHeader(Constants.BUNDLE_CLASSPATH);
		if (header instanceof BundleClasspathHeader) {
			((BundleClasspathHeader) header).addLibrary(library.getName());
		} else {
			addLibrary(library, header);
		}
		fireStructureChanged(library, true);
	}

	/**
	 * @param library
	 * @param header
	 */
	private void addLibrary(IPluginLibrary library, IManifestHeader header) {
		String value = header == null ? null : header.getValue();
		StringBuffer buffer = new StringBuffer(value == null ? "" : value); //$NON-NLS-1$
		if (value != null)
			buffer.append(",\n "); //$NON-NLS-1$
		buffer.append(library.getName());
		getBundle().setHeader(Constants.BUNDLE_CLASSPATH, buffer.toString());
	}

	/**
	 * Removes the specified library from the given 'Bundle-Classpath' header.
	 * 
	 * @param library library to remove
	 * @param header header to update
	 */
	private void removeLibrary(IPluginLibrary library, IManifestHeader header) {
		String value = header == null ? null : header.getValue();
		String name = library.getName();
		int index = value.indexOf(name);
		if (index >= 0) {
			// copy up to the removed library
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i > index; i++) {
				buffer.append(value.charAt(i));
			}
			int after = index + name.length();
			// delete (skip) comma
			if (after < value.length()) {
				while (value.charAt(after) == ',') {
					after++;
				}
			}
			// delete (skip) whitespace
			if (after < value.length()) {
				while (Character.isWhitespace(value.charAt(after))) {
					after++;
				}
			}
			// keep everything else
			while (after < value.length()) {
				buffer.append(value.charAt(after));
				after++;
			}
			getBundle().setHeader(Constants.BUNDLE_CLASSPATH, buffer.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		if (libraries != null) {
			libraries.remove(library);
			IManifestHeader header = getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if (header instanceof BundleClasspathHeader) {
				((BundleClasspathHeader) header).removeLibrary(library.getName());
			} else if (header != null) {
				removeLibrary(library, header);
			}
			fireStructureChanged(library, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		if (iimport == null)
			return;
		if (imports == null)
			// if imports == null, intitialize the imports list by calling getImports()
			getImports();
		addImport(iimport);
		fireStructureChanged(iimport, true);
	}

	public void add(IPluginImport[] iimports) throws CoreException {
		ensureModelEditable();
		if (iimports != null && iimports.length > 0) {
			if (imports == null)
				// if imports == null, initialize the imports list by calling getImports()
				getImports();
			for (int i = 0; i < iimports.length; i++) {
				if (iimports[i] != null)
					addImport(iimports[i]);
			}
			fireStructureChanged(iimports, true);
		}
	}

	private void addImport(IPluginImport iimport) {
		imports.add(iimport);
		Object header = getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header instanceof RequireBundleHeader) {
			((RequireBundleHeader) header).addBundle(iimport);
		} else {
			String value = header == null ? null : ((IManifestHeader) header).getValue();
			StringBuffer buffer = new StringBuffer(value == null ? "" : value); //$NON-NLS-1$
			if (value != null)
				buffer.append(",\n "); //$NON-NLS-1$
			buffer.append(iimport.getId());
			int bundleManifestVersion = getBundleManifestVersion(getBundle());
			if (iimport.isOptional())
				if (bundleManifestVersion > 1)
					buffer.append(";" + Constants.RESOLUTION_DIRECTIVE + ":=" + Constants.RESOLUTION_OPTIONAL); //$NON-NLS-1$ //$NON-NLS-2$
				else
					buffer.append(";" + ICoreConstants.OPTIONAL_ATTRIBUTE + "=true"); //$NON-NLS-1$ //$NON-NLS-2$
			if (iimport.isReexported())
				if (bundleManifestVersion > 1)
					buffer.append(";" + Constants.VISIBILITY_DIRECTIVE + ":=" + Constants.VISIBILITY_REEXPORT); //$NON-NLS-1$ //$NON-NLS-2$
				else
					buffer.append(";" + ICoreConstants.REPROVIDE_ATTRIBUTE + "=true"); //$NON-NLS-1$ //$NON-NLS-2$
			String version = iimport.getVersion();
			if (version != null && version.trim().length() > 0)
				buffer.append(";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + version.trim() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			getBundle().setHeader(Constants.REQUIRE_BUNDLE, buffer.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		ensureModelEditable();
		if (imports != null) {
			imports.remove(pluginImport);
			Object header = getManifestHeader(Constants.REQUIRE_BUNDLE);
			if (header instanceof RequireBundleHeader) {
				((RequireBundleHeader) header).removeBundle(pluginImport.getId());
			}
			fireStructureChanged(pluginImport, false);
		}
	}

	public void remove(IPluginImport[] pluginImports) throws CoreException {
		ensureModelEditable();
		if (imports != null) {
			for (int i = 0; i < pluginImports.length; i++) {
				imports.remove(pluginImports[i]);
				Object header = getManifestHeader(Constants.REQUIRE_BUNDLE);
				if (header instanceof RequireBundleHeader) {
					((RequireBundleHeader) header).removeBundle(pluginImports[i].getId());
				}
			}
			fireStructureChanged(pluginImports, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		IBundle bundle = getBundle();
		if (bundle == null)
			return new IPluginLibrary[0];
		if (libraries == null) {
			libraries = new ArrayList();
			String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
			if (value != null) {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
					for (int i = 0; i < elements.length; i++) {
						PluginLibrary library = new PluginLibrary();
						library.setModel(getModel());
						library.setInTheModel(true);
						library.setParent(this);
						library.load(elements[i].getValue());
						libraries.add(library);
					}
				} catch (BundleException e) {
				}
			}
		}
		return (IPluginLibrary[]) libraries.toArray(new IPluginLibrary[libraries.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getImports()
	 */
	public IPluginImport[] getImports() {
		if (imports == null) {
			imports = new ArrayList();
			BundleDescription description = model.getBundleDescription();
			if (description != null) {
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
			} else {
				IBundle bundle = getBundle();
				if (bundle != null) {
					try {
						String value = bundle.getHeader(Constants.REQUIRE_BUNDLE);
						int bundleManifestVersion = getBundleManifestVersion(bundle);
						if (value != null) {
							ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, value);
							for (int i = 0; i < elements.length; i++) {
								PluginImport importElement = new PluginImport();
								importElement.setModel(getModel());
								importElement.setInTheModel(true);
								importElement.setParent(this);
								imports.add(importElement);
								importElement.load(elements[i], bundleManifestVersion);
							}
						}
					} catch (BundleException e) {
					}
				}
			}
		}
		return (IPluginImport[]) imports.toArray(new IPluginImport[imports.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		IBundle bundle = getBundle();
		return bundle == null ? null : bundle.getHeader(Constants.BUNDLE_VENDOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getProviderName();
			IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_VENDOR);
			if (header instanceof BundleVendorHeader)
				((BundleVendorHeader) header).setVendor(providerName);
			else
				bundle.setHeader(Constants.BUNDLE_VENDOR, providerName);
			model.fireModelObjectChanged(this, IPluginBase.P_PROVIDER, old, providerName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getVersion()
	 */
	public String getVersion() {
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			Version version = desc.getVersion();
			return (version != null) ? version.toString() : null;
		}
		return getValue(Constants.BUNDLE_VERSION, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getVersion();
			IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_VERSION);
			if (header instanceof BundleVersionHeader)
				((BundleVersionHeader) header).setVersionRange(version);
			else
				bundle.setHeader(Constants.BUNDLE_VERSION, version);
			model.fireModelObjectChanged(this, IPluginBase.P_VERSION, old, version);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginLibrary,
	 *      org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
		ensureModelEditable();
		if (libraries != null) {
			int index1 = libraries.indexOf(l1);
			int index2 = libraries.indexOf(l2);
			libraries.set(index1, l2);
			libraries.set(index2, l1);
			Object header = getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if (header instanceof BundleClasspathHeader) {
				((BundleClasspathHeader) header).swap(index1, index2);
			}
			model.fireModelObjectChanged(this, P_IMPORT_ORDER, l1, l2);
		}
	}

	protected void fireStructureChanged(Object object, boolean added) {
		int type = (added) ? IModelChangedEvent.INSERT : IModelChangedEvent.REMOVE;
		model.fireModelChanged(new ModelChangedEvent(model, type, new Object[] {object}, null));
	}

	protected void fireStructureChanged(Object[] objects, boolean added) {
		int type = (added) ? IModelChangedEvent.INSERT : IModelChangedEvent.REMOVE;
		model.fireModelChanged(new ModelChangedEvent(model, type, objects, null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void add(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return;
		extensions.add(extension);

		// reset singleton
		if (getExtensions().length == 1 && getExtensionPoints().length == 0)
			updateSingleton(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void add(IPluginExtensionPoint point) throws CoreException {
		ensureModelEditable();
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return;
		extensions.add(point);

		//reset singleton
		if (getExtensions().length == 0 && getExtensionPoints().length == 1)
			updateSingleton(true);
	}

	public String getResourceString(String key) {
		return model.getResourceString(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensionPoints()
	 */
	public IPluginExtensionPoint[] getExtensionPoints() {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return new IPluginExtensionPoint[0];
		return extensions.getExtensionPoints();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensions()
	 */
	public IPluginExtension[] getExtensions() {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return new IPluginExtension[0];
		return extensions.getExtensions();
	}

	public int getIndexOf(IPluginExtension e) {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return -1;
		return extensions.getIndexOf(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void remove(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null) {
			extensions.remove(extension);
			// reset singleton directive
			if (getExtensions().length == 0 && getExtensionPoints().length == 0)
				updateSingleton(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void remove(IPluginExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null) {
			extensions.remove(extensionPoint);
			// reset singleton directive
			if (getExtensions().length == 0 && getExtensionPoints().length == 0)
				updateSingleton(false);
		}
	}

	protected void updateSingleton(boolean singleton) {
		IManifestHeader header = getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
		if (header instanceof BundleSymbolicNameHeader)
			((BundleSymbolicNameHeader) header).setSingleton(singleton);
		else {
			if (singleton) {
				String version = getBundle().getHeader(Constants.BUNDLE_MANIFESTVERSION);
				if (version == null)
					version = "1"; //$NON-NLS-1$
				String value = header.getValue();
				String singletonValue = null;
				if (version != null && Integer.parseInt(version) >= 2)
					singletonValue = Constants.SINGLETON_DIRECTIVE + ":=true"; //$NON-NLS-1$
				else
					singletonValue = Constants.SINGLETON_DIRECTIVE + "=true"; //$NON-NLS-1$
				if (value.indexOf(singletonValue) != -1)
					return;
				getBundle().setHeader(Constants.BUNDLE_SYMBOLICNAME, value + "; " + singletonValue); //$NON-NLS-1$
			}
			// No current need to remove singleton directive outside of text model.
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension,
	 *      org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException {
		ensureModelEditable();
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null) {
			extensions.swap(e1, e2);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginImport, org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void swap(IPluginImport import1, IPluginImport import2) throws CoreException {
		ensureModelEditable();
		if (imports != null) {
			int index1 = imports.indexOf(import1);
			int index2 = imports.indexOf(import2);
			imports.set(index1, import2);
			imports.set(index2, import1);
			Object header = getManifestHeader(Constants.REQUIRE_BUNDLE);
			if (header instanceof RequireBundleHeader) {
				((RequireBundleHeader) header).swap(index1, index2);
			}
			model.fireModelObjectChanged(this, P_IMPORT_ORDER, import1, import2);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getValue(Constants.BUNDLE_SYMBOLICNAME, true);
	}

	// The key should be a manifest header key, and parse should be true if it needs to be parsed by ManifestElement.parseHeader() 
	protected String getValue(String key, boolean parse) {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		String value = bundle.getHeader(key);
		if (value == null || !parse)
			return value;
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(key, value);
			if (elements.length > 0)
				return elements[0].getValue();
		} catch (BundleException e) {
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		ensureModelEditable();
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getId();
			IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			if (header instanceof BundleSymbolicNameHeader)
				((BundleSymbolicNameHeader) header).setId(id);
			else
				bundle.setHeader(Constants.BUNDLE_SYMBOLICNAME, id);
			model.fireModelObjectChanged(this, IIdentifiable.P_ID, old, id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginModel()
	 */
	public IPluginModelBase getPluginModel() {
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getValue(Constants.BUNDLE_NAME, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getName();
			IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_NAME);
			if (header instanceof BundleNameHeader)
				((BundleNameHeader) header).setBundleName(name);
			else
				bundle.setHeader(Constants.BUNDLE_NAME, name);
			model.fireModelObjectChanged(this, IPluginObject.P_NAME, old, name);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return model != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		return getResourceString(getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getParent()
	 */
	public IPluginObject getParent() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		IExtensions extensions = getExtensionsRoot();
		return getBundle() != null && getBundle().getHeader(Constants.BUNDLE_SYMBOLICNAME) != null && (extensions == null || extensions.isValid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
	}

	static public int getBundleManifestVersion(IBundle bundle) {
		String version = bundle.getHeader(Constants.BUNDLE_MANIFESTVERSION);
		if (version == null)
			return 1; // default to 1
		try {
			return Integer.parseInt(version);
		} catch (NumberFormatException e) {
			return 1; // default to 1
		}
	}

	public void updateImport(IPluginImport iimport) {
		Object header = getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header instanceof RequireBundleHeader && imports != null)
			((RequireBundleHeader) header).updateBundle(imports.indexOf(iimport), iimport);
	}

	public String getTargetVersion() {
		return fTarget != null ? fTarget : TargetPlatformHelper.getTargetVersionString();
	}

	public void setTargetVersion(String target) {
		fTarget = target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getIndexOf(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public int getIndexOf(IPluginImport targetImport) {
		if (imports == null) {
			return -1;
		}
		return imports.indexOf(targetImport);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getPreviousImport(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public IPluginImport getPreviousImport(IPluginImport targetImport) {
		// Ensure we have imports
		if (imports == null) {
			return null;
		} else if (imports.size() <= 1) {
			return null;
		}
		// Get the index of the target import
		int targetIndex = getIndexOf(targetImport);
		// Validate index
		if (targetIndex < 0) {
			// Target import does not exist
			return null;
		} else if (targetIndex == 0) {
			// Target import has no previous import
			return null;
		}
		// 1 <= index < size()
		// Get the previous import
		IPluginImport previousImport = (IPluginImport) imports.get(targetIndex - 1);

		return previousImport;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getNextImport(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public IPluginImport getNextImport(IPluginImport targetImport) {
		// Ensure we have imports
		if (imports == null) {
			return null;
		} else if (imports.size() <= 1) {
			return null;
		}
		// Get the index of the target import
		int targetIndex = getIndexOf(targetImport);
		// Get the index of the last import
		int lastIndex = imports.size() - 1;
		// Validate index
		if (targetIndex < 0) {
			// Target import does not exist
			return null;
		} else if (targetIndex >= lastIndex) {
			// Target import has no next element
			return null;
		}
		// 0 <= index < last element < size()
		// Get the next element
		IPluginImport nextImport = (IPluginImport) imports.get(targetIndex + 1);

		return nextImport;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#add(org.eclipse.pde.core.plugin.IPluginImport, int)
	 */
	public void add(IPluginImport iimport, int index) throws CoreException {
		ensureModelEditable();
		int importCount = 0;
		if (imports != null) {
			importCount = imports.size();
		}
		// Validate index
		if (index < 0) {
			return;
		} else if (index > importCount) {
			return;
		}
		// 0 <= index <= importCount
		// Add the element to the list
		if (imports == null) {
			// Intitialize the imports list by calling getImports()
			getImports();
			// Add the import to the end of the list
			addImport(iimport);
		} else {
			// Add the import to the list at the specified index
			addImport(iimport, index);
		}
		// Fire event
		fireStructureChanged(iimport, true);
	}

	/**
	 * @param iimport
	 * @param index
	 */
	private void addImport(IPluginImport iimport, int index) {
		// Get the header
		IManifestHeader header = getManifestHeader(Constants.REQUIRE_BUNDLE);
		if ((header instanceof RequireBundleHeader) == false) {
			addImport(iimport);
		} else {
			// Add the import to the local container
			imports.add(index, iimport);
			// Add the import to the header
			((RequireBundleHeader) header).addBundle(iimport, index);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary, int)
	 */
	public void add(IPluginLibrary library, int index) throws CoreException {
		ensureModelEditable();
		int libraryCount = 0;
		if (libraries != null) {
			libraryCount = libraries.size();
		}
		// Validate index
		if (index < 0) {
			return;
		} else if (index > libraryCount) {
			return;
		}
		// 0 <= index <= libraryCount
		if (libraries == null) {
			// Intitialize the library list by calling getLibraries()
			getLibraries();
		}
		// Get the header
		IManifestHeader header = getManifestHeader(Constants.BUNDLE_CLASSPATH);
		if ((header instanceof BundleClasspathHeader) == false) {
			// Add the library to the local container
			libraries.add(library);
			// Add the library to a newly created header
			addLibrary(library, header);
		} else {
			// Add the library to the local container at the specified index
			libraries.add(index, library);
			// Add the library to the existing header at the specified index
			((BundleClasspathHeader) header).addLibrary(library.getName(), index);
		}
		// Fire event
		fireStructureChanged(library, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getIndexOf(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public int getIndexOf(IPluginLibrary targetLibrary) {
		if (libraries == null) {
			return -1;
		}
		return libraries.indexOf(targetLibrary);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getNextLibrary(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public IPluginLibrary getNextLibrary(IPluginLibrary targetLibrary) {
		// Ensure we have libraries
		if (libraries == null) {
			return null;
		} else if (libraries.size() <= 1) {
			return null;
		}
		// Get the index of the target library
		int targetIndex = getIndexOf(targetLibrary);
		// Get the index of the last library
		int lastIndex = libraries.size() - 1;
		// Validate index
		if (targetIndex < 0) {
			// Target library does not exist
			return null;
		} else if (targetIndex >= lastIndex) {
			// Target library has no next element
			return null;
		}
		// 0 <= index < last element < size()
		// Get the next library
		IPluginLibrary nextLibrary = (IPluginLibrary) libraries.get(targetIndex + 1);

		return nextLibrary;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getPreviousLibrary(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public IPluginLibrary getPreviousLibrary(IPluginLibrary targetLibrary) {
		// Ensure we have libraries
		if (libraries == null) {
			return null;
		} else if (libraries.size() <= 1) {
			return null;
		}
		// Get the index of the target library
		int targetIndex = getIndexOf(targetLibrary);
		// Validate index
		if (targetIndex < 0) {
			// Target library does not exist
			return null;
		} else if (targetIndex == 0) {
			// Target library has no previous library
			return null;
		}
		// 1 <= index < size()
		// Get the previous library
		IPluginLibrary previousLibrary = (IPluginLibrary) libraries.get(targetIndex - 1);

		return previousLibrary;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!getModel().isEditable()) {
			throwCoreException(PDECoreMessages.PluginObject_readOnlyChange);
		}
	}

	protected void throwCoreException(String message) throws CoreException {
		Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, message, null);
		CoreException ce = new CoreException(status);
		ce.fillInStackTrace();
		throw ce;
	}

}
