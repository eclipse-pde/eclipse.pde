/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.osgi.framework.Constants;

public abstract class WorkspaceBundlePluginModelBase extends WorkspacePluginModelBase implements IBundlePluginModelBase {

	private static final long serialVersionUID = 1L;
	private ISharedExtensionsModel fExtensionsModel = null;
	private IBundleModel fBundleModel = null;
	private final IFile fPluginFile;

	public WorkspaceBundlePluginModelBase(IFile manifestFile, IFile pluginFile) {
		super(manifestFile, false);
		fPluginFile = pluginFile;
	}

	@Override
	abstract public IPluginBase createPluginBase();

	@Override
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		if (fPluginBase == null) {
			fPluginBase = createPluginBase();
		}

		if (fBundleModel == null) {
			fBundleModel = new WorkspaceBundleModel((IFile) getUnderlyingResource());
		}
		fBundleModel.load(stream, outOfSync);
	}

	@Override
	public void save() {
		if (fExtensionsModel != null && fExtensionsModel.getExtensions().getExtensions().length > 0) {
			((BundlePluginBase) fPluginBase).updateSingleton(true);
			if (((IEditableModel) fExtensionsModel).isDirty()) {
				((IEditableModel) fExtensionsModel).save();
			}
		}

		if (fBundleModel != null && ((IEditableModel) fBundleModel).isDirty()) {
			((IEditableModel) fBundleModel).save();
		}
	}

	@Override
	public String getContents() {
		if (fBundleModel != null && fBundleModel instanceof WorkspaceBundleModel) {
			return ((WorkspaceBundleModel) fBundleModel).getContents();
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
	public IBundleModel getBundleModel() {
		if (fBundleModel == null) {
			IFile file = (IFile) getUnderlyingResource();
			fBundleModel = new WorkspaceBundleModel(file);
			if (file.exists()) {
				try {
					fBundleModel.load();
				} catch (CoreException e) {
				}
			}
		}
		return fBundleModel;
	}

	@Override
	public ISharedExtensionsModel getExtensionsModel() {
		if (fExtensionsModel == null && fPluginFile != null) {
			fExtensionsModel = new WorkspaceExtensionsModel(fPluginFile);
			((WorkspaceExtensionsModel) fExtensionsModel).setBundleModel(this);
			((WorkspaceExtensionsModel) fExtensionsModel).setEditable(isEditable());
			if (fPluginFile.exists()) {
				try {
					fExtensionsModel.load();
				} catch (CoreException e) {
				}
			}
		}
		return fExtensionsModel;
	}

	@Override
	public void setBundleModel(IBundleModel bundleModel) {
		if (bundleModel instanceof IEditableModel) {
			fBundleModel = bundleModel;
		}
	}

	@Override
	public void setExtensionsModel(ISharedExtensionsModel extensionsModel) {
		if (extensionsModel instanceof IEditableModel) {
			fExtensionsModel = extensionsModel;
		}
	}

}
