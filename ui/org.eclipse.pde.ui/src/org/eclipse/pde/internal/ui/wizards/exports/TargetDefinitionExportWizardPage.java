/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Ian Bull <irbull@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class TargetDefinitionExportWizardPage extends WizardPage {

	private static final String PAGE_ID = "org.eclipse.pde.target.exportPage"; //$NON-NLS-1$
	private Button browseButton = null;
	private Text destDirText = null;
	private Button clearDestinationDirCheck = null;

	protected TargetDefinitionExportWizardPage() {
		super(PAGE_ID);
		setPageComplete(false);
		setTitle(PDEUIMessages.ExportActiveTargetDefinition);
		// TODO setImage(...)
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		createExportDirectoryControl(container);

		//TODO initSettings();

		Dialog.applyDialogFont(container);
		setControl(container);
		setPageComplete(validate());
	}

	private void createExportDirectoryControl(Composite parent) {
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		new Label(parent, SWT.NONE).setText(PDEUIMessages.ExportTargetCurrentTarget);
		Label l = new Label(parent, SWT.NONE);

		try {
			// TODO this is a bit dirty
			TargetDefinition definition = ((TargetDefinition) TargetPlatformService.getDefault().getWorkspaceTargetHandle().getTargetDefinition());
			l.setText(definition.getName());
		} catch (CoreException e) {
			// TODO log something?
		}

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);
		new Label(parent, SWT.NONE).setText(PDEUIMessages.ExportTargetChooseFolder);

		destDirText = new Text(parent, SWT.BORDER);
		destDirText.setEditable(false);
		destDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		destDirText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged();
			}
		});
		browseButton = new Button(parent, SWT.PUSH);
		browseButton.setText(PDEUIMessages.ExportTargetBrowse);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(PDEUIMessages.ExportTargetSelectDestination);
				dialog.setMessage(PDEUIMessages.ExportTargetSpecifyDestination);
				String dir = destDirText.getText();
				dialog.setFilterPath(dir);
				dir = dialog.open();
				if (dir == null || dir.equals("")) { //$NON-NLS-1$
					return;
				}
				destDirText.setText(dir);
				controlChanged();
			}
		});

		clearDestinationDirCheck = new Button(parent, SWT.CHECK);
		clearDestinationDirCheck.setText(PDEUIMessages.ExportTargetClearDestination);
		gd = new GridData();
		gd.horizontalSpan = 2;
		clearDestinationDirCheck.setLayoutData(gd);
	}

	public String getDestinationDirectory() {
		return destDirText.getText();
	}

	public boolean isClearDestinationDirectory() {
		return clearDestinationDirCheck.getSelection();
	}

	public void controlChanged() {
		setPageComplete(validate());
	}

	protected boolean validate() {
		setMessage(null);

		if (destDirText.getText().equals("")) { //$NON-NLS-1$
			setMessage(PDEUIMessages.ExportTargetError_ChooseDestination, IStatus.WARNING);
			return false;
		}

		return true;
	}

}
