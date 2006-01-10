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
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;

public class ExportDestinationTab extends AbstractExportTab {

	protected static final String S_EXPORT_DIRECTORY = "exportDirectory"; //$NON-NLS-1$
	protected static final String S_DESTINATION = "destination"; //$NON-NLS-1$
	protected static final String S_ZIP_FILENAME = "zipFileName"; //$NON-NLS-1$

	protected static String ZIP_EXTENSION = ".zip"; //$NON-NLS-1$

	protected Button fArchiveFileButton;
	protected Combo fArchiveCombo;
	protected Button fBrowseFile;
	protected Button fDirectoryButton;
	protected Combo fDirectoryCombo;
	protected Button fBrowseDirectory;

	public ExportDestinationTab(AbstractExportWizardPage page) {
		super(page);
	}

	public Control createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fDirectoryButton = new Button(composite, SWT.RADIO);
		fDirectoryButton.setText(PDEUIMessages.ExportWizard_directory);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fDirectoryButton.setLayoutData(gd);
		
		fDirectoryCombo = new Combo(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 15;
		fDirectoryCombo.setLayoutData(gd);

		fBrowseDirectory = new Button(composite, SWT.PUSH);
		fBrowseDirectory.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseDirectory);

		fArchiveFileButton = new Button(composite, SWT.RADIO);
		fArchiveFileButton.setText(PDEUIMessages.ExportWizard_archive);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fArchiveFileButton.setLayoutData(gd);

		fArchiveCombo = new Combo(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 15;
		fArchiveCombo.setLayoutData(gd);

		fBrowseFile = new Button(composite, SWT.PUSH);
		fBrowseFile.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseFile);

		return composite;
	}

	protected void initialize(IDialogSettings settings) {
		String toDirectory = settings.get(S_EXPORT_DIRECTORY);
		boolean useDirectory = toDirectory == null || "true".equals(toDirectory); //$NON-NLS-1$
		fDirectoryButton.setSelection(useDirectory);
		fArchiveFileButton.setSelection(!useDirectory);
		toggleDestinationGroup(useDirectory);
		initializeCombo(settings, S_DESTINATION, fDirectoryCombo);
		initializeCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
		hookListeners();
	}

	protected void toggleDestinationGroup(boolean useDirectory) {
		fArchiveCombo.setEnabled(!useDirectory);
		fBrowseFile.setEnabled(!useDirectory);
		fDirectoryCombo.setEnabled(useDirectory);
		fBrowseDirectory.setEnabled(useDirectory);
	}

	protected void hookListeners() {
		fArchiveFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleDestinationGroup(!fArchiveFileButton.getSelection());
				fPage.pageChanged();
			}
		});

		fBrowseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile(fArchiveCombo, "*" + ZIP_EXTENSION); //$NON-NLS-1$
			}
		});

		fArchiveCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fPage.pageChanged();
			}
		});

		fDirectoryCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fPage.pageChanged();
			}
		});

		fBrowseDirectory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseDestination();
			}
		});
	}

	private void chooseDestination() {
		DirectoryDialog dialog = new DirectoryDialog(fPage.getShell(), SWT.SAVE);
		String path = fDirectoryCombo.getText();
		if (path.trim().length() == 0)
			path = PDEPlugin.getWorkspace().getRoot().getLocation().toString();
		dialog.setFilterPath(path);
		dialog.setText(PDEUIMessages.ExportWizard_dialog_title);
		dialog.setMessage(PDEUIMessages.ExportWizard_dialog_message);
		String res = dialog.open();
		if (res != null) {
			if (fDirectoryCombo.indexOf(res) == -1)
				fDirectoryCombo.add(res, 0);
			fDirectoryCombo.setText(res);
		}
	}
	
	protected void saveSettings(IDialogSettings settings) {
		settings.put(S_EXPORT_DIRECTORY, fDirectoryButton.getSelection());		
		saveCombo(settings, S_DESTINATION, fDirectoryCombo);
		saveCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
	}

	protected String validate() {
		if (fArchiveFileButton.getSelection()
				&& fArchiveCombo.getText().trim().length() == 0)
			return PDEUIMessages.ExportWizard_status_nofile;
		if (fDirectoryButton.getSelection()
				&& fDirectoryCombo.getText().trim().length() == 0)
			return PDEUIMessages.ExportWizard_status_nodirectory;
		return null;
	}

	protected String getFileName() {
		if (fArchiveFileButton.getSelection()) {
			String path = fArchiveCombo.getText();
			if (path != null && path.length() > 0) {
				String fileName = new Path(path).lastSegment();
				if (!fileName.endsWith(ZIP_EXTENSION)) {
					fileName += ZIP_EXTENSION;
				}
				return fileName;
			}
		}
		return null;
	}

	protected String getDestination() {
		if (fArchiveFileButton.getSelection()) {
			String path = fArchiveCombo.getText();
			if (path.length() > 0) {
				path = new Path(path).removeLastSegments(1).toOSString();
				return new File(path).getAbsolutePath();
			}
			return ""; //$NON-NLS-1$
		}

		File dir = new File(fDirectoryCombo.getText().trim());
		return dir.getAbsolutePath();
	}

	protected boolean doExportToDirectory() {
		return fDirectoryButton.getSelection();
	}

}
