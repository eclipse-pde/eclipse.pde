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
package org.eclipse.pde.internal.ui.neweditor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public abstract class BaseNewDialog extends StatusDialog {
	private Button okButton;
	private ISiteModel siteModel;
	private ISiteObject siteObject;
	private IStatus errorStatus;
	private IStatus okStatus;

	public BaseNewDialog(
		Shell shell,
		ISiteModel siteModel,
		ISiteObject siteObject) {
		super(shell);
		this.siteModel = siteModel;
		this.siteObject = siteObject;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		dialogChanged();
	}

	public ISiteObject getSiteObject() {
		return siteObject;
	}

	public ISiteModel getSiteModel() {
		return siteModel;
	}
	
	protected IDialogSettings getDialogSettings(String sectionName) {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection(sectionName);
		if (section==null)
			section = master.addNewSection(sectionName);
		return section;
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 10;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		createEntries(container);

		if (siteObject != null)
			initializeFields();
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		hookListeners(listener);
		setTitle(getDialogTitle());
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, getHelpId());
		return container;
	}

	protected abstract String getDialogTitle();
	protected abstract String getHelpId();
	protected abstract void createEntries(Composite container);
	protected abstract void hookListeners(ModifyListener listener);
	protected abstract void dialogChanged();
	protected abstract String getEmptyErrorMessage();

	protected void initializeFields() {
		if (siteModel.isEditable() == false) {
			okButton.setEnabled(false);
		}
	}

	protected void setIfDefined(Text text, String value) {
		if (value != null)
			text.setText(value);
	}

	protected IStatus getEmptyErrorStatus() {
		if (errorStatus == null)
			errorStatus = createErrorStatus(getEmptyErrorMessage());
		return errorStatus;
	}

	protected IStatus getOKStatus() {
		if (okStatus == null)
			okStatus =
				new Status(
					IStatus.OK,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					"",
					null);
		return okStatus;
	}

	protected IStatus createErrorStatus(String message) {
		return new Status(
			IStatus.ERROR,
			PDEPlugin.getPluginId(),
			IStatus.OK,
			message,
			null);
	}

	protected void okPressed() {
		execute();
		super.okPressed();
	}

	protected abstract void execute();
}
