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

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.core.IWorkspaceModelManager;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class BasicLauncherTab
	extends AbstractLauncherTab
	implements ILauncherSettings {

	private static final String KEY_NAME = "BasicLauncherTab.name";
	
	private Combo workspaceCombo;
	private Button browseButton;
	private Button clearWorkspaceCheck;
	private Button askClearCheck;
	private Combo jreCombo;
	private Text classpathText;
	private Text vmArgsText;
	private Text progArgsText;
	private Button defaultsButton;
	private Image image;
	private String currentClasspath;

	private IStatus jreSelectionStatus;
	private IStatus workspaceSelectionStatus;

	
	private boolean blockChanges = false;

	public BasicLauncherTab() {
		jreSelectionStatus = createStatus(IStatus.OK, "");
		workspaceSelectionStatus = createStatus(IStatus.OK, "");
		image = PDEPluginImages.DESC_ARGUMENT_TAB.createImage();
	}

	public void dispose() {
		super.dispose();
		image.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createWorkspaceDataSection(composite);
		createApplicationSection(composite);
		createCommandLineSettingsSection(composite);
		createDefaultsButton(composite);
		
		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}
	
	protected void createDefaultsButton(Composite parent) {
		defaultsButton = new Button(parent, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.restore"));
		defaultsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(defaultsButton);
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doRestoreDefaults();
				updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void createWorkspaceDataSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("BasicLauncherTab.workspace"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.location"));

		workspaceCombo = new Combo(group, SWT.DROP_DOWN);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		workspaceCombo.setLayoutData(gd);
		workspaceCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				workspaceSelectionStatus = validateWorkspaceSelection();
				if (!blockChanges) 
					updateStatus();
			}
		});
		workspaceCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				workspaceSelectionStatus = validateWorkspaceSelection();				
				if (!blockChanges)
					updateStatus();
			}
		});

		browseButton = new Button(group, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.browse"));
		browseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseWorkspaceLocation();
				if (chosen != null) {
					workspaceCombo.setText(chosen.toOSString());
					updateStatus();
				}
			}
		});
		SWTUtil.setButtonDimensionHint(browseButton);

		clearWorkspaceCheck = new Button(group, SWT.CHECK);
		clearWorkspaceCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.clear"));
		gd = new GridData();
		gd.horizontalSpan = 3;
		clearWorkspaceCheck.setLayoutData(gd);
		clearWorkspaceCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				askClearCheck.setEnabled(clearWorkspaceCheck.getSelection());
				updateLaunchConfigurationDialog();
			}
		});
		
		askClearCheck = new Button(group, SWT.CHECK);
		askClearCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.askClear"));
		gd = new GridData();
		gd.horizontalSpan = 3;
		askClearCheck.setLayoutData(gd);
		askClearCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});	
	}
	
	protected void createCommandLineSettingsSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("BasicLauncherTab.commandLineSettings"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createJRESection(group);
		createVMArgsSection(group);
		createProgArgsSection(group);
		createDevEntriesSection(group);
		createShowSplashSection(group);		
	}
	
	protected void createApplicationSection(Composite parent) {
	}
	
	protected void createJRESection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.jre"));

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		jreCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		jreCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		jreCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				jreSelectionStatus = validateJRESelection();
				updateStatus();
			}
		});
		
		Button button = new Button(composite, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("BasicLauncherTab.installedJREs"));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String currentVM = jreCombo.getText();
				IPreferenceNode node = new InstalledJREsPreferenceNode();
				if (showPreferencePage(node)) {
					jreCombo.setItems(LauncherUtils.getVMInstallNames());
					jreCombo.setText(currentVM);
					if (jreCombo.getSelectionIndex() == -1)
						jreCombo.setText(LauncherUtils.getDefaultVMInstallName());
				}
			}
			private boolean showPreferencePage(final IPreferenceNode targetNode) {
				PreferenceManager manager = new PreferenceManager();
				manager.addToRoot(targetNode);
				final PreferenceDialog dialog =
					new PreferenceDialog(getControl().getShell(), manager);
				final boolean[] result = new boolean[] { false };
				BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
					public void run() {
						dialog.create();
						dialog.setMessage(targetNode.getLabelText());
						if (dialog.open() == PreferenceDialog.OK)
							result[0] = true;
					}
				});
				return result[0];
			}
		});
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(button);		
	}
	
	protected void createVMArgsSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.vmArgs"));
		
		vmArgsText = new Text(parent, SWT.BORDER);
		vmArgsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		vmArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockChanges)	
					updateLaunchConfigurationDialog();
			}
		});
	}
	
	protected void createProgArgsSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.programArgs"));

		progArgsText = new Text(parent, SWT.BORDER);
		progArgsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockChanges)
					updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void createDevEntriesSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.classpath"));
		
		classpathText = new Text(parent, SWT.BORDER);
		classpathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		classpathText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockChanges)	
					updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void createShowSplashSection(Composite parent) {
	}

	public void initializeFrom(ILaunchConfiguration config) {
		try {
			blockChanges = true;
			
			initializeWorkspaceDataSection(config);
			initializeJRESection(config);
			initializeApplicationSection(config);
			initializeVMArgsSection(config);
			initializeProgArgsSection(config);
			initializeDevEntriesSection(config);
			initializeShowSplashSection(config);
				
			workspaceSelectionStatus = validateWorkspaceSelection();
			jreSelectionStatus = validateJRESelection();
			updateStatus();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		blockChanges = false;
	}

	protected void initializeApplicationSection(ILaunchConfiguration config)
		throws CoreException {
	}

	protected void initializeWorkspaceDataSection(ILaunchConfiguration config)
		throws CoreException {
		ArrayList items = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr =
				config.getAttribute(LOCATION + String.valueOf(i), (String) null);
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}

		workspaceCombo.setItems((String[]) items.toArray(new String[items.size()]));
		if (workspaceCombo.getItemCount() > 0)
			workspaceCombo.setText(items.get(0).toString());

		clearWorkspaceCheck.setSelection(config.getAttribute(DOCLEAR, false));
		askClearCheck.setSelection(config.getAttribute(ASKCLEAR, true));
		askClearCheck.setEnabled(clearWorkspaceCheck.getSelection());
	}
	
	protected void initializeJRESection(ILaunchConfiguration config) throws CoreException {
		jreCombo.setItems(LauncherUtils.getVMInstallNames());
		String vmInstallName =
			config.getAttribute(VMINSTALL, LauncherUtils.getDefaultVMInstallName());
		jreCombo.setText(vmInstallName);
		if (jreCombo.getSelectionIndex() == -1)
			jreCombo.setText(LauncherUtils.getDefaultVMInstallName());
	}
	
	protected void initializeVMArgsSection(ILaunchConfiguration config) throws CoreException {
		vmArgsText.setText(config.getAttribute(VMARGS, ""));		
	}
	
	protected void initializeProgArgsSection(ILaunchConfiguration config) throws CoreException {
		progArgsText.setText(config.getAttribute(PROGARGS, LauncherUtils.getDefaultProgramArguments()));		
	}
	
	protected void initializeDevEntriesSection(ILaunchConfiguration config) throws CoreException {
		classpathText.setText(config.getAttribute(CLASSPATH_ENTRIES, getClasspathEntries()));		
	}

	protected void initializeShowSplashSection(ILaunchConfiguration config) throws CoreException {
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(LOCATION + "0", LauncherUtils.getDefaultWorkspace());
		config.setAttribute(DOCLEAR, false);
		config.setAttribute(PROGARGS, LauncherUtils.getDefaultProgramArguments());
		config.setAttribute(SHOW_SPLASH,true);
		config.setAttribute(ASKCLEAR, true);
		config.setAttribute(VMARGS,"");
	}
	
	private String getClasspathEntries() {
		if (currentClasspath != null)
			return currentClasspath;
		IWorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] wsmodels = manager.getAllModels();
		currentClasspath = LauncherUtils.getBuildOutputFolders(wsmodels);
		return currentClasspath;
	}

	protected void doRestoreDefaults() {
		progArgsText.setText(LauncherUtils.getDefaultProgramArguments());
		vmArgsText.setText("");
		workspaceCombo.setText(LauncherUtils.getDefaultWorkspace());
		classpathText.setText(getClasspathEntries());
		clearWorkspaceCheck.setSelection(false);
		askClearCheck.setSelection(true);
		askClearCheck.setEnabled(false);
		jreCombo.setText(LauncherUtils.getDefaultVMInstallName());		
	}

	private void updateStatus() {
		updateStatus(
			getMoreSevere(workspaceSelectionStatus, jreSelectionStatus));
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		try {
			if (isChanged()) {
				saveWorkspaceDataSection(config);
				saveApplicationSection(config);
				saveJRESection(config);
				saveVMArgsSection(config);
				saveProgArgsSection(config);
				saveDevEntriesSection(config);
				saveShowSplashSection(config);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			if (isChanged())
				setChanged(false);
		}
	}
	
	protected void saveWorkspaceDataSection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		config.setAttribute(LOCATION + String.valueOf(0), workspaceCombo.getText());
		if (workspaceCombo.getItemCount() > 1) {
			String[] items = workspaceCombo.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				config.setAttribute(LOCATION + String.valueOf(i + 1), items[i]);
			}
		}

		config.setAttribute(DOCLEAR, clearWorkspaceCheck.getSelection());
		config.setAttribute(ASKCLEAR, askClearCheck.getSelection());
	}
	
	protected void saveJRESection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		if (jreCombo.getSelectionIndex() == -1)
			return;

		String jre = jreCombo.getText();
		if (config.getAttribute(VMINSTALL, (String) null) != null) {
			config.setAttribute(VMINSTALL, jre);
		} else {
			config.setAttribute(
				VMINSTALL,
				jre.equals(LauncherUtils.getDefaultVMInstallName()) ? null : jre);
		}
	}
	
	protected void saveVMArgsSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(VMARGS, vmArgsText.getText().trim());
	}
	
	protected void saveProgArgsSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(PROGARGS, progArgsText.getText().trim());		
	}
	
	protected void saveDevEntriesSection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		String classpath = classpathText.getText().trim();
		if (config.getAttribute(CLASSPATH_ENTRIES, (String) null) != null) {
			config.setAttribute(CLASSPATH_ENTRIES, classpath);
		} else {
			config.setAttribute(
				CLASSPATH_ENTRIES,
				classpath.equals(getClasspathEntries()) ? null : classpath);
		}
	}
	
	protected void saveShowSplashSection(ILaunchConfigurationWorkingCopy config) {
	}
	
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}

	private IPath chooseWorkspaceLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
		dialog.setFilterPath(workspaceCombo.getText());
		dialog.setText(PDEPlugin.getResourceString("BasicLauncherTab.workspace.title"));
		dialog.setMessage(PDEPlugin.getResourceString("BasicLauncherTab.workspace.message"));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	private IStatus validateJRESelection() {
		if (jreCombo.getSelectionIndex() == -1) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.noJRE"));
		}
		return createStatus(IStatus.OK, "");
	}

	private IStatus validateWorkspaceSelection() {
		String location = workspaceCombo.getText().trim();
		if (!Path.ROOT.isValidPath(location)) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.invalidWorkspace"));
		}
		
		IPath curr = new Path(location);
		if (curr.segmentCount() == 0 && curr.getDevice() == null) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.noWorkspace"));
		}

		return createStatus(IStatus.OK, "");
	}

	public String getName() {
		return PDEPlugin.getResourceString(KEY_NAME);
	}
	
	public Image getImage() {
		return image;
	}

}
