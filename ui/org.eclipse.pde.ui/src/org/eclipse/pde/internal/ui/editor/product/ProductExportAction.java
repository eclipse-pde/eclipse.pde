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
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.wizards.ResizableWizardDialog;
import org.eclipse.pde.internal.ui.wizards.exports.ProductExportWizard;
import org.eclipse.ui.PlatformUI;

public class ProductExportAction extends Action {

	private IProject fProject;

	private IStructuredSelection fSelection;

	public ProductExportAction(PDEFormEditor editor) {
		IResource resource = null;
		if (editor != null) {
			IModel model = (IModel) editor.getAggregateModel();
			resource = model == null ? null : model.getUnderlyingResource();
		}
		fSelection = resource != null ? new StructuredSelection(resource) : new StructuredSelection();
		fProject = editor.getCommonProject();
	}

	public ProductExportAction(IStructuredSelection selection) {
		fSelection = selection;
		fProject = null;
	}

	@Override
	public void run() {
		ProductExportWizard wizard = new ProductExportWizard(fProject);
		wizard.init(PlatformUI.getWorkbench(), fSelection);
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		notifyResult(wd.open() == Window.OK);
	}

}
