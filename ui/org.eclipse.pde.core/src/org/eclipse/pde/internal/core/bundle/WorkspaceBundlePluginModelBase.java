/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;

public abstract class WorkspaceBundlePluginModelBase extends WorkspacePluginModelBase implements IBundlePluginModelBase{

	private static final long serialVersionUID = 1L;
	private ISharedExtensionsModel fExtensionsModel = null;
	private IBundleModel fBundleModel = null;
	private IFile fPluginFile;

	public WorkspaceBundlePluginModelBase(IFile manifestFile, IFile pluginFile) {
		super(manifestFile, false);
		fPluginFile = pluginFile;
	}

	abstract public IPluginBase createPluginBase();

	public void load(InputStream stream, boolean outOfSync)	throws CoreException {
		if (fPluginBase == null) 
			fPluginBase = createPluginBase();

		if (fBundleModel == null) {
			fBundleModel = new WorkspaceBundleModel((IFile)getUnderlyingResource());
		}
		fBundleModel.load(stream, outOfSync);
	}

	public void save() {
		if (fExtensionsModel != null && fExtensionsModel.getExtensions().getExtensions().length > 0) {
			((BundlePluginBase)fPluginBase).updateSingleton(true);
			if (((IEditableModel) fExtensionsModel).isDirty())
				((IEditableModel) fExtensionsModel).save();
		}

		if (fBundleModel != null && ((IEditableModel)fBundleModel).isDirty())
			((IEditableModel)fBundleModel).save();
	}

	public String getContents() {
		if (fBundleModel != null && fBundleModel instanceof WorkspaceBundleModel)
			return ((WorkspaceBundleModel)fBundleModel).getContents();
		return null;
	}

	public String getBundleLocalization() {
		IBundle bundle = fBundleModel != null ? fBundleModel.getBundle() : null;
		return bundle != null ? bundle.getLocalization() : null;
	}

	public IBundleModel getBundleModel() {
		if (fBundleModel == null) {
			IFile file = (IFile)getUnderlyingResource();
			fBundleModel = new WorkspaceBundleModel((IFile)getUnderlyingResource());
			if (file.exists()) {
				try {
					fBundleModel.load();
				} catch (CoreException e) {
				}
			}
		}
		return fBundleModel;
	}

	public ISharedExtensionsModel getExtensionsModel() {
		if (fExtensionsModel == null) {
			fExtensionsModel = new WorkspaceExtensionsModel(fPluginFile);
			if (fPluginFile.exists())
				try {
					fExtensionsModel.load();
				} catch (CoreException e) {
				}
		}
		return fExtensionsModel;
	}

	public void setBundleModel(IBundleModel bundleModel) {
		if (bundleModel instanceof IEditableModel)
			fBundleModel = bundleModel;
	}


	public void setExtensionsModel(ISharedExtensionsModel extensionsModel) {
		if (extensionsModel instanceof IEditableModel)
			fExtensionsModel = extensionsModel;
	}

}
