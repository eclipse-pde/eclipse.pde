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
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.ListDialog;

public class ShowResultsAction extends Action {
	
	IPluginImport[] fUnusedImports;
	private boolean fReadOnly;

	public ShowResultsAction(IPluginImport[] unused, boolean readOnly) {
		fUnusedImports = unused;
		fReadOnly = readOnly;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fUnusedImports.length == 0) {
			MessageDialog.openInformation(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString("UnusedDependencies.title"), //$NON-NLS-1$
				PDEPlugin.getResourceString("UnusedDependencies.notFound")); //$NON-NLS-1$
		} else {
			Dialog dialog;
			if (fReadOnly) {
				// Launched from Dependencies View, show information dialog
				dialog = getUnusedDependeciesInfoDialog();
			} else {
				IPluginModelBase model = (IPluginModelBase)fUnusedImports[0].getModel();
				dialog = new UnusedImportsDialog(PDEPlugin
						.getActiveWorkbenchShell(), model, fUnusedImports);
			}
			dialog.create();
			dialog.getShell().setText(
				PDEPlugin.getResourceString("UnusedDependencies.title")); //$NON-NLS-1$
			dialog.open();
		} 
	}

	/**
	 * @return Dialog
	 */
	private Dialog getUnusedDependeciesInfoDialog() {
		ListDialog dialog = new ListDialog(PDEPlugin.getActiveWorkbenchShell());
		dialog.setAddCancelButton(false);
		dialog.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return fUnusedImports;
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}
		});
		dialog.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		dialog.setInput(this);
		return dialog;
	}
}


