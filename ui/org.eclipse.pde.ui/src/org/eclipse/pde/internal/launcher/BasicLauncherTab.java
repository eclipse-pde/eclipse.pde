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
import org.eclipse.pde.internal.util.SWTUtil;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.debug.ui.*;
import org.eclipse.debug.core.*;

public class BasicLauncherTab extends AbstractLauncherTab implements ILauncherSettings, ICurrentLaunchListener {
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

	public static final String RT_WORKSPACE = "runtime-workspace";
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

	public BasicLauncherTab() {
		jreSelectionStatus = createStatus(IStatus.OK, "");
		workspaceSelectionStatus = createStatus(IStatus.OK, "");

		vmInstallations = getAllVMInstances();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		createStartingSpace(composite, 3);
		
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

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 3, false);

		defaultsButton = new Button(composite, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_RESTORE));
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		defaultsButton.setLayoutData(data);
		SWTUtil.setButtonDimensionHint(defaultsButton);

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

	public void initializeFrom(ILaunchConfiguration config) {
		int jreSelectionIndex = 0;
		String vmArgs = "";
		String progArgs = "";
		String appName = "org.eclipse.ui.workbench";
		String[] workspaceSelectionItems = new String[0];
		boolean doClear = false;

		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();

		String defaultWorkspace = getDefaultWorkspace(pstore);

		try {
			vmArgs = config.getAttribute(VMARGS, vmArgs);
			progArgs = config.getAttribute(PROGARGS, progArgs);
			appName = config.getAttribute(APPLICATION, appName);
			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr =
					config.getAttribute(LOCATION + String.valueOf(i), (String) null);
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			workspaceSelectionItems = (String[]) items.toArray(new String[items.size()]);

			String vmInstallName = config.getAttribute(VMINSTALL, (String) null);
			if (vmInstallName != null) {
				for (int i = 0; i < vmInstallations.length; i++) {
					if (vmInstallName.equals(vmInstallations[i].getName())) {
						jreSelectionIndex = i;
						break;
					}
				}
			}
			doClear = config.getAttribute(DOCLEAR, doClear);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
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
		//validate
		workspaceSelectionStatus = validateWorkspaceSelection();
		jreSelectionStatus = validateJRESelection();
		updateStatus();
	}

	static String getDefaultWorkspace(IPreferenceStore pstore) {
		TargetPlatformPreferencePage.initializePlatformPath();
		IPath ppath =
			new Path(pstore.getString(TargetPlatformPreferencePage.PROP_PLATFORM_PATH));
		IPath runtimeWorkspace = ppath.append(RT_WORKSPACE);
		return runtimeWorkspace.toOSString();
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		String vmArgs = "";
		String progArgs = "";
		String appName = "org.eclipse.ui.workbench";
		boolean tracing = false;

		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		String defaultWorkspace = getDefaultWorkspace(pstore);
		config.setAttribute(VMARGS, vmArgs);
		config.setAttribute(PROGARGS, progArgs);
		config.setAttribute(APPLICATION, appName);
		config.setAttribute(TRACING, tracing);
		config.setAttribute(VMINSTALL, JavaRuntime.getDefaultVMInstall().getName());
		config.setAttribute(LOCATION + "0", defaultWorkspace);
		config.setAttribute(DOCLEAR, false);
	}

	private void doRestoreDefaults() {
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		String defaultWorkspace = getDefaultWorkspace(pstore);
		progArgsText.setText("");
		vmArgsText.setText("");
		applicationNameText.setText("org.eclipse.ui.workbench");
		workspaceCombo.setText(defaultWorkspace);
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
		IStatus running = PDEPlugin.getDefault().getCurrentLaunchStatus();
		if (running != null)
			updateStatus(running);
		else
			updateStatus(getMoreSevere(workspaceSelectionStatus, jreSelectionStatus));
	}
	
	public void currentLaunchChanged() {
		getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				updateStatus();
			}
		});
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(VMARGS, getVMArguments());
		config.setAttribute(PROGARGS, getProgramArguments());
		config.setAttribute(VMINSTALL, getVMInstall().getName());
		config.setAttribute(APPLICATION, getApplicationName());
		config.setAttribute(DOCLEAR, doClearWorkspace());
		
		config.setAttribute(LOCATION + String.valueOf(0),
									workspaceCombo.getText());
		String[] items = workspaceCombo.getItems();
		int nEntries = Math.min(items.length, 5);
		for (int i = 0; i < nEntries; i++) {
			config.setAttribute(LOCATION + String.valueOf(i + 1), items[i]);
		}
	}

	/**
	 * Browses for a workbench location.
	 */
	private IPath chooseWorkspaceLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
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

	static IVMInstall[] getAllVMInstances() {
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