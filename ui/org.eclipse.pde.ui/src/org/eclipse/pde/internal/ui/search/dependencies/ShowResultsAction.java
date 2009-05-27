/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.dialogs.ListDialog;

public class ShowResultsAction extends Action {

	private IPluginModelBase fModel;
	Object[] fUnusedImports;
	private boolean fReadOnly;

	public ShowResultsAction(IPluginModelBase model, Object[] unused, boolean readOnly) {
		fModel = model;
		fUnusedImports = unused;
		fReadOnly = readOnly;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fUnusedImports.length == 0) {
			MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.UnusedDependencies_title, PDEUIMessages.UnusedDependencies_notFound);
		} else {
			Dialog dialog;
			if (fReadOnly) {
				// Launched from Dependencies View, show information dialog
				dialog = getUnusedDependeciesInfoDialog();
			} else {
				dialog = new UnusedImportsDialog(PDEPlugin.getActiveWorkbenchShell(), fModel, fUnusedImports);
				dialog.create();
			}
			dialog.getShell().setText(PDEUIMessages.UnusedDependencies_title);
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

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		dialog.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		dialog.setInput(this);
		dialog.create();
		dialog.getTableViewer().setComparator(new UnusedImportsDialog.Comparator());
		return dialog;
	}
}
