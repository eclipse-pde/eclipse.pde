/*
 * Created on Oct 1, 2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.osgi.framework.*;
import org.osgi.framework.Constants;

/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class BundlePluginBase
	extends PlatformObject
	implements IBundlePluginBase {
	private IBundlePluginModelBase model;
	private ArrayList libraries;
	private ArrayList imports;

	public void reset() {
		libraries = null;
		imports = null;
	}
	
	public String getSchemaVersion() {
		return "3.0";
	}
	
	public void setSchemaVersion(String value) throws CoreException {
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == ModelChangedEvent.WORLD_CHANGED) {
			reset();
		} else if (event.getChangeType() == ModelChangedEvent.CHANGE) {
			String header = event.getChangedProperty();
			if (header.equals(Constants.IMPORT_PACKAGE)
				|| header.equals(Constants.REQUIRE_BUNDLE))
				imports = null;
			else if (header.equals(Constants.BUNDLE_CLASSPATH))
				libraries = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginBase#getBundle()
	 */
	public Dictionary getManifest() {
		if (model != null) {
			IBundleModel bmodel = model.getBundleModel();
			return bmodel != null ? bmodel.getManifest() : null;
		}
		return null;
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
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginBase#getExtensionsRoot()
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
		Dictionary manifest = getManifest();
		if (manifest == null)
			return;

		if (libraries != null) {
			libraries.add(library);
		}
		
		String libName = library.getName();
		String cp = (String)manifest.get(Constants.BUNDLE_CLASSPATH);
		cp = (cp == null) ? libName : cp + ", " + libName;
		manifest.put(Constants.BUNDLE_CLASSPATH, cp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
		throwException("Cannot remove library from BundlePlugin");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return;

		if (imports != null) {
			imports.add(pluginImport);
			fireStructureChanged(pluginImport, true);			
		}
		String rname = pluginImport.getId();
		String header = (String)manifest.get(Constants.REQUIRE_BUNDLE);
		header = (header == null) ? rname : header + ", " + rname;
		manifest.put(Constants.REQUIRE_BUNDLE, header);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		throwException("Cannot remove import from BundlePlugin");
		fireStructureChanged(pluginImport, false);		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return new IPluginLibrary[0];
		if (libraries == null) {
			libraries = new ArrayList();
			String[] libNames = PDEStateHelper.getClasspath(manifest);
			for (int i = 0; i < libNames.length; i++) {
				PluginLibrary library = new PluginLibrary();
				library.setModel(getModel());
				library.setInTheModel(true);
				library.setParent(this);
				library.load(libNames[i]);
				libraries.add(library);
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
			}
		}
		return (IPluginImport[])imports.toArray(new IPluginImport[imports.size()]);
	}

		
	public void setImportAttributes(IPluginImport iimport, String key, String value) throws CoreException{
		if (value == null || value.length() == 0)
			return;
		
		if (key.equals(Constants.BUNDLE_VERSION_ATTRIBUTE )) {
			iimport.setVersion(value);
		} else if (key.equals(Constants.REPROVIDE_ATTRIBUTE) || key.equals("provide-packages")) {
			iimport.setReexported(value.equals("true"));
		} else if (key.equals(Constants.VERSION_MATCH_ATTRIBUTE)) {
			if (value.equalsIgnoreCase(Constants.VERSION_MATCH_QUALIFIER) || 
					value.equalsIgnoreCase(Constants.VERSION_MATCH_MICRO)) {
				iimport.setMatch(IMatchRules.PERFECT);
			} else if (value.equalsIgnoreCase(Constants.VERSION_MATCH_GREATERTHANOREQUAL)) {
				iimport.setMatch(IMatchRules.GREATER_OR_EQUAL);
			} else if (value.equalsIgnoreCase(Constants.VERSION_MATCH_MINOR)) {
				iimport.setMatch(IMatchRules.EQUIVALENT);
			}
		} else if (key.equals(Constants.OPTIONAL_ATTRIBUTE)) {
			iimport.setOptional(value.equals("true"));
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		return parseHeader(Constants.BUNDLE_VENDOR);
	}
	
	private String parseHeader(String header) {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return "";
		String value = (String)manifest.get(header);
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(header, value);
			if (elements.length > 0)
				return elements[0].getValue();
		} catch (BundleException e) {
		}
		return "";
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return;
		Object oldValue = getProviderName();
		manifest.put(Constants.BUNDLE_VENDOR, providerName);
		model.fireModelObjectChanged(this, IPluginBase.P_PROVIDER, oldValue, providerName);			
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
			if (version != null)
				return version.toString();
		} 
		return parseHeader(Constants.BUNDLE_VERSION);
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return;
		Object oldValue = getVersion();
		manifest.put(Constants.BUNDLE_VERSION, version);
		model.fireModelObjectChanged(this, IPluginBase.P_VERSION, oldValue, version);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginLibrary,
	 *      org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void swap(IPluginLibrary l1, IPluginLibrary l2)
		throws CoreException {
		throwException("Cannot swap libraries in BundlePlugin");
	}
	
	private void fireStructureChanged(Object object, boolean added) {
		int type = (added)?IModelChangedEvent.INSERT:IModelChangedEvent.REMOVE;
		model.fireModelChanged(new ModelChangedEvent(model, type, new Object[]{object}, null ));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void add(IPluginExtension extension) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return;
		extensions.add(extension);
		fireStructureChanged(extension, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void add(IPluginExtensionPoint point) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return;
		extensions.add(point);
		fireStructureChanged(point, true);
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
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null) {
			extensions.remove(extension);
			fireStructureChanged(extension, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void remove(IPluginExtensionPoint extensionPoint)
		throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null) {
			extensions.remove(extensionPoint);
			fireStructureChanged(extensionPoint, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension,
	 *      org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2)
		throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null) {
			extensions.swap(e1, e2);
			model.fireModelObjectChanged(this, P_EXTENSION_ORDER, e1, e2);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginImport, org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void swap(IPluginImport import1, IPluginImport import2)
			throws CoreException {	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		BundleDescription desc = model.getBundleDescription();
		return (desc != null) ? desc.getSymbolicName() : parseHeader(Constants.BUNDLE_SYMBOLICNAME);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return;
		Object oldValue = getId();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, id);
		model.fireModelObjectChanged(this, IPluginBase.P_ID, oldValue, id);
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
		return parseHeader(Constants.BUNDLE_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return;
		Object oldValue = getProviderName();
		manifest.put(Constants.BUNDLE_NAME, name);
		model.fireModelObjectChanged(this, IPluginBase.P_NAME, oldValue, name);
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
		return getName();
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
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return key;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		IExtensions extensions = getExtensionsRoot();
		return getManifest() != null
			&& getManifest().get(Constants.BUNDLE_SYMBOLICNAME) != null
			&& (extensions == null || extensions.isValid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}

	private void throwException(String message) throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				PDECore.PLUGIN_ID,
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
	}
}
