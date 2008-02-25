/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionWizard;
import org.eclipse.ui.IEditorPart;

public class AddNewExtensionResolution extends AbstractPDEMarkerResolution {

	public AddNewExtensionResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return PDEUIMessages.AddNewExtensionResolution_description;
	}

	protected void createChange(IBaseModel model) {
		IEditorPart part = PDEPlugin.getActivePage().getActiveEditor();
		if (part instanceof ManifestEditor) {
			ManifestEditor editor = (ManifestEditor) part;
			IBaseModel base = editor.getAggregateModel();
			if (base instanceof IBundlePluginModelBase) {
				IBundlePluginModelBase pluginModel = (IBundlePluginModelBase) base;
				NewExtensionWizard wizard = new NewExtensionWizard(pluginModel.getUnderlyingResource().getProject(), pluginModel, editor) {
					public boolean performFinish() {
						return super.performFinish();
					}
				};
				WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, 500);
				dialog.open();
			}
		}
	}

}