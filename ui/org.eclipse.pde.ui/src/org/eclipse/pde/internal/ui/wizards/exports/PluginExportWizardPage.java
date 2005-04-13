/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
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
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;


public class PluginExportWizardPage extends ExportWizardPageWithTable {
	
	public PluginExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"pluginExport", //$NON-NLS-1$
			PDEUIMessages.ExportWizard_Plugin_pageBlock); //$NON-NLS-1$
		setTitle(PDEUIMessages.ExportWizard_Plugin_pageTitle); //$NON-NLS-1$
	}

	public Object[] getListElements() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList result = new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			if (!WorkspaceModelManager.isBinaryPluginProject(projects[i])
				&& WorkspaceModelManager.isPluginProject(projects[i])) {
				IModel model = PDECore.getDefault().getModelManager().findModel(projects[i]);
				if (model != null && isValidModel(model) && hasBuildProperties((IPluginModelBase)model)) {
					result.add(model);
				}
			}
		}
		return (IModel[]) result.toArray(new IModel[result.size()]);
	}
	
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}
	
	private boolean hasBuildProperties(IPluginModelBase model) {
		File file = new File(model.getInstallLocation(),"build.properties"); //$NON-NLS-1$
		return file.exists();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizardPage#isValidModel(org.eclipse.pde.core.IModel)
	 */
	protected boolean isValidModel(IModel model) {
		return model != null && model instanceof IPluginModelBase;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizardPage#findModelFor(org.eclipse.core.runtime.IAdaptable)
	 */
	protected IModel findModelFor(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject)object).getProject();
		if (object instanceof IProject)
			return PDECore.getDefault().getModelManager().findModel((IProject)object);
		if (object instanceof PersistablePluginObject) {
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(((PersistablePluginObject)object).getPluginID());
			if (entry != null) {
				return entry.getWorkspaceModel();
			}
		}
		return null;
	}
	
	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_packageJARs; //$NON-NLS-1$
	}
				
}
