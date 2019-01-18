/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.AbstractNLModel;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
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

	@Override
	public IBundleModel getBundleModel() {
		return fBundleModel;
	}

	@Override
	public IResource getUnderlyingResource() {
		return fBundleModel.getUnderlyingResource();
	}

	@Override
	public ISharedExtensionsModel getExtensionsModel() {
		return fExtensionsModel;
	}

	@Override
	public void dispose() {
		if (fBundleModel != null) {
			if (fBundlePluginBase != null) {
				fBundleModel.removeModelChangedListener(fBundlePluginBase);
			}
			fBundleModel.dispose();
			fBundleModel = null;
		}
		if (fExtensionsModel != null) {
			if (fBundlePluginBase != null) {
				fExtensionsModel.removeModelChangedListener(fBundlePluginBase);
			}
			fExtensionsModel.dispose();
			fExtensionsModel = null;
		}
		super.dispose();
	}

	@Override
	public void save() {
		if (fBundleModel != null && fBundleModel instanceof IEditableModel) {
			IEditableModel emodel = (IEditableModel) fBundleModel;
			if (emodel.isDirty()) {
				emodel.save();
			}
		}
		if (fExtensionsModel != null && fExtensionsModel instanceof IEditableModel) {
			IEditableModel emodel = (IEditableModel) fExtensionsModel;
			if (emodel.isDirty()) {
				emodel.save();
			}
		}
	}

	@Override
	public void setBundleModel(IBundleModel bundleModel) {
		if (fBundleModel != null && fBundlePluginBase != null) {
			fBundleModel.removeModelChangedListener(fBundlePluginBase);
		}
		fBundleModel = bundleModel;
		if (fBundleModel != null && fBundlePluginBase != null) {
			bundleModel.addModelChangedListener(fBundlePluginBase);
		}
	}

	@Override
	public void setExtensionsModel(ISharedExtensionsModel extensionsModel) {
		if (fExtensionsModel != null && fBundlePluginBase != null) {
			fExtensionsModel.removeModelChangedListener(fBundlePluginBase);
		}
		fExtensionsModel = extensionsModel;
		if (fExtensionsModel instanceof PluginModelBase) {
			((PluginModelBase) fExtensionsModel).setLocalization(getBundleLocalization());
		}
		if (extensionsModel != null && fBundlePluginBase != null) {
			extensionsModel.addModelChangedListener(fBundlePluginBase);
		}
	}

	@Override
	@Deprecated
	public IBuildModel getBuildModel() {
		return fBuildModel;
	}

	public void setBuildModel(IBuildModel buildModel) {
		fBuildModel = buildModel;
	}

	@Override
	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}

	@Override
	public IExtensions getExtensions() {
		return getPluginBase();
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		super.fireModelChanged(event);
		Object[] objects = event.getChangedObjects();
		if (objects != null && objects.length > 0) {
			if (objects[0] instanceof IPluginImport) {
				fBundlePluginBase.updateImport((IPluginImport) objects[0]);
			}
		}
	}

	@Override
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (fBundlePluginBase == null && createIfMissing) {
			fBundlePluginBase = (BundlePluginBase) createPluginBase();
			if (fBundleModel != null) {
				fBundleModel.addModelChangedListener(fBundlePluginBase);
			}
			setLoaded(true);
		}
		return fBundlePluginBase;
	}

	@Override
	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}

	@Override
	public IPluginModelFactory getPluginFactory() {
		return this;
	}

	@Override
	public IExtensionsModelFactory getFactory() {
		if (fExtensionsModel != null) {
			return fExtensionsModel.getFactory();
		}
		return null;
	}

	@Override
	public String getInstallLocation() {
		if (fBundleModel != null) {
			return fBundleModel.getInstallLocation();
		}
		return null;
	}

	@Override
	public String getBundleLocalization() {
		IBundle bundle = fBundleModel != null ? fBundleModel.getBundle() : null;
		return bundle != null ? bundle.getLocalization() : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;

	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		String localization = getBundleLocalization();
		return localization == null ? null : new NLResourceHelper(localization, PDEManager.getNLLookupLocations(this));
	}

	@Override
	@Deprecated
	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public boolean isEditable() {
		if (fBundleModel != null && fBundleModel.isEditable() == false) {
			return false;
		}
		if (fExtensionsModel != null && fExtensionsModel.isEditable() == false) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isInSync() {
		return ((fBundleModel == null || fBundleModel.isInSync()) && (fExtensionsModel == null || fExtensionsModel.isInSync()));
	}

	@Override
	public boolean isValid() {
		return ((fBundleModel == null || fBundleModel.isValid()) && (fExtensionsModel == null || fExtensionsModel.isValid()));
	}

	@Override
	public void load() {
	}

	@Override
	public void load(InputStream source, boolean outOfSync) {
	}

	@Override
	public void reload(InputStream source, boolean outOfSync) {
	}

	/**
	 * @return Returns the enabled.
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void setEnabled(boolean enabled) {
	}

	@Override
	protected void updateTimeStamp() {
	}

	@Override
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

	@Override
	public IPluginLibrary createLibrary() {
		PluginLibrary library = new PluginLibrary();
		library.setModel(this);
		library.setParent(getPluginBase());
		return library;
	}

	@Override
	public IPluginAttribute createAttribute(IPluginElement element) {
		if (fExtensionsModel != null) {
			return fExtensionsModel.getFactory().createAttribute(element);
		}
		return null;
	}

	@Override
	public IPluginElement createElement(IPluginObject parent) {
		if (fExtensionsModel != null) {
			return fExtensionsModel.getFactory().createElement(parent);
		}
		return null;
	}

	@Override
	public IPluginExtension createExtension() {
		if (fExtensionsModel != null) {
			return fExtensionsModel.getFactory().createExtension();
		}
		return null;
	}

	@Override
	public IPluginExtensionPoint createExtensionPoint() {
		if (fExtensionsModel != null) {
			return fExtensionsModel.getFactory().createExtensionPoint();
		}
		return null;
	}

	public boolean isBundleModel() {
		return true;
	}

	@Override
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}

	@Override
	public void setBundleDescription(BundleDescription description) {
		fBundleDescription = description;
	}

	@Override
	public boolean isDirty() {
		if (fBundleModel != null && (fBundleModel instanceof IEditable) && ((IEditable) fBundleModel).isDirty()) {
			return true;
		}
		if (fExtensionsModel != null && (fExtensionsModel instanceof IEditable) && ((IEditable) fExtensionsModel).isDirty()) {
			return true;
		}
		return false;
	}

	@Override
	public void save(PrintWriter writer) {
		// Does nothing - individual models are saved instead
	}

	@Override
	public void setDirty(boolean dirty) {
		//does nothing
	}

	@Override
	public String toString() {
		IPluginBase base = getPluginBase();
		if (base != null) {
			return base.getId();
		}
		return super.toString();
	}
}
