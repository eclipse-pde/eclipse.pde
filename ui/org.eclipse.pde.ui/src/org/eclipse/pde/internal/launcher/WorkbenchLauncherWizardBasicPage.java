/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.launching.*;
import org.eclipse.pde.internal.preferences.PDEBasePreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.pde.internal.wizards.StatusWizardPage;
import org.eclipse.pde.internal.PDEPlugin;

public class WorkbenchLauncherWizardBasicPage extends StatusWizardPage {
	private static final String KEY_DESC = "";

	private static final String SETTINGS_VMARGS = "vmargs";
	private static final String SETTINGS_PROGARGS = "progargs";
	private static final String SETTINGS_LOCATION = "location";
	private static final String SETTINGS_VMINSTALL = "vminstall";
	private static final String SETTINGS_APPLICATION = "application";
	private static final String SETTINGS_DOCLEAR = "clearws";
	private static final String DEFAULT_VALUE = "[-]";

	private Combo workspaceCombo;
	private Button browseButton;
	private Button clearWorkspaceCheck;
	private Combo jreCombo;
	private Text vmArgsText;
	private Text progArgsText;
	private Text applicationNameText;
	private Button defaultsButton;

	private IStatus jreSelectionStatus;
	private IStatus workspaceSelectionStatus;

	private IVMInstall[] vmInstallations;

	public WorkbenchLauncherWizardBasicPage(String title) {
		super("WorkbenchLauncherWizardBasicPage", true);
		setTitle(title);
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		jreSelectionStatus = createStatus(IStatus.OK, "");
		workspaceSelectionStatus = createStatus(IStatus.OK, "");

		vmInstallations = getAllVMInstances();
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NULL);
		label.setText("&Workspace data:");

		workspaceCombo = new Combo(composite, SWT.DROP_DOWN);
		fillIntoGrid(workspaceCombo, 1, true);

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("B&rowse...");

