/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.core.*;
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

	private static final String KEY_NAME = "BasicLauncherTab.name"; //$NON-NLS-1$
	
	private Combo fWorkspaceCombo;
	private Button fBrowseButton;
	private Button fClearWorkspaceCheck;
	private Button fAskClearCheck;
	private Combo fJreCombo;
	private Text fVmArgsText;
	private Text fProgArgsText;
	private Image fImage;
	private Button fJavawButton;
	private Button fJavaButton;

	private IStatus fJreSelectionStatus;
	private IStatus fWorkspaceSelectionStatus;
	
	private boolean fBlockChanges = false;

	protected Combo fApplicationCombo;

	private Text fBootstrap;

	private Combo fProductCombo;

	private Button fProductButton;

	private Button fApplicationButton;

	public BasicLauncherTab() {
		fJreSelectionStatus = createStatus(IStatus.OK, ""); //$NON-NLS-1$
		fWorkspaceSelectionStatus = createStatus(IStatus.OK, ""); //$NON-NLS-1$
		fImage = PDEPluginImages.DESC_MAIN_TAB.createImage();
	}

	public void dispose() {
		super.dispose();
		fImage.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createWorkspaceDataSection(composite);
		createProgramSection(composite);
		createCommandLineSettingsSection(composite);
		
		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}
	
	private void createProgramSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("BasicLauncherTab.programToRun")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		createApplicationSection(group);
		createProductSection(group);		
	}

	protected void createWorkspaceDataSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("BasicLauncherTab.workspace")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.location")); //$NON-NLS-1$

		fWorkspaceCombo = new Combo(group, SWT.DROP_DOWN);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fWorkspaceCombo.setLayoutData(gd);
		fWorkspaceCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fWorkspaceSelectionStatus = validateWorkspaceSelection();
				if (!fBlockChanges) 
					updateStatus();
			}
		});
		fWorkspaceCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fWorkspaceSelectionStatus = validateWorkspaceSelection();				
				if (!fBlockChanges)
					updateStatus();
			}
		});

		fBrowseButton = new Button(group, SWT.PUSH);
		fBrowseButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.browse")); //$NON-NLS-1$
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseWorkspaceLocation();
				if (chosen != null) {
					String destination = chosen.toOSString();
					if (fWorkspaceCombo.indexOf(destination) == -1)
						fWorkspaceCombo.add(destination, 0);
					fWorkspaceCombo.setText(destination);
					if (fClearWorkspaceCheck.getSelection())
						fClearWorkspaceCheck.setSelection(false);
					updateStatus();
				}
			}
		});
		SWTUtil.setButtonDimensionHint(fBrowseButton);

		fClearWorkspaceCheck = new Button(group, SWT.CHECK);
		fClearWorkspaceCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.clear")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fClearWorkspaceCheck.setLayoutData(gd);
		fClearWorkspaceCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAskClearCheck.setEnabled(fClearWorkspaceCheck.getSelection());
				updateLaunchConfigurationDialog();
			}
		});
		
		fAskClearCheck = new Button(group, SWT.CHECK);
		fAskClearCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.askClear")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fAskClearCheck.setLayoutData(gd);
		fAskClearCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});	
	}
	
	protected void createCommandLineSettingsSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("BasicLauncherTab.commandLineSettings")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createJavaExecutableSection(group);
		createJRESection(group);
		createVMArgsSection(group);
		createProgArgsSection(group);
		createBootstrapEntriesSection(group);
	}
	
	protected void createProductSection(Composite parent) {
		fProductButton = new Button(parent, SWT.RADIO);
		fProductButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.runProduct")); //$NON-NLS-1$
		fProductButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fProductButton.getSelection();
				fApplicationCombo.setEnabled(!selected);
				fProductCombo.setEnabled(selected);
				updateLaunchConfigurationDialog();
			}
		});
		
		fProductCombo = new Combo(parent, SWT.READ_ONLY|SWT.DROP_DOWN);
		fProductCombo.setItems(TargetPlatform.getProductNames());
		fProductCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProductCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void createApplicationSection(Composite parent) {
		fApplicationButton = new Button(parent, SWT.RADIO);
		fApplicationButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.runApplication")); //$NON-NLS-1$
			
		fApplicationCombo = new Combo(parent, SWT.READ_ONLY|SWT.DROP_DOWN);
		fApplicationCombo.setItems(getApplicationNames());
		fApplicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fApplicationCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected String[] getApplicationNames() {
		return TargetPlatform.getApplicationNames();
	}
	
	protected String getApplicationAttribute() {
		return APPLICATION;
	}
		
	protected void createJRESection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.jre")); //$NON-NLS-1$

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fJreCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fJreCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fJreCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fJreSelectionStatus = validateJRESelection();
				updateStatus();
			}
		});
		
		Button button = new Button(composite, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("BasicLauncherTab.installedJREs")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String currentVM = fJreCombo.getText();
				IPreferenceNode node = new InstalledJREsPreferenceNode();
				if (showPreferencePage(node)) {
					fJreCombo.setItems(LauncherUtils.getVMInstallNames());
					fJreCombo.setText(currentVM);
					if (fJreCombo.getSelectionIndex() == -1)
						fJreCombo.setText(LauncherUtils.getDefaultVMInstallName());
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
	
	protected void createJavaExecutableSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.javaExec")); //$NON-NLS-1$

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		layout.horizontalSpacing = 20;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fJavawButton = new Button(composite, SWT.RADIO);
		fJavawButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.javaExecDefault")); //$NON-NLS-1$ 
		fJavawButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fJavaButton = new Button(composite, SWT.RADIO);
		fJavaButton.setText("&java");	 //$NON-NLS-1$
		fJavaButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}
	
	protected void createVMArgsSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.vmArgs")); //$NON-NLS-1$
		
		fVmArgsText = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fVmArgsText.setLayoutData(gd);		
		fVmArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)	
					updateLaunchConfigurationDialog();
			}
		});
	}
	
	protected void createProgArgsSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.programArgs")); //$NON-NLS-1$

		fProgArgsText = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fProgArgsText.setLayoutData(gd);
		fProgArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					updateLaunchConfigurationDialog();
			}
		});		
	}
		
	private void createBootstrapEntriesSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.bootstrap")); //$NON-NLS-1$
		
		fBootstrap = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fBootstrap.setLayoutData(gd);
		fBootstrap.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)	
					updateLaunchConfigurationDialog();
			}
		});		
	}

	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fBlockChanges = true;			
			initializeWorkspaceDataSection(config);
			initializeJRESection(config);
			initializeProgramToRunSection(config);
			initializeVMArgsSection(config);
			initializeProgArgsSection(config);
			initializeBootstrapEntriesSection(config);	
			fWorkspaceSelectionStatus = validateWorkspaceSelection();
			fJreSelectionStatus = validateJRESelection();
			updateStatus();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			fBlockChanges = false;
		}
	}
	
	protected void initializeProgramToRunSection(ILaunchConfiguration config) throws CoreException {
		initializeApplicationSection(config);
		initializeProductSection(config);
		
		boolean useProduct = config.getAttribute(USE_PRODUCT, false)
			&& PDECore.getDefault().getModelManager().isOSGiRuntime() 
			&& fProductCombo.getItemCount() > 0;
		fApplicationButton.setSelection(!useProduct);
		fApplicationCombo.setEnabled(!useProduct);
		fProductButton.setSelection(useProduct);
		fProductButton.setEnabled(fProductCombo.getItemCount() > 0);
		fProductCombo.setEnabled(useProduct);
	}
	
	protected void initializeProductSection(ILaunchConfiguration config) throws CoreException {
		if (fProductCombo.getItemCount() > 0) {
			String productName = config.getAttribute(PRODUCT, (String)null);
			int index = productName == null ? -1 : fProductCombo.indexOf(productName);
			if (index == -1)
				index = 0;
			fProductCombo.setText(fProductCombo.getItem(index));
		}
	}

	protected void initializeApplicationSection(ILaunchConfiguration config)
		throws CoreException {
		
		String attribute = getApplicationAttribute();
		
		// first see if the application name has been set on the launch config
		String application = config.getAttribute(attribute, (String) null);
		if (application == null
			|| fApplicationCombo.indexOf(application) == -1) {
			application = null;
			
			// check if the user has entered the -application arg in the program arg field
			StringTokenizer tokenizer =
				new StringTokenizer(config.getAttribute(PROGARGS, "")); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equals("-application") && tokenizer.hasMoreTokens()) { //$NON-NLS-1$
					application = tokenizer.nextToken();
					break;
				}
			}
			
			int index = -1;
			if (application != null)
				index = fApplicationCombo.indexOf(application);
			
			// use default application as specified in the install.ini of the target platform
			if (index == -1)
				index = fApplicationCombo.indexOf(LauncherUtils.getDefaultApplicationName());
			
			if (index != -1) {
				fApplicationCombo.setText(fApplicationCombo.getItem(index));
			} else if (fApplicationCombo.getItemCount() > 0) {
				fApplicationCombo.setText(fApplicationCombo.getItem(0));
			}
		} else {
			fApplicationCombo.setText(application);
		}
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

		fWorkspaceCombo.setItems((String[]) items.toArray(new String[items.size()]));
		if (fWorkspaceCombo.getItemCount() > 0)
			fWorkspaceCombo.setText(items.get(0).toString());

		fClearWorkspaceCheck.setSelection(config.getAttribute(DOCLEAR, false));
		fAskClearCheck.setSelection(config.getAttribute(ASKCLEAR, true));
		fAskClearCheck.setEnabled(fClearWorkspaceCheck.getSelection());
	}
	
	protected void initializeJRESection(ILaunchConfiguration config) throws CoreException {
		String javaCommand = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, "javaw"); //$NON-NLS-1$
		fJavawButton.setSelection(javaCommand.equals("javaw")); //$NON-NLS-1$
		fJavaButton.setSelection(!fJavawButton.getSelection());
		
		fJreCombo.setItems(LauncherUtils.getVMInstallNames());
		String vmInstallName =
			config.getAttribute(VMINSTALL, LauncherUtils.getDefaultVMInstallName());
		fJreCombo.setText(vmInstallName);
		if (fJreCombo.getSelectionIndex() == -1)
			fJreCombo.setText(LauncherUtils.getDefaultVMInstallName());
	}
	
	protected void initializeVMArgsSection(ILaunchConfiguration config) throws CoreException {
		fVmArgsText.setText(config.getAttribute(VMARGS, ""));		 //$NON-NLS-1$
	}
	
	protected void initializeProgArgsSection(ILaunchConfiguration config) throws CoreException {
		fProgArgsText.setText(config.getAttribute(PROGARGS, ""));		 //$NON-NLS-1$
	}
	
	private void initializeBootstrapEntriesSection(ILaunchConfiguration config) throws CoreException {
		fBootstrap.setText(config.getAttribute(BOOTSTRAP_ENTRIES, "")); //$NON-NLS-1$
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(LOCATION + "0", LauncherUtils.getDefaultWorkspace()); //$NON-NLS-1$
		config.setAttribute(DOCLEAR, false);
		config.setAttribute(ASKCLEAR, true);
		config.setAttribute(PROGARGS, ""); //$NON-NLS-1$
		config.setAttribute(VMARGS,""); //$NON-NLS-1$
		config.setAttribute(BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
	}
	
	private void updateStatus() {
		updateStatus(getMoreSevere(fWorkspaceSelectionStatus, fJreSelectionStatus));
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		try {
			saveWorkspaceDataSection(config);
			saveApplicationSection(config);
			saveProductSection(config);
			saveJRESection(config);
			saveVMArgsSection(config);
			saveProgArgsSection(config);
			saveBootstrapEntriesSection(config);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected void saveWorkspaceDataSection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		config.setAttribute(LOCATION + String.valueOf(0), fWorkspaceCombo.getText());
		String[] items = fWorkspaceCombo.getItems();
		int nEntries = Math.min(items.length, 5);
		for (int i = 0; i < nEntries; i++) {
			config.setAttribute(LOCATION + String.valueOf(i+1), items[i]);
		}
	
		config.setAttribute(DOCLEAR, fClearWorkspaceCheck.getSelection());
		config.setAttribute(ASKCLEAR, fAskClearCheck.getSelection());
	}
	
	protected void saveJRESection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		
		String javaCommand = fJavawButton.getSelection() ? null : "java"; //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, javaCommand);
		
		if (fJreCombo.getSelectionIndex() == -1)
			return;

		String jre = fJreCombo.getText();
		if (config.getAttribute(VMINSTALL, (String) null) != null) {
			config.setAttribute(VMINSTALL, jre);
		} else {
			config.setAttribute(
				VMINSTALL,
				jre.equals(LauncherUtils.getDefaultVMInstallName()) ? null : jre);
		}
	}
	
	protected void saveVMArgsSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(VMARGS, fVmArgsText.getText().trim());
	}
	
	protected void saveProgArgsSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(PROGARGS, fProgArgsText.getText().trim());		
	}
	
	protected void saveBootstrapEntriesSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(BOOTSTRAP_ENTRIES, fBootstrap.getText().trim());
	}
	
	protected void saveProductSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USE_PRODUCT, fProductButton.getSelection());
		config.setAttribute(PRODUCT, fProductCombo.getText());
	}

	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		String text = fApplicationCombo.getText();
		String attribute = getApplicationAttribute();
		if (text.length() == 0 || text.equals(LauncherUtils.getDefaultApplicationName()))
			config.setAttribute(attribute, (String) null);
		else
			config.setAttribute(attribute, text);
	}

	private IPath chooseWorkspaceLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
		dialog.setFilterPath(fWorkspaceCombo.getText());
		dialog.setText(PDEPlugin.getResourceString("BasicLauncherTab.workspace.title")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("BasicLauncherTab.workspace.message")); //$NON-NLS-1$
		String res = dialog.open();
		return res != null ? new Path(res) : null;
	}

	private IStatus validateJRESelection() {
		if (fJreCombo.getSelectionIndex() == -1) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.noJRE")); //$NON-NLS-1$
		}
		return createStatus(IStatus.OK, ""); //$NON-NLS-1$
	}

	private IStatus validateWorkspaceSelection() {
		String location = fWorkspaceCombo.getText().trim();
		if (!Path.ROOT.isValidPath(location)) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.invalidWorkspace")); //$NON-NLS-1$
		}
		
		IPath curr = new Path(location);
		if (curr.segmentCount() == 0 && curr.getDevice() == null) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.noWorkspace")); //$NON-NLS-1$
		}

		return createStatus(IStatus.OK, ""); //$NON-NLS-1$
	}

	public String getName() {
		return PDEPlugin.getResourceString(KEY_NAME);
	}
	
	public Image getImage() {
		return fImage;
	}
	
}
