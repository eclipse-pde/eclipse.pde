/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.events.*;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.wizards.StatusWizardPage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.pde.internal.ui.preferences.TargetEnvironmentPreferencePage;
import org.eclipse.pde.internal.ui.TargetPlatform;

public class PluginImportWizardFirstPage
	extends StatusWizardPage {

	private static final String SETTINGS_DROPLOCATION = "droplocation";
	private static final String SETTINGS_DOOTHER = "doother";
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
	private static final String KEY_OTHER_DESC = "ImportWizard.FirstPage.otherDesc";
	private static final String KEY_OTHER_FOLDER =
		"ImportWizard.FirstPage.otherFolder";
	private static final String KEY_BROWSE = "ImportWizard.FirstPage.browse";
	private static final String KEY_IMPORT_CHECK =
		"ImportWizard.FirstPage.importCheck";
	private static final String KEY_EXTRACT_CHECK =
		"ImportWizard.FirstPage.extractCheck";
	private static final String KEY_TARGET_DESC = "ImportWizard.FirstPage.targetDesc";
	private static final String KEY_FOLDER_TITLE =
		"ImportWizard.messages.folder.title";
	private static final String KEY_FOLDER_MESSAGE =
		"ImportWizard.messages.folder.message";
	private static final String KEY_LOCATION_MISSING =
		"ImportWizard.errors.locationMissing";
	private static final String KEY_BUILD_INVALID =
		"ImportWizard.errors.buildFolderInvalid";
	private static final String KEY_BUILD_MISSING =
		"ImportWizard.errors.buildFolderMissing";

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

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			IStatus running = PDEPlugin.getDefault().getCurrentLaunchStatus(null);
			if (running != null)
				updateStatus(running);
		} 
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Label label;
		GridData gd;
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		runtimeLocationButton = new Button(composite, SWT.RADIO);
		fillHorizontal(runtimeLocationButton, 3, false);
		runtimeLocationButton.setText(
			PDEPlugin.getResourceString(KEY_RUNTIME_LOCATION));
			
		int wizardClientWidth = parent.getSize().x - 2 * layout.marginWidth;

		createMultiLineLabel(composite, wizardClientWidth, PDEPlugin.getResourceString(KEY_RUNTIME_DESC), 3);

		otherLocationButton = new Button(composite, SWT.RADIO);
		fillHorizontal(otherLocationButton, 3, false);
		otherLocationButton.setText(PDEPlugin.getResourceString(KEY_OTHER_LOCATION));
		createMultiLineLabel(composite, wizardClientWidth, PDEPlugin.getResourceString(KEY_OTHER_DESC), 3);

		otherLocationLabel = new Label(composite, SWT.NULL);
		otherLocationLabel.setText(PDEPlugin.getResourceString(KEY_OTHER_FOLDER));

		dropLocation = new Combo(composite, SWT.DROP_DOWN);
		fillHorizontal(dropLocation, 1, true);

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString(KEY_BROWSE));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					dropLocation.setText(chosen.toOSString());
			}
		});

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = fillHorizontal(label, 3, false);
		gd.heightHint = 20;
		
		doImportCheck = new Button(composite, SWT.CHECK);
		doImportCheck.setText(PDEPlugin.getResourceString(KEY_IMPORT_CHECK));
		fillHorizontal(doImportCheck, 3, false);

		doExtractCheck = new Button(composite, SWT.CHECK);
		doExtractCheck.setText(PDEPlugin.getResourceString(KEY_EXTRACT_CHECK));
		fillHorizontal(doExtractCheck, 3, false);
		
		createTargetEnvironmentLabels(composite, wizardClientWidth, 3);
		
		initializeFields(getDialogSettings());
		hookListeners();

		setControl(composite);
	}
	
	private Label createMultiLineLabel(Composite composite, int parentWidth, String text, int span) {
		Label label = new Label(composite, SWT.WRAP);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.widthHint = parentWidth;
		label.setLayoutData(gd);
		return label;
	}
	
	private void createTargetEnvironmentLabels(Composite composite, int width, int span) {
		Label label = new Label(composite, SWT.NULL);
		fillHorizontal(label, 3, false);
		Composite container = new Composite(composite, SWT.NULL);
		fillHorizontal(container, 3, false);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		
		createMultiLineLabel(container, width, PDEPlugin.getResourceString(KEY_TARGET_DESC), 2);
		createTargetLine(container, TargetEnvironmentPreferencePage.KEY_OS, TargetPlatform.getOS());
		createTargetLine(container, TargetEnvironmentPreferencePage.KEY_WS, TargetPlatform.getWS());
		createTargetLine(container, TargetEnvironmentPreferencePage.KEY_NL, TargetPlatform.getNL());
		createTargetLine(container, TargetEnvironmentPreferencePage.KEY_ARCH, TargetPlatform.getOSArch());
	}
	
	private void createTargetLine(Composite parent, String nameKey, String value) {
		GridData gd = new GridData();
		Label label = new Label(parent, SWT.NULL);
		label.setText(trimMnemonics(PDEPlugin.getResourceString(nameKey)));
		gd.horizontalIndent = 10;
		label.setLayoutData(gd);
		
		label = new Label(parent, SWT.NULL);
		label.setText(value);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		label.setLayoutData(gd);
	}
	private String trimMnemonics(String name) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<name.length(); i++) {
			char c = name.charAt(i);
			if (c!='&') 
				buf.append(c);
		}
		return buf.toString();
	}

	private void hookListeners() {
		runtimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (runtimeLocationButton.getSelection()) {
					setOtherEnabled(false);
					updateStatus();
				}
			}
		});
		otherLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (otherLocationButton.getSelection()) {
					setOtherEnabled(true);
					updateStatus();
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
				updateStatus();
			}
		});
		dropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});
	}

	private void updateStatus() {
		IStatus running = PDEPlugin.getDefault().getCurrentLaunchStatus(null);
		if (running != null) {
			updateStatus(running);
		} else {
			validateDropLocation();
			updateStatus(dropLocationStatus);
		}
	}

	private GridData fillHorizontal(Control control, int span, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	private void initializeFields(IDialogSettings initialSettings) {
		String[] dropItems = new String[0];
		boolean doImport = true;
		boolean doExtract = false;
		boolean doOther = false;

		if (initialSettings != null) {
			doOther = initialSettings.getBoolean(SETTINGS_DOOTHER);
			doImport = !initialSettings.getBoolean(SETTINGS_DOIMPORT);
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
		doImportCheck.setEnabled(!doExtract);
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
			settings.put(SETTINGS_DOIMPORT, !doImportCheck.getSelection());
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