		clearWorkspaceCheck = new Button(composite, SWT.CHECK);
		clearWorkspaceCheck.setText("&Clear workspace data before launching");
		fillIntoGrid(clearWorkspaceCheck, 3, false);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 3, false);

		label = new Label(composite, SWT.NULL);
		label.setText("&JRE:");

		jreCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fillIntoGrid(jreCombo, 2, false);

		label = new Label(composite, SWT.NULL);
		label.setText("&VM Arguments");

		vmArgsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fillIntoGrid(vmArgsText, 2, false);

		label = new Label(composite, SWT.NULL);
		label.setText("&Program arguments:");

		progArgsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fillIntoGrid(progArgsText, 2, false);

		label = new Label(composite, SWT.NULL);
		label.setText("Applic&ation name:");

		applicationNameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fillIntoGrid(applicationNameText, 2, false);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 3, false);

		defaultsButton = new Button(composite, SWT.PUSH);
		defaultsButton.setText("Restore &Defaults");
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, defaultsButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		defaultsButton.setLayoutData(data);

		label = new Label(composite, SWT.NULL);
		label.setText(
			"You can restore launcher settings to the values set in the Preferences.");
		data = fillIntoGrid(label, 2, false);
		data.verticalAlignment = GridData.BEGINNING;

		initializeFields();
		hookListeners();

		setControl(composite);
	}

	private GridData fillIntoGrid(
		Control control,
		int horizontalSpan,
		boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = horizontalSpan;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	private void initializeFields() {
		IDialogSettings initialSettings = getDialogSettings();
		int jreSelectionIndex = 0;
		String vmArgs = "";
		String progArgs = "-dev bin";
		String appName = "org.eclipse.ui.workbench";
		String[] workspaceSelectionItems = new String[0];
		boolean doClear = false;

		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();

		String defaultWorkspace =
			pstore.getString(PDEBasePreferencePage.PROP_PLATFORM_LOCATION);
		progArgs = pstore.getString(PDEBasePreferencePage.PROP_PLATFORM_ARGS);
		vmArgs = pstore.getString(PDEBasePreferencePage.PROP_VM_ARGS);
		workspaceCombo.setText(defaultWorkspace);

		if (initialSettings != null) {
			String value = initialSettings.get(SETTINGS_VMARGS);
			if (value != null && !isDefault(value)) {
				vmArgs = value;
			}
			value = initialSettings.get(SETTINGS_PROGARGS);
			if (value != null && !isDefault(value)) {
				progArgs = value;
			}
			value = initialSettings.get(SETTINGS_APPLICATION);
			if (value != null) {
				appName = value;
			}

			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr = initialSettings.get(SETTINGS_LOCATION + String.valueOf(i));
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			workspaceSelectionItems = (String[]) items.toArray(new String[items.size()]);

			String vmInstallName = initialSettings.get(SETTINGS_VMINSTALL);
			if (vmInstallName != null) {
				for (int i = 0; i < vmInstallations.length; i++) {
					if (vmInstallName.equals(vmInstallations[i].getName())) {
						jreSelectionIndex = i;
						break;
					}
				}
			}
			//doClear= initialSettings.getBoolean(SETTINGS_DOCLEAR);
		}
		jreCombo.setItems(getVMInstallNames(vmInstallations));
		jreCombo.select(jreSelectionIndex);
		vmArgsText.setText(vmArgs);
		progArgsText.setText(progArgs);
		applicationNameText.setText(appName);
		workspaceCombo.setItems(workspaceSelectionItems);
		workspaceCombo.select(0);
		clearWorkspaceCheck.setSelection(doClear);
		//validate
		workspaceSelectionStatus = validateWorkspaceSelection();
		jreSelectionStatus = validateJRESelection();
		updateStatus();
	}

	private boolean isDefault(String value) {
		return value.equals(DEFAULT_VALUE);
	}

	private void doRestoreDefaults() {
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();

		String defaultWorkspace =
			pstore.getString(PDEBasePreferencePage.PROP_PLATFORM_LOCATION);
		progArgsText.setText(
			pstore.getString(PDEBasePreferencePage.PROP_PLATFORM_ARGS));
		vmArgsText.setText(pstore.getString(PDEBasePreferencePage.PROP_VM_ARGS));
		workspaceCombo.setText(defaultWorkspace);
		IDialogSettings settings = getDialogSettings();
		settings.put(SETTINGS_VMARGS, DEFAULT_VALUE);
		settings.put(SETTINGS_PROGARGS, DEFAULT_VALUE);
		settings.put(SETTINGS_LOCATION + "0", defaultWorkspace);
		clearWorkspaceCheck.setSelection(false);
	}

	private void hookListeners() {
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseWorkspaceLocation();
				if (chosen != null) {
					workspaceCombo.setText(chosen.toOSString());
					updateStatus();
				}
			}
		});
		workspaceCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				workspaceSelectionStatus = validateWorkspaceSelection();
				updateStatus();
			}
		});
		workspaceCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				workspaceSelectionStatus = validateWorkspaceSelection();
				updateStatus();
			}
		});
		jreCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				jreSelectionStatus = validateJRESelection();
				updateStatus();
			}
		});
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doRestoreDefaults();
			}
		});
	}

	private void updateStatus() {
		updateStatus(getMoreSevere(workspaceSelectionStatus, jreSelectionStatus));
	}

	public void storeSettings(boolean finishPressed) {
		IDialogSettings initialSettings = getDialogSettings();
		if (finishPressed) {
			initialSettings.put(SETTINGS_VMARGS, getVMArguments());
			initialSettings.put(SETTINGS_PROGARGS, getProgramArguments());
			initialSettings.put(SETTINGS_VMINSTALL, getVMInstall().getName());
			initialSettings.put(SETTINGS_APPLICATION, getApplicationName());
			initialSettings.put(SETTINGS_DOCLEAR, doClearWorkspace());
		}

		if (finishPressed || workspaceCombo.getText().length() > 0) {
			initialSettings.put(
				SETTINGS_LOCATION + String.valueOf(0),
				workspaceCombo.getText());
			String[] items = workspaceCombo.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				initialSettings.put(SETTINGS_LOCATION + String.valueOf(i + 1), items[i]);
			}
		}
	}

	/**
	 * Browses for a workbench location.
	 */
	private IPath chooseWorkspaceLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(workspaceCombo.getText());
		dialog.setText("Workspace Location");
		dialog.setMessage("Select the workspace data location.");
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	private IStatus validateJRESelection() {
		IVMInstall curr = getVMInstall();
		if (curr == null) {
			return createStatus(IStatus.ERROR, "No JRE selected.");
		}
		return createStatus(IStatus.OK, "");
	}

	private IStatus validateWorkspaceSelection() {
		IPath curr = getWorkspaceLocation();
		if (curr.segmentCount() == 0) {
			return createStatus(IStatus.ERROR, "Enter the workspace data path.");
		}
		if (!Path.ROOT.isValidPath(workspaceCombo.getText())) {
			return createStatus(IStatus.ERROR, "Workspace data path is invalid.");
		}

		File file = curr.toFile();
		if (file.isFile()) {
			return createStatus(
				IStatus.ERROR,
				"Workspace data path points to an existing file.");
		}
		return createStatus(IStatus.OK, "");
	}

	// ----- public API -------

	/**
	 * Returns the selected VMInstall.
	 */
	public IVMInstall getVMInstall() {
		int index = jreCombo.getSelectionIndex();
		if (index >= 0 && index < vmInstallations.length) {
			return vmInstallations[index];
		}
		return null;
	}

	/**
	 * Returns the selected VM arguments.
	 */
	public String getVMArguments() {
		return vmArgsText.getText();
	}

	/**
	 * Returns the selected program arguments.
	 */
	public String getProgramArguments() {
		return progArgsText.getText();
	}

	/**
	 * Returns the selected workspace location. (data location)
	 */
	public IPath getWorkspaceLocation() {
		return new Path(workspaceCombo.getText());
	}

	/**
	 * Returns the selected workspace location. (data location)
	 */
	public boolean doClearWorkspace() {
		return clearWorkspaceCheck.getSelection();
	}

	/**
	 * Returns the selected VM arguments.
	 */
	public String getApplicationName() {
		return applicationNameText.getText();
	}

	private IVMInstall[] getAllVMInstances() {
		ArrayList res = new ArrayList();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstall[] installs = types[i].getVMInstalls();
			for (int k = 0; k < installs.length; k++) {
				res.add(installs[k]);
			}
		}
		return (IVMInstall[]) res.toArray(new IVMInstall[res.size()]);
	}

	private String[] getVMInstallNames(IVMInstall[] installs) {
		String[] names = new String[installs.length];
		for (int i = 0; i < installs.length; i++) {
			names[i] =
				installs[i].getName() + " - " + installs[i].getVMInstallType().getName();
		}
		return names;
	}

}