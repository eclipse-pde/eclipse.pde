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


import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class UnusedDependenciesAction extends Action {
	
	private IPluginModelBase model;

	public UnusedDependenciesAction(IPluginModelBase model) {
		this.model = model;
		setText(PDEPlugin.getResourceString("UnusedDependencies.action"));
	}
	
	public void run() {
		try {
			UnusedDependenciesOperation op = new UnusedDependenciesOperation(model);
			PDEPlugin.getWorkspace().run(op, (ISchedulingRule)null, IWorkspace.AVOID_UPDATE, new NullProgressMonitor());

			IPluginImport[] unused = op.getUnusedDependencies();
			if (unused.length == 0)
				MessageDialog.openInformation(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString("UnusedDependencies.title"),
					PDEPlugin.getResourceString("UnusedDependencies.notFound"));
			else if (model.isEditable()) {
				UnusedImportsDialog dialog =
					new UnusedImportsDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						model,
						unused);
				dialog.create();
				dialog.getShell().setText(
					PDEPlugin.getResourceString("UnusedDependencies.title"));
				dialog.open();
			} else {
				String lineSeparator = System.getProperty("line.separator");
				StringBuffer buffer =
					new StringBuffer(
						PDEPlugin.getResourceString("UnusedDependencies.found"));
				for (int i = 0; i < unused.length; i++) {
					buffer.append(lineSeparator + unused[i].getId());
				}
				MessageDialog.openInformation(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString("UnusedDependencies.title"),
					buffer.toString());
			}
		} catch (CoreException e) {
		}
	}
	
}
