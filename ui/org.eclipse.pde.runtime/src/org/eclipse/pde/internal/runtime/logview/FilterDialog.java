/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;


public class FilterDialog extends TrayDialog {
	private Button limit;
	private Text limitText;

	private Button okButton;
	private Button errorButton;
	private Button warningButton;
	private Button infoButton;
	private Button showAllButton;
	private IMemento memento;

	public FilterDialog(Shell parentShell, IMemento memento) {
		super(parentShell);
		this.memento = memento;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite)super.createDialogArea(parent);		
		createEventTypesGroup(container);
		createLimitSection(container);
		createSessionSection(container);
		
		Dialog.applyDialogFont(container);
		return container;
	}
	
	private void createEventTypesGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 275;
		group.setLayoutData(gd);
		group.setText(PDERuntimeMessages.LogView_FilterDialog_eventTypes); 
		
		infoButton = new Button(group, SWT.CHECK);
		infoButton.setText(PDERuntimeMessages.LogView_FilterDialog_information); 
		infoButton.setSelection(memento.getString(LogView.P_LOG_INFO).equals("true")); //$NON-NLS-1$
		
		warningButton = new Button(group, SWT.CHECK);
		warningButton.setText(PDERuntimeMessages.LogView_FilterDialog_warning); 
		warningButton.setSelection(memento.getString(LogView.P_LOG_WARNING).equals("true")); //$NON-NLS-1$
		
		errorButton = new Button(group, SWT.CHECK);
		errorButton.setText(PDERuntimeMessages.LogView_FilterDialog_error); 
		errorButton.setSelection(memento.getString(LogView.P_LOG_ERROR).equals("true"));		 //$NON-NLS-1$
	}
	
	private void createLimitSection(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		limit = new Button(comp, SWT.CHECK);
		limit.setText(PDERuntimeMessages.LogView_FilterDialog_limitTo); 
		limit.setSelection(memento.getString(LogView.P_USE_LIMIT).equals("true")); //$NON-NLS-1$
		limit.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			limitText.setEnabled(((Button)e.getSource()).getSelection());
		}});
		
		limitText = new Text(comp, SWT.BORDER);
		limitText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				try {
					if (okButton == null)
						return;
					Integer.parseInt(limitText.getText());
					okButton.setEnabled(true);
				} catch (NumberFormatException e1) {
					okButton.setEnabled(false);
				}
			}});
		limitText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		limitText.setText(memento.getString(LogView.P_LOG_LIMIT));
		limitText.setEnabled(limit.getSelection());

	}
	
	private void createSessionSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(container, SWT.NONE);
		label.setText(PDERuntimeMessages.LogView_FilterDialog_eventsLogged); 
		
		showAllButton = new Button(container, SWT.RADIO);
		showAllButton.setText(PDERuntimeMessages.LogView_FilterDialog_allSessions); 
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		showAllButton.setLayoutData(gd);
		
		Button button = new Button(container, SWT.RADIO);
		button.setText(PDERuntimeMessages.LogView_FilterDialog_recentSession); 
		gd = new GridData();
		gd.horizontalIndent = 20;
		button.setLayoutData(gd);
		
		if (memento.getString(LogView.P_SHOW_ALL_SESSIONS).equals("true")) { //$NON-NLS-1$
			showAllButton.setSelection(true);
		} else {
			button.setSelection(true);
		}
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			false);
	}
	
	protected void okPressed() {
		memento.putString(LogView.P_LOG_INFO, infoButton.getSelection() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(LogView.P_LOG_WARNING, warningButton.getSelection() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(LogView.P_LOG_ERROR, errorButton.getSelection() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(LogView.P_LOG_LIMIT, limitText.getText());
		memento.putString(LogView.P_USE_LIMIT, limit.getSelection() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(LogView.P_SHOW_ALL_SESSIONS, showAllButton.getSelection() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		super.okPressed();
	}

}
