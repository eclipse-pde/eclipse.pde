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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.help.WorkbenchHelp;


public class PluginExportWizardPage extends BaseExportWizardPage {
	private Label label;


	public PluginExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"pluginExport",
			PDEPlugin.getResourceString("ExportWizard.Plugin.pageBlock"),
			false);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Plugin.pageTitle"));
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getAllModels();
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}
				
}
