/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ProductDestinationGroup extends AbstractExportTab {

	protected static final String S_EXPORT_DIRECTORY = "exportDirectory"; //$NON-NLS-1$
	protected static final String S_DESTINATION = "destination"; //$NON-NLS-1$
	protected static final String S_ZIP_FILENAME = "zipFileName"; //$NON-NLS-1$

	protected static String ZIP_EXTENSION = ".zip"; //$NON-NLS-1$
	protected static String WAR_EXTENSION = ".war"; //$NON-NLS-1$

	protected Button fArchiveFileButton;
	protected Combo fArchiveCombo;
	protected Button fBrowseFile;
	protected Button fDirectoryButton;
	protected Combo fDirectoryCombo;
	protected Button fBrowseDirectory;

	public ProductDestinationGroup(AbstractExportWizardPage page) {
		super(page);
	}

	public Control createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ExportWizard_destination);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fDirectoryButton = new Button(group, SWT.RADIO);
		fDirectoryButton.setText(PDEUIMessages.ExportWizard_directory);

		fDirectoryCombo = new Combo(group, SWT.BORDER);
		fDirectoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseDirectory = new Button(group, SWT.PUSH);
		fBrowseDirectory.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseDirectory);

		fArchiveFileButton = new Button(group, SWT.RADIO);
		fArchiveFileButton.setText(PDEUIMessages.ExportWizard_archive);

		fArchiveCombo = new Combo(group, SWT.BORDER);
		fArchiveCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseFile = new Button(group, SWT.PUSH);
		fBrowseFile.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseFile);

		return group;
	}

	protected void initialize(IDialogSettings settings) {
		initialize(settings, null);
	}

	protected void initialize(IDialogSettings settings, IFile file) {
		try {
			String toDirectory = (file != null) ? file.getPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_DIR) : null;
			if (toDirectory == null)
				toDirectory = settings.get(S_EXPORT_DIRECTORY);
			boolean useDirectory = toDirectory == null || "true".equals(toDirectory); //$NON-NLS-1$
			fDirectoryButton.setSelection(useDirectory);
			fArchiveFileButton.setSelection(!useDirectory);
			toggleDestinationGroup(useDirectory);

			initializeCombo(settings, S_DESTINATION, fDirectoryCombo);
			initializeCombo(settings, S_ZIP_FILENAME, fArchiveCombo);

			updateDestination(file);
			hookListeners();
		} catch (CoreException e) {
		}
	}

	protected void initializeCombo(IDialogSettings settings, String key, Combo combo) {
		super.initializeCombo(settings, key, combo);
		if (!isValidLocation(combo.getText().trim())) // If default value is invalid, make it blank
			combo.setText(""); //$NON-NLS-1$
	}

	protected void updateDestination(IFile file) {
		try {
			if (file == null)
				return;
			String toDirectory = file.getPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_DIR);
			if (toDirectory == null)
				return;
			boolean useDirectory = "true".equals(toDirectory); //$NON-NLS-1$
			fArchiveFileButton.setSelection(!useDirectory);
			fDirectoryButton.setSelection(useDirectory);
			toggleDestinationGroup(useDirectory);

			Combo combo = useDirectory ? fDirectoryCombo : fArchiveCombo;
			String destination = file.getPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_LOCATION);
			if (destination != null) {
				if (combo.indexOf(destination) == -1)
					combo.add(destination, 0);
				combo.setText(destination);
			}
		} catch (CoreException e) {
		}
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
				chooseFile(fArchiveCombo, new String[] {"*" + ZIP_EXTENSION, "*" + WAR_EXTENSION}); //$NON-NLS-1$ //$NON-NLS-2$
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.AbstractExportTab#saveSettings(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	protected void saveSettings(IDialogSettings settings) {
		settings.put(S_EXPORT_DIRECTORY, fDirectoryButton.getSelection());
		saveCombo(settings, S_DESTINATION, fDirectoryCombo);
		saveCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
		IFile file = ((ProductExportWizardPage) fPage).getProductFile();
		try {
			if (file != null && file.exists()) {
				file.setPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_DIR, Boolean.toString(doExportToDirectory()));
				file.setPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_LOCATION, doExportToDirectory() ? fDirectoryCombo.getText().trim() : fArchiveCombo.getText().trim());
			}
		} catch (CoreException e) {
		}
	}

	protected boolean doExportToDirectory() {
		return fDirectoryButton.getSelection();
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

	protected String validate() {
		if (fArchiveFileButton.getSelection()) {
			if (fArchiveCombo.getText().trim().length() == 0)
				return PDEUIMessages.ExportWizard_status_nofile;
			else if (!isValidLocation(fArchiveCombo.getText().trim()))
				return PDEUIMessages.ExportWizard_status_invaliddirectory;
		}
		if (fDirectoryButton.getSelection()) {
			if (fDirectoryCombo.getText().trim().length() == 0)
				return PDEUIMessages.ExportWizard_status_nodirectory;
			else if (!isValidLocation(fDirectoryCombo.getText().trim()))
				return PDEUIMessages.ExportWizard_status_invaliddirectory;
		}
		return null;
	}

	protected String getFileName() {
		if (fArchiveFileButton.getSelection()) {
			String path = fArchiveCombo.getText();
			if (path != null && path.length() > 0) {
				String fileName = new Path(path).lastSegment();
				if (!fileName.endsWith(ZIP_EXTENSION) && !fileName.endsWith(WAR_EXTENSION)) {
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

}
