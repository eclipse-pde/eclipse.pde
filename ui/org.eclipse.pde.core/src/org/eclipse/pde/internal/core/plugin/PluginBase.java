/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.util.ArrayList;
import java.util.Locale;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.osgi.framework.Version;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class PluginBase extends AbstractExtensions implements IPluginBase {
	private static final long serialVersionUID = 1L;

	private static final Version maxVersion = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private ArrayList<IPluginLibrary> fLibraries = new ArrayList<>();
	private ArrayList<IPluginImport> fImports = new ArrayList<>();
	private String fProviderName;
	private String fId;
	private String fVersion;
	private boolean fHasBundleStructure;
	private String fBundleSourceEntry;

	public PluginBase(boolean readOnly) {
		super(readOnly);
	}

	@Override
	public void add(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		fLibraries.add(library);
		((PluginLibrary) library).setInTheModel(true);
		((PluginLibrary) library).setParent(this);
		fireStructureChanged(library, IModelChangedEvent.INSERT);
	}

	@Override
	public void add(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		((PluginImport) iimport).setInTheModel(true);
		((PluginImport) iimport).setParent(this);
		fImports.add(iimport);
		fireStructureChanged(iimport, IModelChangedEvent.INSERT);
	}

	public void add(IPluginImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (IPluginImport iimport : iimports) {
			((PluginImport) iimport).setInTheModel(true);
			((PluginImport) iimport).setParent(this);
			fImports.add(iimport);
		}
		fireStructureChanged(iimports, IModelChangedEvent.INSERT);
	}

	@Override
	public IPluginLibrary[] getLibraries() {
		// Returns an empty array if no libraries are specified in the manifest of the plug-in.
		// If no libraries are specified, the root of the bundle '.' is the default library location
		return fLibraries.toArray(new IPluginLibrary[fLibraries.size()]);
	}

	@Override
	public IPluginImport[] getImports() {
		return fImports.toArray(new IPluginImport[fImports.size()]);
	}

	@Override
	public IPluginBase getPluginBase() {
		return this;
	}

	@Override
	public String getProviderName() {
		return fProviderName;
	}

	@Override
	public String getVersion() {
		return fVersion;
	}

	@Override
	public String getId() {
		return fId;
	}

	void load(BundleDescription bundleDesc, PDEState state) {
		fId = bundleDesc.getSymbolicName();
		fVersion = bundleDesc.getVersion().toString();
		fName = state.getPluginName(bundleDesc.getBundleId());
		fProviderName = state.getProviderName(bundleDesc.getBundleId());
		fHasBundleStructure = state.hasBundleStructure(bundleDesc.getBundleId());
		fBundleSourceEntry = state.getBundleSourceEntry(bundleDesc.getBundleId());
		loadRuntime(bundleDesc, state);
		loadImports(bundleDesc);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
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
		super.restoreProperty(name, oldValue, newValue);
	}

	void load(Node node, String schemaVersion) {
		if (node == null) {
			return;
		}
		fSchemaVersion = schemaVersion;
		fId = getNodeAttribute(node, "id"); //$NON-NLS-1$
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fProviderName = getNodeAttribute(node, "provider-name"); //$NON-NLS-1$
		fVersion = getNodeAttribute(node, "version"); //$NON-NLS-1$

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				processChild(child);
			}
		}
	}

	void loadRuntime(BundleDescription description, PDEState state) {
		String[] libraryNames = state.getLibraryNames(description.getBundleId());
		for (String libraryName : libraryNames) {
			PluginLibrary library = new PluginLibrary();
			library.setModel(getModel());
			library.setInTheModel(true);
			library.setParent(this);
			library.load(libraryName);
			fLibraries.add(library);
		}
	}

	void loadRuntime(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().toLowerCase(Locale.ENGLISH).equals("library")) { //$NON-NLS-1$
				PluginLibrary library = new PluginLibrary();
				library.setModel(getModel());
				library.setInTheModel(true);
				library.setParent(this);
				fLibraries.add(library);
				library.load(child);
			}
		}
	}

	void loadImports(BundleDescription description) {
		BundleSpecification[] required = description.getRequiredBundles();
		for (BundleSpecification spec : required) {
			PluginImport importElement = new PluginImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			fImports.add(importElement);
			importElement.load(spec);
		}
		BundleDescription[] imported = getImportedBundles(description);
		for (BundleDescription element : imported) {
			PluginImport importElement = new PluginImport();
			importElement.setModel(getModel());
			importElement.setInTheModel(true);
			importElement.setParent(this);
			fImports.add(importElement);
			importElement.load(element);
		}
	}

	/**
	 * Returns the bundles that export packages imported by the given bundle
	 * via the Import-Package header.  Provided as a static utility method so
	 * it can be reused in {@link BundlePluginBase}
	 *
	 * @param root the given bundle
	 *
	 * @return an array of bundles that export packages being imported by the given bundle
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null) {
			return new BundleDescription[0];
		}
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList<BundleDescription> resolvedImports = new ArrayList<>(packages.length);
		for (int i = 0; i < packages.length; i++) {
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter())) {
				resolvedImports.add(packages[i].getExporter());
			}
		}
		return resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

	void loadImports(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().toLowerCase(Locale.ENGLISH).equals("import")) { //$NON-NLS-1$
				PluginImport importElement = new PluginImport();
				importElement.setModel(getModel());
				importElement.setInTheModel(true);
				importElement.setParent(this);
				fImports.add(importElement);
				importElement.load(child);
			}
		}
	}

	@Override
	protected void processChild(Node child) {
		String name = child.getNodeName().toLowerCase(Locale.ENGLISH);
		if (name.equals("runtime")) { //$NON-NLS-1$
			loadRuntime(child);
		} else if (name.equals("requires")) { //$NON-NLS-1$
			loadImports(child);

			// check to see if this model is a workspace model.  If so, don't load extensions/extension points through Node.
			// Instead, the extensions/extension points will be control by the extension registry.
			// One instance of where we want to load an external model's extensions/extension points from a Node is the convertSchemaToHTML ANT task.
		} else if (getModel().getUnderlyingResource() == null) {
			super.processChild(child);
		}
	}

	@Override
	public void remove(IPluginLibrary library) throws CoreException {
		ensureModelEditable();
		fLibraries.remove(library);
		((PluginLibrary) library).setInTheModel(false);
		fireStructureChanged(library, IModelChangedEvent.REMOVE);
	}

	@Override
	public void remove(IPluginImport iimport) throws CoreException {
		ensureModelEditable();
		fImports.remove(iimport);
		((PluginImport) iimport).setInTheModel(false);
		fireStructureChanged(iimport, IModelChangedEvent.REMOVE);
	}

	public void remove(IPluginImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (IPluginImport iimport : iimports) {
			fImports.remove(iimport);
			((PluginImport) iimport).setInTheModel(false);
		}
		fireStructureChanged(iimports, IModelChangedEvent.REMOVE);
	}

	@Override
	public void reset() {
		fLibraries = new ArrayList<>();
		fImports = new ArrayList<>();
		fProviderName = null;
		fSchemaVersion = null;
		fVersion = ""; //$NON-NLS-1$
		fName = ""; //$NON-NLS-1$
		fId = ""; //$NON-NLS-1$
		if (getModel() != null && getModel().getUnderlyingResource() != null) {
			fId = getModel().getUnderlyingResource().getProject().getName();
			fName = fId;
			fVersion = ICoreConstants.DEFAULT_VERSION;
		}
		super.reset();
	}

	@Override
	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		String oldValue = fProviderName;
		fProviderName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, fProviderName);
	}

	@Override
	public void setVersion(String newVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = fVersion;
		fVersion = newVersion;
		firePropertyChanged(P_VERSION, oldValue, fVersion);
	}

	@Override
	public void setId(String newId) throws CoreException {
		ensureModelEditable();
		String oldValue = fId;
		fId = newId;
		firePropertyChanged(P_ID, oldValue, fId);
	}

	public void internalSetVersion(String newVersion) {
		fVersion = newVersion;
	}

	@Override
	public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
		ensureModelEditable();
		int index1 = fLibraries.indexOf(l1);
		int index2 = fLibraries.indexOf(l2);
		if (index1 == -1 || index2 == -1) {
			throwCoreException(PDECoreMessages.PluginBase_librariesNotFoundException);
		}
		fLibraries.set(index2, l1);
		fLibraries.set(index1, l2);
		firePropertyChanged(this, P_LIBRARY_ORDER, l1, l2);
	}

	@Override
	public void swap(IPluginImport import1, IPluginImport import2) throws CoreException {
		ensureModelEditable();
		int index1 = fImports.indexOf(import1);
		int index2 = fImports.indexOf(import2);
		if (index1 == -1 || index2 == -1) {
			throwCoreException(PDECoreMessages.PluginBase_importsNotFoundException);
		}
		fImports.set(index2, import1);
		fImports.set(index1, import2);
		firePropertyChanged(this, P_IMPORT_ORDER, import1, import2);
	}

	@Override
	public boolean isValid() {
		return hasRequiredAttributes();
	}

	@Override
	protected boolean hasRequiredAttributes() {
		if (fName == null) {
			return false;
		}
		if (fId == null) {
			return false;
		}
		if (fVersion == null) {
			return false;
		}

		// validate libraries
		for (int i = 0; i < fLibraries.size(); i++) {
			IPluginLibrary library = fLibraries.get(i);
			if (!library.isValid()) {
				return false;
			}
		}
		// validate imports
		for (int i = 0; i < fImports.size(); i++) {
			IPluginImport iimport = fImports.get(i);
			if (!iimport.isValid()) {
				return false;
			}
		}
		return super.hasRequiredAttributes();
	}

	protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
		return SAXParserFactory.newInstance().newSAXParser();
	}

	public static int getMatchRule(VersionRange versionRange) {
		if (versionRange == null || versionRange.getMinimum() == null) {
			return IMatchRules.NONE;
		}

		Version minimum = versionRange.getLeft();
		Version maximum = versionRange.getRight() == null ? maxVersion : versionRange.getRight();

		if (maximum.compareTo(maxVersion) >= 0) {
			return IMatchRules.GREATER_OR_EQUAL;
		} else if (minimum.equals(maximum)) {
			return IMatchRules.PERFECT;
		} else if (!versionRange.isIncluded(minimum) || versionRange.isIncluded(maximum)) {
			return IMatchRules.NONE; // no real match rule for this
		} else if (minimum.getMajor() == maximum.getMajor() - 1) {
			return IMatchRules.COMPATIBLE;
		} else if (minimum.getMajor() != maximum.getMajor()) {
			return IMatchRules.NONE; // no real match rule for this
		} else if (minimum.getMinor() == maximum.getMinor() - 1) {
			return IMatchRules.EQUIVALENT;
		} else if (minimum.getMinor() != maximum.getMinor()) {
			return IMatchRules.NONE; // no real match rule for this
		} else if (minimum.getMicro() == maximum.getMicro() - 1) {
			return IMatchRules.PERFECT; // this is as close as we got
		}

		return IMatchRules.NONE; // no real match rule for this
	}

	public boolean hasBundleStructure() {
		return fHasBundleStructure;
	}

	/**
	 * @return The bundle source entry from the manifest for this plugin or <code>null</code> if no entry exists.
	 */
	public String getBundleSourceEntry() {
		return fBundleSourceEntry;
	}

}
