/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.swt.events.*;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.wizards.StatusWizardPage;
import org.eclipse.jface.resource.JFaceResources;

public class PluginImportWizardFirstPage extends StatusWizardPage {

	private static final String SETTINGS_DROPLOCATION = "droplocation";
	private static final String SETTINGS_DOOTHER = "doimport";
	private static final String SETTINGS_DOIMPORT = "doimport";
	private static final String SETTINGS_DOEXTRACT = "doextract";
	private static final String KEY_TITLE = "ImportWizard.FirstPage.title";
	private static final String KEY_DESC = "ImportWizard.FirstPage.desc";
	private static final String KEY_RUNTIME_LOCATION =
		"ImportWizard.FirstPage.runtimeLocation";
	private static final String KEY_OTHER_LOCATION =
		"ImportWizard.FirstPage.otherLocation";
	private static final String KEY_RUNTIME_DESC =
		"ImportWizard.FirstPage.runtimeDesc";
	private static final String KEY_OTHER_DESC =
		"ImportWizard.FirstPage.otherDesc";
	private static final String KEY_OTHER_FOLDER =
		"ImportWizard.FirstPage.otherFolder";
	private static final String KEY_BROWSE = "ImportWizard.FirstPage.browse";
	private static final String KEY_IMPORT_CHECK =
		"ImportWizard.FirstPage.importCheck";
	private static final String KEY_EXTRACT_CHECK =
		"ImportWizard.FirstPage.extractCheck";
	private static final String KEY_FOLDER_TITLE = "ImportWizard.messages.folder.title";
	private static final String KEY_FOLDER_MESSAGE = "ImportWizard.messages.folder.message";
	private static final String KEY_LOCATION_MISSING = "ImportWizard.errors.locationMissing";
	private static final String KEY_BUILD_INVALID = "ImportWizard.errors.buildFolderInvalid";
	private static final String KEY_BUILD_MISSING = "ImportWizard.errors.buildFolderMissing";

	private Label otherLocationLabel;
	private Button runtimeLocationButton;
	private Button otherLocationButton;
	private Button browseButton;
	private Combo dropLocation;
	private Button doImportCheck;
	private Button doExtractCheck;

	private IStatus dropLocationStatus;

