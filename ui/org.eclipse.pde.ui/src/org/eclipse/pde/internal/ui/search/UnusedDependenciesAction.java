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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.PlatformUI;

public class UnusedDependenciesAction extends Action {

	private IPluginModelBase model;

	public UnusedDependenciesAction(IPluginModelBase model) {
		this.model = model;
		setText(PDEPlugin.getResourceString("UnusedDependencies.action")); //$NON-NLS-1$
	}

	public void run() {
		UnusedDependenciesOperation op = new UnusedDependenciesOperation(model);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (Exception e) {
		}

		IPluginImport[] unused = op.getUnusedDependencies();
		if (unused.length == 0)
			MessageDialog.openInformation(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString("UnusedDependencies.title"), //$NON-NLS-1$
				PDEPlugin.getResourceString("UnusedDependencies.notFound")); //$NON-NLS-1$
		else if (model.isEditable()) {
			UnusedImportsDialog dialog =
				new UnusedImportsDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					model,
					unused);
			dialog.create();
			dialog.getShell().setText(
				PDEPlugin.getResourceString("UnusedDependencies.title")); //$NON-NLS-1$
			dialog.open();
		} else {
			String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
			StringBuffer buffer =
				new StringBuffer(PDEPlugin.getResourceString("UnusedDependencies.found")); //$NON-NLS-1$
			for (int i = 0; i < unused.length; i++) {
				buffer.append(lineSeparator + unused[i].getId());
			}
			MessageDialog.openInformation(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString("UnusedDependencies.title"), //$NON-NLS-1$
				buffer.toString());
		}
	}

}
