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

import java.util.*;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IWorkspaceModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.help.WorkbenchHelp;


public class FeatureExportWizardPage extends BaseExportWizardPage {
	
	private static String S_SELECTED_FEATURES = "selectedFeatures";
	
	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport",
			PDEPlugin.getResourceString("ExportWizard.Feature.pageBlock"),
			true);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Feature.pageTitle"));
	}

	public Object[] getListElements() {
		IWorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getFeatureModels();
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
	}
	
	protected void checkSelected() {
		IDialogSettings settings = getDialogSettings();
		String selectedPlugins = settings.get(S_SELECTED_FEATURES);
		if (selectedPlugins == null) {
			super.checkSelected();
		} else {
			ArrayList tokens = new ArrayList();
			StringTokenizer tokenizer = new StringTokenizer(selectedPlugins, ",");
			while (tokenizer.hasMoreTokens()) {
				tokens.add(tokenizer.nextToken());
			}
			ArrayList selected = new ArrayList();
			IFeatureModel[] models = PDECore.getDefault().getWorkspaceModelManager().getFeatureModels();
			for (int i = 0; i < models.length; i++) {
				if (tokens.contains(models[i].getFeature().getId()))
					selected.add(models[i]);
			}
			exportPart.setSelection(selected.toArray());
		}		
	}
	
	public void saveSettings() {
		super.saveSettings();
		Object[] selected = exportPart.getSelection();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < selected.length; i++) {
			IFeatureModel model = (IFeatureModel)selected[i];
			buffer.append(model.getFeature().getId());
			if (i < selected.length - 1)
				buffer.append(",");
		}
		if (buffer.length() > 0)
			getDialogSettings().put(S_SELECTED_FEATURES, buffer.toString());
	}

}
