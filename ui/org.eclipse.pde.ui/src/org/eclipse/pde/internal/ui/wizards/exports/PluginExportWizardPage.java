/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.help.WorkbenchHelp;


public class PluginExportWizardPage extends BaseExportWizardPage {

	public PluginExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"pluginExport",
			PDEPlugin.getResourceString("ExportWizard.Plugin.pageBlock"),
			false);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Plugin.pageTitle"));
	}

	public Object[] getListElements() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList result = new ArrayList();
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		for (int i = 0; i < projects.length; i++) {
			if (!WorkspaceModelManager.isBinaryPluginProject(projects[i])
				&& WorkspaceModelManager.isPluginProject(projects[i])) {
				IModel model = manager.getWorkspaceModel(projects[i]);
				if (model != null) {
					result.add(model);
				}
			}
		}
		return (IModel[]) result.toArray(new IModel[result.size()]);
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}
				
}
