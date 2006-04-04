/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


public class PluginStatusDialog extends TrayDialog {
	

	private PluginValidationOperation fOperation;

	public PluginStatusDialog(Shell parentShell, PluginValidationOperation op) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fOperation = op;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 300;
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.PluginStatusDialog_label); 
		
		TreeViewer treeViewer = new TreeViewer(container);
		treeViewer.setContentProvider(fOperation.getContentProvider());
		treeViewer.setLabelProvider(fOperation.getLabelProvider());
		treeViewer.setInput(fOperation.getState());
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		getShell().setText(PDEUIMessages.PluginStatusDialog_pluginValidation); 
		Dialog.applyDialogFont(container);
		return container;
	}

}
