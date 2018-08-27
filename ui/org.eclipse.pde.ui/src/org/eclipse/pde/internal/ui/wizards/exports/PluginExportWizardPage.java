/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

public class PluginExportWizardPage extends BaseExportWizardPage {

	public PluginExportWizardPage(IStructuredSelection selection) {
		super(selection, "pluginExport", //$NON-NLS-1$
				PDEUIMessages.ExportWizard_Plugin_pageBlock);
		setTitle(PDEUIMessages.ExportWizard_Plugin_pageTitle);
	}

	@Override
	protected Object getInput() {
		return PDECore.getDefault().getModelManager();
	}

	@Override
	public Object[] getListElements() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<IModel> result = new ArrayList<>();
		for (int i = 0; i < projects.length; i++) {
			if (!WorkspaceModelManager.isBinaryProject(projects[i]) && WorkspaceModelManager.isPluginProject(projects[i])) {
				IModel model = PluginRegistry.findModel(projects[i]);
				if (model != null && isValidModel(model) && hasBuildProperties((IPluginModelBase) model)) {
					result.add(model);
				}
			}
		}
		return result.toArray();
	}

	@Override
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}

	private boolean hasBuildProperties(IPluginModelBase model) {
		File file = new File(model.getInstallLocation(), ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
		return file.exists();
	}

	@Override
	protected boolean isValidModel(IModel model) {
		return model != null && model instanceof IPluginModelBase;
	}

	@Override
	protected IModel findModelFor(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject) object).getProject();
		if (object instanceof IProject)
			return PluginRegistry.findModel((IProject) object);
		if (object instanceof PersistablePluginObject) {
			IPluginModelBase model = PluginRegistry.findModel(((PersistablePluginObject) object).getPluginID());
			if (model != null && model.getUnderlyingResource() != null) {
				return model;
			}
		}
		return null;
	}

	protected boolean isEnableJarButton() {
		return getSelectedItems().length <= 1;
	}

	@Override
	protected void adjustAdvancedTabsVisibility() {
		adjustJARSigningTabVisibility();
		pageChanged();
	}
}
