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
import org.eclipse.pde.internal.preferences.TargetPlatformPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.pde.internal.wizards.StatusWizardPage;
import org.eclipse.pde.internal.PDEPlugin;

public class WorkbenchLauncherWizardBasicPage extends StatusWizardPage {
	private static final String KEY_DESC = "";

	private static final String KEY_WORKSPACE =
		"WorkbenchLauncherWizardBasicPage.workspace";
	private static final String KEY_BROWSE =
		"WorkbenchLauncherWizardBasicPage.browse";
	private static final String KEY_CLEAR =
		"WorkbenchLauncherWizardBasicPage.clear";
	private static final String KEY_JRE = "WorkbenchLauncherWizardBasicPage.jre";
	private static final String KEY_VMARGS =
		"WorkbenchLauncherWizardBasicPage.vmArgs";
	private static final String KEY_PARGS =
		"WorkbenchLauncherWizardBasicPage.programArgs";
	private static final String KEY_APPNAME =
		"WorkbenchLauncherWizardBasicPage.appName";
	private static final String KEY_TRACING =
		"WorkbenchLauncherWizardBasicPage.tracing";
	private static final String KEY_RESTORE =
		"WorkbenchLauncherWizardBasicPage.restore";
	private static final String KEY_RESTORE_TEXT =
		"WorkbenchLauncherWizardBasicPage.restoreText";
	private static final String KEY_WTITLE =
		"WorkbenchLauncherWizardBasicPage.workspace.title";
	private static final String KEY_WMESSAGE =
		"WorkbenchLauncherWizardBasicPage.workspace.message";
	private static final String KEY_NO_JRE =
		"WorkbenchLauncherWizardBasicPage.noJRE";
	private static final String KEY_ENTER_WORKSPACE =
		"WorkbenchLauncherWizardBasicPage.enterWorkspace";
	private static final String KEY_INVALID_WORKSPACE =
		"WorkbenchLauncherWizardBasicPage.invalidWorkspace";
	private static final String KEY_EXISTING_WORKSPACE =
		"WorkbenchLauncherWizardBasicPage.workspaceExisting";

	private static final String SETTINGS_VMARGS = "vmargs";
	private static final String SETTINGS_PROGARGS = "progargs";
	private static final String SETTINGS_LOCATION = "location";
	private static final String SETTINGS_VMINSTALL = "vminstall";
	private static final String SETTINGS_APPLICATION = "application";
	private static final String SETTINGS_DOCLEAR = "clearws";
	private static final String SETTINGS_TRACING = "tracing";
	private static final String DEFAULT_VALUE = "[-]";

