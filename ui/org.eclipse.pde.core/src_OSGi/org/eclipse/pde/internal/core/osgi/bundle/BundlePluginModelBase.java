/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.io.InputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.plugin.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BundlePluginModelBase extends AbstractModel implements IBundlePluginModelBase {
	private IBundleModel bundleModel;
	private IExtensionsModel extensionsModel;
	private IBundlePluginBase bundlePluginBase;
	private IBuildModel buildModel;
	private boolean enabled;
	
	public BundlePluginModelBase() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginModelBase#getBundleModel()
	 */
	public IBundleModel getBundleModel() {
		return bundleModel;
	}
	
	public IResource getUnderlyingResource() {
		return bundleModel.getUnderlyingResource();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginModelBase#getExtensionsModel()
	 */
	public IExtensionsModel getExtensionsModel() {
		return extensionsModel;
	}
	
	public void dispose() {
		if (bundleModel!=null) {
			bundleModel.dispose();
			bundleModel=null;
		}
		if (extensionsModel!=null) {
			extensionsModel.dispose();
			extensionsModel=null;
		}
		super.dispose();
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginModelBase#setBundleModel(org.eclipse.pde.core.osgi.bundle.IBundleModel)
	 */
	public void setBundleModel(IBundleModel bundleModel) {
		this.bundleModel = bundleModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginModelBase#setExtensionsModel(org.eclipse.pde.core.plugin.IExtensionsModel)
	 */
	public void setExtensionsModel(IExtensionsModel extensionsModel) {
		this.extensionsModel = extensionsModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#createPluginBase()
	 */
	public IPluginBase createPluginBase() {
		BundlePlugin bplugin = new BundlePlugin();
		bplugin.setModel(this);
		return bplugin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBuildModel()
	 */
	public IBuildModel getBuildModel() {
		return buildModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase(boolean)
	 */
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (bundlePluginBase == null && createIfMissing) {
			bundlePluginBase = (BundlePlugin) createPluginBase();
			loaded = true;
		}
		return bundlePluginBase;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginFactory()
	 */
	public IPluginModelFactory getPluginFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getFactory()
	 */
	public IExtensionsModelFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return key;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isEditable()
	 */
	public boolean isEditable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isInSync()
	 */
	public boolean isInSync() {
		return ( (bundleModel==null||bundleModel.isInSync()) &&
				(extensionsModel==null || extensionsModel.isInSync()) );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isValid()
	 */
	public boolean isValid() {
		return ( (bundleModel==null||bundleModel.isValid()) &&
				(extensionsModel==null || extensionsModel.isValid()) );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load()
	 */
	public void load() throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync)
		throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync)
		throws CoreException {
	}
	/**
	 * @return Returns the enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled The enabled to set.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#updateTimeStamp()
	 */
	protected void updateTimeStamp() {
	}
	public IPluginImport createImport() {
		PluginImport iimport = new PluginImport();
		iimport.setModel(this);
		iimport.setParent(getPluginBase());
		return iimport;
	}
	public IPluginLibrary createLibrary() {
		PluginLibrary library = new PluginLibrary();
		library.setModel(this);
		library.setParent(getPluginBase());
		return library;
	}
}
