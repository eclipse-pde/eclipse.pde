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
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class BasicLauncherTab
	extends AbstractLauncherTab
	implements ILauncherSettings {
	private static final String KEY_DESC = "";

	private static final String KEY_NAME = "BasicLauncherTab.name";
	private static final String KEY_WORKSPACE = "BasicLauncherTab.workspace";
	private static final String KEY_BROWSE = "BasicLauncherTab.browse";
	private static final String KEY_CLEAR = "BasicLauncherTab.clear";
	private static final String KEY_ASK_CLEAR = "BasicLauncherTab.askClear";
	private static final String KEY_JRE = "BasicLauncherTab.jre";
	private static final String KEY_VMARGS = "BasicLauncherTab.vmArgs";
	private static final String KEY_PARGS = "BasicLauncherTab.programArgs";
	private static final String KEY_APPNAME = "BasicLauncherTab.appName";
	private static final String KEY_SHOW_SPLASH = "BasicLauncherTab.showSplash";
	private static final String KEY_RESTORE = "BasicLauncherTab.restore";
	private static final String KEY_RESTORE_TEXT =
		"BasicLauncherTab.restoreText";
	private static final String KEY_WTITLE = "BasicLauncherTab.workspace.title";
	private static final String KEY_WMESSAGE =
		"BasicLauncherTab.workspace.message";
	private static final String KEY_NO_JRE = "BasicLauncherTab.noJRE";
	private static final String KEY_ENTER_WORKSPACE =
		"BasicLauncherTab.enterWorkspace";
	private static final String KEY_INVALID_WORKSPACE =
		"BasicLauncherTab.invalidWorkspace";
	private static final String KEY_EXISTING_WORKSPACE =
		"BasicLauncherTab.workspaceExisting";

	public static final String RT_WORKSPACE = "runtime-workspace";
	private Combo workspaceCombo;
	private Button browseButton;
	private Button clearWorkspaceCheck;
	private Button askClearCheck;
	private Combo jreCombo;
	private Text vmArgsText;
	private Text progArgsText;
	private Text applicationNameText;
	private Button showSplashCheck;
	private Button defaultsButton;
	private Image image;

	private IStatus jreSelectionStatus;
	private IStatus workspaceSelectionStatus;

	private IVMInstall[] vmInstallations;
	
	private boolean blockChanges = false;

	public BasicLauncherTab() {
		jreSelectionStatus = createStatus(IStatus.OK, "");
		workspaceSelectionStatus = createStatus(IStatus.OK, "");

		vmInstallations = getAllVMInstances();
		image = PDEPluginImages.DESC_ARGUMENT_TAB.createImage();
	}

	public void dispose() {
		super.dispose();
		image.dispose();
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
		
		askClearCheck = new Button(composite, SWT.CHECK);
		askClearCheck.setText(PDEPlugin.getResourceString(KEY_ASK_CLEAR));
		fillIntoGrid(askClearCheck, 3, false);

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
		
		showSplashCheck = new Button(composite, SWT.CHECK);
		showSplashCheck.setText(PDEPlugin.getResourceString(KEY_SHOW_SPLASH));
		fillIntoGrid(showSplashCheck, 3, false);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 3, false);

		defaultsButton = new Button(composite, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_RESTORE));
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		defaultsButton.setLayoutData(data);
		SWTUtil.setButtonDimensionHint(defaultsButton);

		hookListeners();
		setControl(composite);
		
		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
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
		try {
			blockChanges = true;
			vmArgsText.setText(config.getAttribute(VMARGS, ""));
			progArgsText.setText(config.getAttribute(PROGARGS, getDefaultProgramArguments()));
			applicationNameText.setText(config.getAttribute(APPLICATION, ""));
			clearWorkspaceCheck.setSelection(config.getAttribute(DOCLEAR, false));
			askClearCheck.setSelection(config.getAttribute(ASKCLEAR, true));
			showSplashCheck.setSelection(config.getAttribute(SHOW_SPLASH, true));
			askClearCheck.setEnabled(clearWorkspaceCheck.getSelection());
			
			jreCombo.setItems(getVMInstallNames(vmInstallations));
			String vmInstallName =
				config.getAttribute(VMINSTALL, getDefaultVMInstallName());
			for (int i = 0; i < vmInstallations.length; i++) {
				if (vmInstallName.equals(vmInstallations[i].getName())) {
					jreCombo.select(i);
					break;
				}
			}

			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr =
					config.getAttribute(
						LOCATION + String.valueOf(i),
						(String) null);
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}

			workspaceCombo.setItems((String[])items.toArray(new String[items.size()]));
			if (workspaceCombo.getItemCount() > 0) 
				workspaceCombo.select(0);
			else
				workspaceCombo.setText(getDefaultWorkspace());
				
			//validate
			workspaceSelectionStatus = validateWorkspaceSelection();
			jreSelectionStatus = validateJRESelection();
			updateStatus();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		blockChanges = false;
	}

	static String getDefaultWorkspace() {
		IPath path =
			PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
		path = path.append(RT_WORKSPACE);
		return path.toOSString();
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(LOCATION + "0", getDefaultWorkspace());
		config.setAttribute(APPLICATION,"");
		config.setAttribute(DOCLEAR, false);
		config.setAttribute(PROGARGS, getDefaultProgramArguments());
		config.setAttribute(SHOW_SPLASH,true);
		config.setAttribute(ASKCLEAR, true);
		config.setAttribute(VMARGS,"");
	}

	private void doRestoreDefaults() {
		progArgsText.setText(getDefaultProgramArguments());
		vmArgsText.setText("");
		applicationNameText.setText("");
		workspaceCombo.setText(getDefaultWorkspace());
		clearWorkspaceCheck.setSelection(false);
		showSplashCheck.setSelection(true);
		askClearCheck.setSelection(true);
		askClearCheck.setEnabled(false);
		jreCombo.select(
			jreCombo.indexOf(
				JavaRuntime.getDefaultVMInstall().getName()
					+ " - "
					+ JavaRuntime
						.getDefaultVMInstall()
						.getVMInstallType()
						.getName()));
	}
	static String getDefaultVMInstallName() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null)
			return install.getName();
		return null;
	}

	static String getDefaultProgramArguments() {
		String os = TargetPlatform.getOS();
		String ws = TargetPlatform.getWS();
		String arch = TargetPlatform.getOSArch();
		String nl = TargetPlatform.getNL();
		return "-os " + os + " -ws " + ws + " -arch " + arch + " -nl " + nl;
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
				if (!blockChanges) {
					updateStatus();
				} 
			}
		});
		workspaceCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				workspaceSelectionStatus = validateWorkspaceSelection();				
				if (!blockChanges) {
					updateStatus();
				}
			}
		});
		
		clearWorkspaceCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				askClearCheck.setEnabled(clearWorkspaceCheck.getSelection());
				updateLaunchConfigurationDialog();
			}
		});
		
		askClearCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		showSplashCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		
		vmArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockChanges)	
					updateLaunchConfigurationDialog();
			}
		});
		
		progArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockChanges)
					updateLaunchConfigurationDialog();
			}
		});
		
		applicationNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockChanges)
					updateLaunchConfigurationDialog();
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
				updateLaunchConfigurationDialog();
			}
		});
	}

	private void updateStatus() {
		updateStatus(
			getMoreSevere(workspaceSelectionStatus, jreSelectionStatus));
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (!isChanged())
			return;
		config.setAttribute(VMARGS, getVMArguments());
		config.setAttribute(PROGARGS, getProgramArguments());
		try {
			IVMInstall vmInstall = getVMInstall();
			if (vmInstall != null) {
				if (config.getAttribute(VMINSTALL, (String) null) != null) {
					config.setAttribute(VMINSTALL, vmInstall.getName());
				} else {
					config.setAttribute(
						VMINSTALL,
						vmInstall.getName().equals(
							getDefaultVMInstallName())
							? null
							: vmInstall.getName());
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		config.setAttribute(APPLICATION, getApplicationName());
		config.setAttribute(DOCLEAR, doClearWorkspace());
		config.setAttribute(ASKCLEAR, doAskClear());
		config.setAttribute(SHOW_SPLASH, doShowSplash());

		config.setAttribute(
			LOCATION + String.valueOf(0),
			workspaceCombo.getText());
		String[] items = workspaceCombo.getItems();
		int nEntries = Math.min(items.length, 5);
		for (int i = 0; i < nEntries; i++) {
			config.setAttribute(LOCATION + String.valueOf(i + 1), items[i]);
		}
		setChanged(false);
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
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_JRE));
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
		return vmArgsText.getText().trim();
	}

	/**
	 * Returns the selected program arguments.
	 */
	public String getProgramArguments() {
		return progArgsText.getText().trim();
	}

	/**
	 * Returns the selected workspace location. (data location)
	 */
	public IPath getWorkspaceLocation() {
		return new Path(workspaceCombo.getText().trim());
	}

	/**
	 * Returns the selected workspace location. (data location)
	 */
	public boolean doClearWorkspace() {
		return clearWorkspaceCheck.getSelection();
	}
	
	/**
	 * Returns true if users should confirm the workspace
	 * clearing.
	 * @return boolean
	 */
	public boolean doAskClear() {
		return askClearCheck.getSelection();
	}
	
	public boolean doShowSplash() {
		return showSplashCheck.getSelection();
	}

	/**
	 * Returns the selected VM arguments.
	 */
	public String getApplicationName() {
		return applicationNameText.getText().trim();
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
				installs[i].getName()
					+ " - "
					+ installs[i].getVMInstallType().getName();
		}
		return names;
	}

	public String getName() {
		return PDEPlugin.getResourceString(KEY_NAME);
	}
	public Image getImage() {
		return image;
	}

}
