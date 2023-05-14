/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Constants;

public abstract class BaseManifestOperation implements IRunnableWithProgress {

	private Shell fShell;
	protected String fPluginId;

	public BaseManifestOperation(Shell shell, String pluginId) {
		fShell = shell;
		fPluginId = pluginId;
	}

	protected Shell getShell() {
		return fShell;
	}

	protected IFile getFile() {
		IPluginModelBase model = PluginRegistry.findModel(fPluginId);
		IProject project = model.getUnderlyingResource().getProject();
		return model instanceof IFragmentModel ? PDEProject.getFragmentXml(project) : PDEProject.getPluginXml(project);
	}

	protected IPluginModelBase getModel(IFile file) {
		if (ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR.equals(file.getName()))
			return new WorkspacePluginModel(file, false);
		return new WorkspaceFragmentModel(file, false);
	}

	protected void updateSingleton(IProgressMonitor monitor) throws CoreException {
		IPluginModelBase plugin = PluginRegistry.findModel(fPluginId);
		if (plugin instanceof IBundlePluginModel) {
			IFile file = (IFile) plugin.getUnderlyingResource();
			IStatus status = PDEPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
			if (!status.isOK())
				throw new CoreException(Status.error(NLS.bind(PDEUIMessages.ProductDefinitionOperation_readOnly, fPluginId), null));

			ModelModification mod = new ModelModification(file) {
				@Override
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (!(model instanceof IBundlePluginModelBase))
						return;
					IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
					IBundle bundle = modelBase.getBundleModel().getBundle();
					IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
					if (header instanceof BundleSymbolicNameHeader) {
						BundleSymbolicNameHeader symbolic = (BundleSymbolicNameHeader) header;
						if (!symbolic.isSingleton())
							symbolic.setSingleton(true);
					}
				}
			};
			PDEModelUtility.modifyModel(mod, null);
		}
	}
}
