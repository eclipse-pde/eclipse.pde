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

import java.io.*;
import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.help.WorkbenchHelp;


public class PluginExportWizardPage extends BaseExportWizardPage {
	
	private static String S_SELECTED_PLUGINS = "selectedPlugins";
	
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
				if (model != null && hasBuildProperties((WorkspacePluginModelBase)model)) {
					result.add(model);
				}
			}
		}
		return (IModel[]) result.toArray(new IModel[result.size()]);
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}
	
	private boolean hasBuildProperties(WorkspacePluginModelBase model) {
		File file = new File(model.getInstallLocation(),"build.properties");
		return file.exists();
	}
	
	protected void checkSelected() {
		IDialogSettings settings = getDialogSettings();
		String selectedPlugins = settings.get(S_SELECTED_PLUGINS);
		if (selectedPlugins == null) {
			super.checkSelected();
		} else {
			ArrayList selected = new ArrayList();
			StringTokenizer tokenizer = new StringTokenizer(selectedPlugins, ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				IPluginModelBase model = PDECore.getDefault().getModelManager().findPlugin(token,null,0);
				if (model != null && model instanceof WorkspacePluginModelBase) {
					selected.add(model);
				}
			}
			exportPart.setSelection(selected.toArray());
		}		
	}
	
	public void saveSettings() {
		super.saveSettings();
		Object[] selected = exportPart.getSelection();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < selected.length; i++) {
			IPluginModelBase model = (IPluginModelBase)selected[i];
			buffer.append(model.getPluginBase().getId());
			if (i < selected.length - 1)
				buffer.append(",");
		}
		if (buffer.length() > 0)
			getDialogSettings().put(S_SELECTED_PLUGINS, buffer.toString());
	}
				
}
