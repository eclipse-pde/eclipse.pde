/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;
import org.eclipse.ui.*;

public class ProductExportAction extends Action {

	private IStructuredSelection fSelection;
	
	public ProductExportAction(PDEFormEditor editor) {
		IResource resource = null;
		if (editor != null)
			resource = ((IModel) editor.getAggregateModel()).getUnderlyingResource();
		fSelection = resource != null ? new StructuredSelection(resource) : new StructuredSelection();
	}
	
	public ProductExportAction(IStructuredSelection selection) {
		fSelection = selection;
	}
	
	public void run() {
		ProductExportWizard wizard = new ProductExportWizard();
		wizard.init(PlatformUI.getWorkbench(), fSelection);
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		notifyResult(wd.open() == WizardDialog.OK);
	}
	
}
