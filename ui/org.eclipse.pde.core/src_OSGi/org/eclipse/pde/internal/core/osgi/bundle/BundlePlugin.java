/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.io.PrintWriter;
import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BundlePlugin extends PlatformObject implements IBundlePluginBase {
	private IBundlePluginModelBase model;
	private ArrayList libraries;
	private ArrayList imports;
	
	public void reset() {
		libraries = null;
		imports = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginBase#getBundle()
	 */
	public IBundle getBundle() {
		if (model!=null) {
			IBundleModel bmodel = model.getBundleModel();
			return bmodel!=null?bmodel.getBundle():null;
		}
		return null;
	}
	
	public ISharedPluginModel getModel() {
		return model;
	}
	
	void setModel(IBundlePluginModelBase model) {
		this.model = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginBase#getExtensionsRoot()
	 */
	public IExtensions getExtensionsRoot() {
		if (model!=null) {
			IExtensionsModel emodel = model.getExtensionsModel();
			return emodel!=null?emodel.getExtensions():null;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void add(IPluginLibrary library) throws CoreException {
		throwException("Cannot add library to BundlePlugin");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
		throwException("Cannot add import to BundlePlugin");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		throwException("Cannot remove import from BundlePlugin");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		if (libraries==null) {
			libraries = new ArrayList();
			StringTokenizer stok = new StringTokenizer(getSafeHeader(IBundle.KEY_CLASSPATH), ",");
			while (stok.hasMoreTokens()) {
				String token = stok.nextToken().trim();
				try {
					IPluginLibrary library = model.createLibrary();
					library.setName(token);
					libraries.add(library);
				}
				catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		}
		return (IPluginLibrary[])libraries.toArray(new IPluginLibrary[libraries.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getImports()
	 */
	public IPluginImport[] getImports() {
		if (imports==null) {
			imports = new ArrayList();
			StringTokenizer stok = new StringTokenizer(getSafeHeader(IBundle.KEY_REQUIRE_BUNDLE), ",");
			String token = stok.nextToken().trim();
			try {
				IPluginImport iimport = model.createImport();
				iimport.setId(token);
				imports.add(iimport);
			}
			catch (CoreException e) {
				PDECore.logException(e);
			}
		}
		return (IPluginImport[])imports.toArray(new IPluginImport[imports.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		IBundle bundle = getBundle();
		if (bundle==null) return null;
		return bundle.getHeader(IBundle.KEY_VENDOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getVersion()
	 */
	public String getVersion() {
		IBundle bundle = getBundle();
		if (bundle==null) return null;
		return bundle.getHeader(IBundle.KEY_VERSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
		throwException("Cannot remove library from BundlePlugin");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			bundle.setHeader(IBundle.KEY_VENDOR, providerName);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			bundle.setHeader(IBundle.KEY_VERSION, version);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginLibrary, org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void swap(IPluginLibrary l1, IPluginLibrary l2)
		throws CoreException {
		throwException("Cannot swap libraries in BundlePlugin");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#load(org.eclipse.pde.core.osgi.bundle.IBundle, org.eclipse.pde.core.plugin.IExtensions)
	 */
	public void load(IBundle bundle, IExtensions extensions) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void add(IPluginExtension extension) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions==null) return;
		extensions.add(extension);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void add(IPluginExtensionPoint point) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions==null) return;
		extensions.add(point);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensionPoints()
	 */
	public IPluginExtensionPoint[] getExtensionPoints() {
		IExtensions extensions = getExtensionsRoot();
		if (extensions==null) return new IPluginExtensionPoint[0];
		return extensions.getExtensionPoints();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensions()
	 */
	public IPluginExtension[] getExtensions() {
		IExtensions extensions = getExtensionsRoot();
		if (extensions==null) return new IPluginExtension[0];
		return extensions.getExtensions();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void remove(IPluginExtension extension) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions!=null)
			extensions.remove(extension);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void remove(IPluginExtensionPoint extensionPoint)
		throws CoreException {
			IExtensions extensions = getExtensionsRoot();
			if (extensions!=null)
				extensions.remove(extensionPoint);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension, org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2)
		throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions!=null)
			extensions.swap(e1, e2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#load(org.eclipse.pde.core.plugin.IExtensions)
	 */
	public void load(IExtensions plugin) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		IBundle bundle = getBundle();
		if (bundle==null) return null;
		String id = bundle.getHeader(IBundle.KEY_UNIQUEID);
		if (id==null)
			id = bundle.getHeader(IBundle.KEY_NAME);
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			bundle.setHeader(IBundle.KEY_NAME, id);
			bundle.setHeader(IBundle.KEY_UNIQUEID, id);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginModel()
	 */
	public IPluginModelBase getPluginModel() {
		// TODO Auto-generated method stub
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		IBundle bundle = getBundle();
		if (bundle==null) return null;
		return bundle.getHeader(IBundle.KEY_DESC);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		return getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getParent()
	 */
	public IPluginObject getParent() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return key;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null)
			bundle.setHeader(IBundle.KEY_DESC, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		IBundle bundle = getBundle();
		IExtensions extensions = getExtensionsRoot();
		return bundle!=null && bundle.isValid() && (extensions==null || extensions.isValid());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}

	private void throwException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR,
				PDECore.PLUGIN_ID, 
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}
	
	private String getSafeHeader(String key) {
		String value = getBundle().getHeader(key);
		return value!=null?value:"";
	}
}