	public PluginImportWizardFirstPage() {
		super("PluginImportWizardPage", true);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		dropLocationStatus = createStatus(IStatus.OK, "");
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		//layout.minimumWidth= convertWidthInCharsToPixels(60);
		composite.setLayout(layout);

		runtimeLocationButton = new Button(composite, SWT.RADIO);
		fillHorizontal(runtimeLocationButton, 3);
		runtimeLocationButton.setText(
			PDEPlugin.getResourceString(KEY_RUNTIME_LOCATION));

		Label label = new Label(composite, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString(KEY_RUNTIME_DESC));
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		gd.widthHint = convertWidthInCharsToPixels(85);
		label.setLayoutData(gd);

		otherLocationButton = new Button(composite, SWT.RADIO);
		fillHorizontal(otherLocationButton, 3);
		otherLocationButton.setText(PDEPlugin.getResourceString(KEY_OTHER_LOCATION));

		label = new Label(composite, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString(KEY_OTHER_DESC));
		gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(85);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		otherLocationLabel = new Label(composite, SWT.NULL);
		otherLocationLabel.setText(PDEPlugin.getResourceString(KEY_OTHER_FOLDER));

		dropLocation = new Combo(composite, SWT.DROP_DOWN);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		dropLocation.setLayoutData(gd);

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString(KEY_BROWSE));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					dropLocation.setText(chosen.toOSString());
			}
		});

		label = new Label(composite, SWT.NULL);
		fillHorizontal(label, 3);

		doImportCheck = new Button(composite, SWT.CHECK);
		doImportCheck.setText(PDEPlugin.getResourceString(KEY_IMPORT_CHECK));
		fillHorizontal(doImportCheck, 3);

		doExtractCheck = new Button(composite, SWT.CHECK);
		doExtractCheck.setText(PDEPlugin.getResourceString(KEY_EXTRACT_CHECK));
		fillHorizontal(doExtractCheck, 3);
		initializeFields(getDialogSettings());

		runtimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (runtimeLocationButton.getSelection()) {
					setOtherEnabled(false);
					validateDropLocation();
					updateStatus(dropLocationStatus);
				}
			}
		});
		otherLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (otherLocationButton.getSelection()) {
					setOtherEnabled(true);
					validateDropLocation();
					updateStatus(dropLocationStatus);
				}
			}
		});
		doImportCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			}
		});
		doExtractCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (doExtractCheck.getSelection()) {
					if (!doImportCheck.getSelection()) {
						doImportCheck.setSelection(true);
					}
				}
				doImportCheck.setEnabled(!doExtractCheck.getSelection());
			}
		});
		dropLocation.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				validateDropLocation();
				updateStatus(dropLocationStatus);
			}
		});
		setControl(composite);
	}
	
	private GridData fillHorizontal(Control control, int span) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		control.setLayoutData(gd);
		return gd;
	}

	private void initializeFields(IDialogSettings initialSettings) {
		String[] dropItems = new String[0];
		boolean doImport = false;
		boolean doExtract = false;
		boolean doOther = false;

		if (initialSettings != null) {
			doOther = initialSettings.getBoolean(SETTINGS_DOOTHER);
			doImport = initialSettings.getBoolean(SETTINGS_DOIMPORT);
			doExtract = initialSettings.getBoolean(SETTINGS_DOEXTRACT);

			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr = initialSettings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			dropItems = (String[]) items.toArray(new String[items.size()]);
		}
		dropLocation.setItems(dropItems);
		runtimeLocationButton.setSelection(!doOther);
		otherLocationButton.setSelection(doOther);
		setOtherEnabled(doOther);
		if (doOther)
			dropLocation.select(0);
		doImportCheck.setSelection(doImport);
		doExtractCheck.setSelection(doExtract);
		validateDropLocation();
		updateStatus(dropLocationStatus);
	}

	private void setOtherEnabled(boolean enabled) {
		otherLocationLabel.setEnabled(enabled);
		dropLocation.setEnabled(enabled);
		browseButton.setEnabled(enabled);
	}

	public void storeSettings(boolean finishPressed) {
		IDialogSettings settings = getDialogSettings();
		boolean other = otherLocationButton.getSelection();
		if (finishPressed || dropLocation.getText().length() > 0 && other) {
			settings.put(SETTINGS_DROPLOCATION + String.valueOf(0), dropLocation.getText());
			String[] items = dropLocation.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(SETTINGS_DROPLOCATION + String.valueOf(i + 1), items[i]);
			}
		}
		if (finishPressed) {
			settings.put(SETTINGS_DOOTHER, other);
			settings.put(SETTINGS_DOIMPORT, doImportCheck.getSelection());
			settings.put(SETTINGS_DOEXTRACT, doExtractCheck.getSelection());
		}
	}

	/**
	 * Browses for a drop location.
	 */
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(otherLocationLabel.getText());
		dialog.setText(PDEPlugin.getResourceString(KEY_FOLDER_TITLE));
		dialog.setMessage(PDEPlugin.getResourceString(KEY_FOLDER_MESSAGE));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}
	


	private void validateDropLocation() {
		if (isOtherLocation()) {
			IPath curr = getDropLocation();
			if (curr.segmentCount() == 0) {
				dropLocationStatus =
					createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_LOCATION_MISSING));
				return;
			}
			if (!Path.ROOT.isValidPath(dropLocation.getText())) {
				dropLocationStatus =
					createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_BUILD_INVALID));
				return;
			}

			File file = curr.toFile();
			if (!file.isDirectory()) {
				dropLocationStatus =
					createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_BUILD_MISSING));
				return;
			}
		}
		dropLocationStatus = createStatus(IStatus.OK, "");
	}

	/**
	 * Returns the drop location.
	 */
	public IPath getDropLocation() {
		return new Path(dropLocation.getText());
	}

	public boolean isOtherLocation() {
		return otherLocationButton.getSelection();
	}

	/**
	 * Returns the drop location.
	 */
	public boolean doImportToWorkspace() {
		return doImportCheck.getSelection();
	}

	/**
	 * Returns the drop location.
	 */
	public boolean doExtractPluginSource() {
		return doExtractCheck.getSelection();
	}

}