/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
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
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.bnd.BndProjectManager;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

/**
 * Page of the {@link BundleExportWizard} to select the workspace bundles
 * (plug-ins and fragments) to export.
 */
public class BundleExportWizardPage extends BaseExportWizardPage {

	public BundleExportWizardPage(IStructuredSelection selection) {
		super(selection, "bundleExport", //$NON-NLS-1$
				PDEUIMessages.ExportWizard_Plugin_pageBlock);
		setTitle(PDEUIMessages.ExportWizard_Plugin_pageTitle);
	}

	@Override
	protected ExportOptionsTab createExportOptionsTab() {
		return new BundleExportOptionsTab(this);
	}

	@Override
	protected Object getInput() {
		return PDECore.getDefault().getModelManager();
	}

	@Override
	public Object[] getListElements() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<IModel> result = new ArrayList<>();
		for (IProject project : projects) {
			if (!WorkspaceModelManager.isBinaryProject(project) && WorkspaceModelManager.isPluginProject(project)) {
				IModel model = PluginRegistry.findModel(project);
				if (model != null && isValidModel(model) && (hasBuildProperties((IPluginModelBase) model) || hasPdeBndDescriptor(project))) {
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
		return new File(model.getInstallLocation(), ICoreConstants.BUILD_FILENAME_DESCRIPTOR).exists();
	}

	private boolean hasPdeBndDescriptor(IProject project) {
		try {
			return BndProjectManager.getBndProject(project).isPresent();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected boolean isValidModel(IModel model) {
		return model instanceof IPluginModelBase;
	}

	@Override
	protected IModel findModelFor(IAdaptable object) {
		if (object instanceof IJavaProject javaProject) {
			object = javaProject.getProject();
		}
		if (object instanceof IProject project) {
			return PluginRegistry.findModel(project);
		}
		if (object instanceof PersistablePluginObject pluginObject) {
			IPluginModelBase model = PluginRegistry.findModel(pluginObject.getPluginID());
			if (model != null && model.getUnderlyingResource() != null) {
				return model;
			}
		}
		return null;
	}

	@Override
	protected void adjustAdvancedTabsVisibility() {
		adjustJARSigningTabVisibility();
		pageChanged();
	}
}
