package org.eclipse.pde.internal.core.bundle;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.osgi.framework.*;
import org.osgi.framework.Constants;

public class BundlePluginBase
	extends PlatformObject
	implements IBundlePluginBase {
	private IBundlePluginModelBase model;
	private ArrayList libraries;
	private ArrayList imports;
	private String fId;
	private String fName;
	private String fProvider;
	private String fVersion;

	public void reset() {
		libraries = null;
		imports = null;
		fId = null;
		fName = null;
		fProvider = null;
		fVersion = null;
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
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginBase#getBundle()
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
		if (libraries != null) {
			libraries.add(library);
			fireStructureChanged(library, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
		if (libraries != null) {
			libraries.remove(library);
			fireStructureChanged(library, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
		if (imports != null) {
			imports.add(pluginImport);
			fireStructureChanged(pluginImport, true);			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		if (imports != null) {
			imports.remove(pluginImport);
			fireStructureChanged(pluginImport, false);	
		}
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
			} else {
				Dictionary manifest = getManifest();
				if (manifest != null) {
					try {
						String value = (String) manifest.get(Constants.REQUIRE_BUNDLE);
						if (value != null) {
							ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, value);
							for (int i = 0; i < elements.length; i++) {
								PluginImport importElement = new PluginImport();
								importElement.setModel(getModel());
								importElement.setInTheModel(true);
								importElement.setParent(this);
								imports.add(importElement);
								importElement.load(elements[i]);							
							}
						}
					} catch (BundleException e) {
					}				
				}
			}
		}
		return (IPluginImport[])imports.toArray(new IPluginImport[imports.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		return fProvider != null ? fProvider : parseSingleValuedHeader(Constants.BUNDLE_VENDOR);
	}
	
	protected String parseSingleValuedHeader(String header) {
		String[] values = parseMultiValuedHeader(header);
		return (values.length > 0) ? values[values.length - 1] : null;
	}
	
	protected String[] parseMultiValuedHeader(String header) {
		Dictionary manifest = getManifest();
		if (manifest == null)
			return new String[0];
		String value = (String)manifest.get(header);
		if (value == null)
			return new String[0];
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(header, value);
			if (elements.length > 0)
				return elements[0].getValueComponents();
		} catch (BundleException e) {
		}
		return new String[0];		
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		Object oldValue = getProviderName();
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
			else
				return null;
		} 
		return fVersion != null ? fVersion : parseSingleValuedHeader(Constants.BUNDLE_VERSION);
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		Object oldValue = getVersion();
		fVersion = version;
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
		if (imports != null) {
			int index1 = imports.indexOf(l1);
			int index2 = imports.indexOf(l2);
			imports.set(index1, l2);
			imports.set(index2, l1);
			model.fireModelObjectChanged(this, P_IMPORT_ORDER, l1, l2);
		}		
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
		if (imports != null) {
			int index1 = imports.indexOf(import1);
			int index2 = imports.indexOf(import2);
			imports.set(index1, import2);
			imports.set(index2, import1);
			model.fireModelObjectChanged(this, P_IMPORT_ORDER, import1, import2);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) 
			return desc.getSymbolicName();
		return fId != null ? fId : parseSingleValuedHeader(Constants.BUNDLE_SYMBOLICNAME);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		Object oldValue = getId();
		fId = id;
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
		return fName != null ? fName : parseSingleValuedHeader(Constants.BUNDLE_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		Object oldValue = getName();
		fName = name;
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
	}
}