	public static final String RT_WORKSPACE = "runtime-workspace";
	private Combo workspaceCombo;
	private Button browseButton;
	private Button clearWorkspaceCheck;
	private Combo jreCombo;
	private Text vmArgsText;
	private Text progArgsText;
	private Text applicationNameText;
	private Button defaultsButton;
	private Button tracingCheck;

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
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		IStatus running = PDEPlugin.getDefault().getCurrentLaunchStatus();
		if (running!=null) updateStatus(running);
	}	

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_WORKSPACE));

		workspaceCombo = new Combo(composite, SWT.DROP_DOWN);
		fillIntoGrid(workspaceCombo, 1, true);

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString(KEY_BROWSE));

		clearWorkspaceCheck = new Button(composite, SWT.CHECK);
		clearWorkspaceCheck.setText(PDEPlugin.getResourceString(KEY_CLEAR));
		fillIntoGrid(clearWorkspaceCheck, 3, false);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 3, false);

		label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_JRE));

		jreCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fillIntoGrid(jreCombo, 2, false);

		label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_VMARGS));

		vmArgsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fillIntoGrid(vmArgsText, 2, false);

		label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PARGS));

		progArgsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fillIntoGrid(progArgsText, 2, false);

		label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_APPNAME));

		applicationNameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fillIntoGrid(applicationNameText, 2, false);

		tracingCheck = new Button(composite, SWT.CHECK);
		tracingCheck.setText(PDEPlugin.getResourceString(KEY_TRACING));
		fillIntoGrid(tracingCheck, 2, false);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 3, false);

		defaultsButton = new Button(composite, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_RESTORE));
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint =
			Math.max(
				widthHint,
				defaultsButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		defaultsButton.setLayoutData(data);

		label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_RESTORE_TEXT));
		data = fillIntoGrid(label, 2, false);
		//data.verticalAlignment = GridData.BEGINNING;

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
		String progArgs = "";
		String appName = "org.eclipse.ui.workbench";
		String[] workspaceSelectionItems = new String[0];
		boolean doClear = false;
		boolean tracing = false;

		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();

		String defaultWorkspace = getDefaultWorkspace(pstore);

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
			tracing = initialSettings.getBoolean(SETTINGS_TRACING);

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
		if (workspaceSelectionItems.length > 0)
			workspaceCombo.select(0);
		else
			workspaceCombo.setText(defaultWorkspace);
		clearWorkspaceCheck.setSelection(doClear);
		tracingCheck.setSelection(tracing);
		//validate
		workspaceSelectionStatus = validateWorkspaceSelection();
		jreSelectionStatus = validateJRESelection();
		updateStatus();
	}

	private static String getDefaultWorkspace(IPreferenceStore pstore) {
		TargetPlatformPreferencePage.initializePlatformPath();
		IPath ppath =
			new Path(pstore.getString(TargetPlatformPreferencePage.PROP_PLATFORM_PATH));
		IPath runtimeWorkspace = ppath.append(RT_WORKSPACE);
		return runtimeWorkspace.toOSString();
	}

	/**
	 * Load the stored settings into the provided data object to be
	 * used by the headless launcher
	 */
	static void setLauncherData(IDialogSettings settings, LauncherData data) {
		IVMInstall launcher = null;
		int jreSelectionIndex = 0;
		String vmArgs = "";
		String progArgs = "";
		String appName = "org.eclipse.ui.workbench";
		String[] workspaceSelectionItems = new String[0];
		boolean doClear = false;
		String defaultWorkspace = "";
		boolean tracing = false;

		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		defaultWorkspace = getDefaultWorkspace(pstore);

		if (settings != null) {
			String value = settings.get(SETTINGS_VMARGS);
			if (value != null && !isDefault(value)) {
				vmArgs = value;
			}
			value = settings.get(SETTINGS_PROGARGS);
			if (value != null && !isDefault(value)) {
				progArgs = value;
			}
			value = settings.get(SETTINGS_APPLICATION);
			if (value != null && !isDefault(value)) {
				appName = value;
			}
			value = settings.get(SETTINGS_LOCATION + "0");
			if (value != null && !isDefault(value)) {
				defaultWorkspace = value;
			}
			tracing = settings.getBoolean(SETTINGS_TRACING);

			String vmInstallName = settings.get(SETTINGS_VMINSTALL);
			IVMInstall[] vmInstallations = getAllVMInstances();
			if (vmInstallName != null) {
				for (int i = 0; i < vmInstallations.length; i++) {
					if (vmInstallName.equals(vmInstallations[i].getName())) {
						launcher = vmInstallations[i];
						break;
					}
				}
			} else
				launcher = vmInstallations[0];
			//doClear= initialSettings.getBoolean(SETTINGS_DOCLEAR);
		}
		// Initialize launcher data
		data.setVmInstall(launcher);
		data.setVmArguments(vmArgs);
		data.setProgramArguments(progArgs);
		data.setApplicationName(appName);
		data.setClearWorkspace(doClear);
		data.setWorkspaceLocation(new Path(defaultWorkspace));
		data.setTracingEnabled(tracing);
	}

	private static boolean isDefault(String value) {
		return value.equals(DEFAULT_VALUE);
	}

	private void doRestoreDefaults() {
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();

		String defaultWorkspace = getDefaultWorkspace(pstore);
		progArgsText.setText("");
		vmArgsText.setText("");
		workspaceCombo.setText(defaultWorkspace);
		IDialogSettings settings = getDialogSettings();
		settings.put(SETTINGS_VMARGS, DEFAULT_VALUE);
		settings.put(SETTINGS_PROGARGS, DEFAULT_VALUE);
		settings.put(SETTINGS_LOCATION + "0", defaultWorkspace);
		clearWorkspaceCheck.setSelection(false);
		tracingCheck.setSelection(false);
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
		IStatus running = PDEPlugin.getDefault().getCurrentLaunchStatus();
		if (running != null)
			updateStatus(running);
		else
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
			initialSettings.put(SETTINGS_TRACING, isTracingEnabled());
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
		dialog.setText(PDEPlugin.getResourceString(KEY_WTITLE));
		dialog.setMessage(PDEPlugin.getResourceString(KEY_WMESSAGE));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	private IStatus validateJRESelection() {
		IVMInstall curr = getVMInstall();
		if (curr == null) {
			return createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_NO_JRE));
		}
		return createStatus(IStatus.OK, "");
	}

	private IStatus validateWorkspaceSelection() {
		IPath curr = getWorkspaceLocation();
		if (curr.segmentCount() == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_ENTER_WORKSPACE));
		}
		if (!Path.ROOT.isValidPath(workspaceCombo.getText())) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_INVALID_WORKSPACE));
		}

		File file = curr.toFile();
		if (file.isFile()) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_EXISTING_WORKSPACE));
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

	/**
	 * Returns true if tracing is enabled
	 */
	public boolean isTracingEnabled() {
		return tracingCheck.getSelection();
	}

	private static IVMInstall[] getAllVMInstances() {
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