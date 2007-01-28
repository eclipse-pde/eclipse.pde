/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.update;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddSiteDialog extends StatusDialog {
	
	private Text fPath;
//	private String fSiteLocation;
//	private String fTargetLocation;
	private String fLocation;
	private IStatus fOkStatus;
	private IStatus fErrorStatus;

	public AddSiteDialog(Shell parent, String location) {
		super(parent);
		getEmptyErrorStatus();
		//fLocation = location;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		layout.marginHeight = layout.marginWidth = 10;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		
		createEntry(container);

		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		fPath.addModifyListener(listener);
		setTitle(PDEUIMessages.LocationDialog_title); 
		Dialog.applyDialogFont(container);
		
		dialogChanged();
		
		return container;
	}
	
	protected void createEntry(Composite container) {
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.LocationDialog_path); 
		label.setLayoutData(new GridData());
		
		fPath = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		fPath.setLayoutData(gd);
		
		if (fLocation != null) {
			fPath.setText(fLocation);
		}
		
		label = new Label(container, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		Button fs = new Button(container, SWT.PUSH);
		fs.setText(PDEUIMessages.LocationDialog_fileSystem);
		fs.setLayoutData(new GridData());
		fs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseFileSystem();
			}
		});
		SWTUtil.setButtonDimensionHint(fs);
		
		Button var = new Button(container, SWT.PUSH);
		var.setText(PDEUIMessages.LocationDialog_variables);
		var.setLayoutData(new GridData());
		var.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleInsertVariable();
			}
		});
		SWTUtil.setButtonDimensionHint(var);
	}
	
	private IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK,
				message, null);
	}
	
	private void dialogChanged() {
		IStatus status = null;
//		if (fPath.getText().length() == 0)
//			status = getEmptyErrorStatus();
//		else {
//			if (hasPath(fPath.getText()))
//				status = createErrorStatus(PDEUIMessages.LocationDialog_locationExists); 
//		}
		if (status == null)
			status = getOKStatus();
		updateStatus(status);
	}
	
	private IStatus getOKStatus() {
		if (fOkStatus == null)
			fOkStatus = new Status(IStatus.OK, PDEPlugin.getPluginId(),
					IStatus.OK, "", //$NON-NLS-1$
					null);
		return fOkStatus;
	}
	
	private IStatus getEmptyErrorStatus() {
		if (fErrorStatus == null)
			fErrorStatus = createErrorStatus(PDEUIMessages.LocationDialog_emptyPath); 
		return fErrorStatus;
	}
		
	protected void handleBrowseFileSystem() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(fPath.getText());
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection); 
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose); 
		String result = dialog.open();
		if (result != null) {
			fPath.setText(result);
		}
	}
	
	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = 
					new StringVariableSelectionDialog(getShell());
		if (dialog.open() == Window.OK) {
			fPath.insert(dialog.getVariableExpression());
		}
	}
	
	protected void okPressed() {
		//fLocation = fPath.getText();
		super.okPressed();
	}
	
	public String getLocation() {
		return fLocation;
	}
}
