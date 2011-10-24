/*******************************************************************************
 * Copyright (c) 2010, 2011 EclipseSource Corporation and others.
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

import org.eclipse.pde.core.target.ITargetDefinition;

import java.io.File;
import java.io.IOException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class TargetDefinitionExportWizardPage extends WizardPage {

	private static final String PAGE_ID = "org.eclipse.pde.target.exportPage"; //$NON-NLS-1$
	private Button fBrowseButton = null;
	private Combo fDestinationCombo = null;
	private Button fClearDestinationButton = null;
	private ITargetDefinition fTarget = null;

	/**
	 * Dialog settings key for the most recent location
	 */
	private static final String SETTINGS_LOCATION_1 = "location1"; //$NON-NLS-1$

	/**
	 * Dialog settings key for the second most recent location
	 */
	private static final String SETTINGS_LOCATION_2 = "location2"; //$NON-NLS-1$

	/**
	 * Dialog settings key for the third most recent location 
	 */
	private static final String SETTINGS_LOCATION_3 = "location3"; //$NON-NLS-1$

	/**
	 * Dialog settings key for whether the clear the destination directory
	 */
	private static final String SETTINGS_CLEAR = "clear"; //$NON-NLS-1$

	protected TargetDefinitionExportWizardPage(ITargetDefinition target) {
		super(PAGE_ID);
		fTarget = target;
		setPageComplete(false);
		setTitle(PDEUIMessages.ExportActiveTargetDefinition);
		setMessage(PDEUIMessages.ExportActiveTargetDefinition_message);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		createExportDirectoryControl(container);
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.TARGET_EXPORT_WIZARD);
	}

	private void createExportDirectoryControl(Composite parent) {
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		new Label(parent, SWT.NONE).setText(PDEUIMessages.ExportTargetCurrentTarget);
		Label l = new Label(parent, SWT.NONE);
		l.setText(fTarget.getName());

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);
		new Label(parent, SWT.NONE).setText(PDEUIMessages.ExportTargetChooseFolder);

		fDestinationCombo = SWTFactory.createCombo(parent, SWT.BORDER, 1, null);
		fDestinationCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged();
			}
		});

		fBrowseButton = new Button(parent, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.ExportTargetBrowse);
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(PDEUIMessages.ExportTargetSelectDestination);
				dialog.setMessage(PDEUIMessages.ExportTargetSpecifyDestination);
				String dir = fDestinationCombo.getText();
				dialog.setFilterPath(dir);
				dir = dialog.open();
				if (dir == null || dir.equals("")) { //$NON-NLS-1$
					return;
				}
				fDestinationCombo.setText(dir);
				controlChanged();
			}
		});

		fClearDestinationButton = new Button(parent, SWT.CHECK);
		fClearDestinationButton.setText(PDEUIMessages.ExportTargetClearDestination);
		gd = new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 15;
		fClearDestinationButton.setLayoutData(gd);

		initSettings();
	}

	public String getDestinationDirectory() {
		return fDestinationCombo.getText();
	}

	public boolean isClearDestinationDirectory() {
		return fClearDestinationButton.getSelection();
	}

	public void controlChanged() {
		setPageComplete(validate());
	}

	protected boolean validate() {
		setMessage(null);

		if (fDestinationCombo.getText().equals("")) { //$NON-NLS-1$
			setErrorMessage(PDEUIMessages.ExportTargetError_ChooseDestination);
			return false;
		} else if (!isValidLocation(fDestinationCombo.getText().trim())) {
			setErrorMessage(PDEUIMessages.ExportTargetError_validPath);
			return false;
		}
		setErrorMessage(null);

		return true;
	}

	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			settings.put(SETTINGS_CLEAR, fClearDestinationButton.getSelection());

			String newLocation = fDestinationCombo.getText().trim();
			if (newLocation.charAt(newLocation.length() - 1) == File.separatorChar) {
				newLocation = newLocation.substring(0, newLocation.length() - 1);
			}
			String[] items = fDestinationCombo.getItems();
			for (int i = 0; i < items.length; i++) {
				if (items[i].equals(newLocation)) {
					// Already have this location stored
					return;
				}
			}
			String location = settings.get(SETTINGS_LOCATION_2);
			if (location != null) {
				settings.put(SETTINGS_LOCATION_3, location);
			}
			location = settings.get(SETTINGS_LOCATION_1);
			if (location != null) {
				settings.put(SETTINGS_LOCATION_2, location);
			}
			settings.put(SETTINGS_LOCATION_1, newLocation);
		}
	}

	private void initSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			fClearDestinationButton.setSelection(settings.getBoolean(SETTINGS_CLEAR));

			String location = settings.get(SETTINGS_LOCATION_1);
			if (location != null) {
				fDestinationCombo.add(location);
			}
			location = settings.get(SETTINGS_LOCATION_2);
			if (location != null) {
				fDestinationCombo.add(location);
			}
			location = settings.get(SETTINGS_LOCATION_3);
			if (location != null) {
				fDestinationCombo.add(location);
			}

			if (fDestinationCombo.getItemCount() > 0) {
				fDestinationCombo.setText(fDestinationCombo.getItem(0));
			}
		}
	}

	protected boolean isValidLocation(String location) {
		try {
			String destinationPath = new File(location).getCanonicalPath();
			if (destinationPath == null || destinationPath.length() == 0)
				return false;
		} catch (IOException e) {
			return false;
		}

		return true;
	}

}
