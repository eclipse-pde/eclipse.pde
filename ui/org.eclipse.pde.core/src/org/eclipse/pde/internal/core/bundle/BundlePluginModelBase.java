/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.plugin.PluginImport;
import org.eclipse.pde.internal.core.plugin.PluginLibrary;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.osgi.framework.Constants;

public abstract class BundlePluginModelBase extends AbstractNLModel implements IBundlePluginModelBase, IPluginModelFactory {

	private static final long serialVersionUID = 1L;
	private IBundleModel fBundleModel;
	private ISharedExtensionsModel fExtensionsModel;
	private BundlePluginBase fBundlePluginBase;
	private IBuildModel fBuildModel;
	private BundleDescription fBundleDescription;

	public BundlePluginModelBase() {
		getPluginBase();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase#getBundleModel()
	 */
	public IBundleModel getBundleModel() {
		return fBundleModel;
	}

	public IResource getUnderlyingResource() {
		return fBundleModel.getUnderlyingResource();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase#getExtensionsModel()
	 */
	public ISharedExtensionsModel getExtensionsModel() {
		return fExtensionsModel;
	}

	public void dispose() {
		if (fBundleModel != null) {
			if (fBundlePluginBase != null)
				fBundleModel.removeModelChangedListener(fBundlePluginBase);
			fBundleModel.dispose();
			fBundleModel = null;
		}
		if (fExtensionsModel != null) {
			if (fBundlePluginBase != null)
				fExtensionsModel.removeModelChangedListener(fBundlePluginBase);
			fExtensionsModel.dispose();
			fExtensionsModel = null;
		}
		super.dispose();
	}

	public void save() {
		if (fBundleModel != null && fBundleModel instanceof IEditableModel) {
			IEditableModel emodel = (IEditableModel) fBundleModel;
			if (emodel.isDirty())
				emodel.save();
		}
		if (fExtensionsModel != null && fExtensionsModel instanceof IEditableModel) {
			IEditableModel emodel = (IEditableModel) fExtensionsModel;
			if (emodel.isDirty())
				emodel.save();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase#setBundleModel(org.eclipse.pde.internal.core.ibundle.IBundleModel)
	 */
	public void setBundleModel(IBundleModel bundleModel) {
		if (fBundleModel != null && fBundlePluginBase != null) {
			fBundleModel.removeModelChangedListener(fBundlePluginBase);
		}
		fBundleModel = bundleModel;
		if (fBundleModel != null && fBundlePluginBase != null)
			bundleModel.addModelChangedListener(fBundlePluginBase);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase#setExtensionsModel(org.eclipse.pde.core.plugin.IExtensionsModel)
	 */
	public void setExtensionsModel(ISharedExtensionsModel extensionsModel) {
		if (fExtensionsModel != null && fBundlePluginBase != null) {
			fExtensionsModel.removeModelChangedListener(fBundlePluginBase);
		}
		fExtensionsModel = extensionsModel;
		if (fExtensionsModel instanceof PluginModelBase) {
			((PluginModelBase) fExtensionsModel).setLocalization(getBundleLocalization());
		}
		if (extensionsModel != null && fBundlePluginBase != null)
			extensionsModel.addModelChangedListener(fBundlePluginBase);
	}

	public IBuildModel getBuildModel() {
		return fBuildModel;
	}

	public void setBuildModel(IBuildModel buildModel) {
		fBuildModel = buildModel;
	}

	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}

	public IExtensions getExtensions() {
		return getPluginBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangeProvider#fireModelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void fireModelChanged(IModelChangedEvent event) {
		super.fireModelChanged(event);
		Object[] objects = event.getChangedObjects();
		if (objects != null && objects.length > 0) {
			if (objects[0] instanceof IPluginImport) {
				fBundlePluginBase.updateImport((IPluginImport) objects[0]);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase(boolean)
	 */
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (fBundlePluginBase == null && createIfMissing) {
			fBundlePluginBase = (BundlePluginBase) createPluginBase();
			if (fBundleModel != null)
				fBundleModel.addModelChangedListener(fBundlePluginBase);
			setLoaded(true);
		}
		return fBundlePluginBase;
	}

	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginFactory()
	 */
	public IPluginModelFactory getPluginFactory() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getFactory()
	 */
	public IExtensionsModelFactory getFactory() {
		if (fExtensionsModel != null)
			return fExtensionsModel.getFactory();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		if (fBundleModel != null)
			return fBundleModel.getInstallLocation();
		return null;
	}

	public String getBundleLocalization() {
		IBundle bundle = fBundleModel != null ? fBundleModel.getBundle() : null;
		return bundle != null ? bundle.getLocalization() : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;

	}

	protected NLResourceHelper createNLResourceHelper() {
		String localization = getBundleLocalization();
		return localization == null ? null : new NLResourceHelper(localization, PDEManager.getNLLookupLocations(this));
	}

	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModel#isEditable()
	 */
	public boolean isEditable() {
		if (fBundleModel != null && fBundleModel.isEditable() == false)
			return false;
		if (fExtensionsModel != null && fExtensionsModel.isEditable() == false)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModel#isInSync()
	 */
	public boolean isInSync() {
		return ((fBundleModel == null || fBundleModel.isInSync()) && (fExtensionsModel == null || fExtensionsModel.isInSync()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModel#isValid()
	 */
	public boolean isValid() {
		return ((fBundleModel == null || fBundleModel.isValid()) && (fExtensionsModel == null || fExtensionsModel.isValid()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModel#load()
	 */
	public void load() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync) {
	}

	/**
	 * @return Returns the enabled.
	 */
	public boolean isEnabled() {
		return true;
	}

	public void setEnabled(boolean enabled) {
	}

	/*
	 * (non-Javadoc)
	 * 
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

	public IPluginImport createImport(String pluginId) {
		PluginImport iimport = new PluginImport(this, pluginId);
		iimport.setParent(getPluginBase());
		return iimport;
	}

	public IPluginLibrary createLibrary() {
		PluginLibrary library = new PluginLibrary();
		library.setModel(this);
		library.setParent(getPluginBase());
		return library;
	}

	public IPluginAttribute createAttribute(IPluginElement element) {
		if (fExtensionsModel != null)
			return fExtensionsModel.getFactory().createAttribute(element);
		return null;
	}

	public IPluginElement createElement(IPluginObject parent) {
		if (fExtensionsModel != null)
			return fExtensionsModel.getFactory().createElement(parent);
		return null;
	}

	public IPluginExtension createExtension() {
		if (fExtensionsModel != null)
			return fExtensionsModel.getFactory().createExtension();
		return null;
	}

	public IPluginExtensionPoint createExtensionPoint() {
		if (fExtensionsModel != null)
			return fExtensionsModel.getFactory().createExtensionPoint();
		return null;
	}

	public boolean isBundleModel() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBundleDescription()
	 */
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setBundleDescription(org.eclipse.osgi.service.resolver.BundleDescription)
	 */
	public void setBundleDescription(BundleDescription description) {
		fBundleDescription = description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IEditable#isDirty()
	 */
	public boolean isDirty() {
		if (fBundleModel != null && (fBundleModel instanceof IEditable) && ((IEditable) fBundleModel).isDirty())
			return true;
		if (fExtensionsModel != null && (fExtensionsModel instanceof IEditable) && ((IEditable) fExtensionsModel).isDirty())
			return true;
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IEditable#save(java.io.PrintWriter)
	 */
	public void save(PrintWriter writer) {
		// Does nothing - individual models are saved instead
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IEditable#setDirty(boolean)
	 */
	public void setDirty(boolean dirty) {
		//does nothing
	}

	public String toString() {
		IPluginBase base = getPluginBase();
		if (base != null)
			return base.getId();
		return super.toString();
	}
}
