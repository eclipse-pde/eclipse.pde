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

import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.core.plugin.*;
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

	private static final String KEY_NAME = "BasicLauncherTab.name";
	
	private Combo fWorkspaceCombo;
	private Button fBrowseButton;
	private Button fClearWorkspaceCheck;
	private Button fAskClearCheck;
	private Combo fJreCombo;
	private Text fClasspathText;
	private Text fVmArgsText;
	private Text fProgArgsText;
	private Button fDefaultsButton;
	private Image fImage;
	private String fCurrentClasspath;

	private IStatus fJreSelectionStatus;
	private IStatus fWorkspaceSelectionStatus;
	
	private boolean fBlockChanges = false;

	protected Combo fApplicationCombo;

	public BasicLauncherTab() {
		fJreSelectionStatus = createStatus(IStatus.OK, "");
		fWorkspaceSelectionStatus = createStatus(IStatus.OK, "");
		fImage = PDEPluginImages.DESC_ARGUMENT_TAB.createImage();
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
		createCommandLineSettingsSection(composite);
		createDefaultsButton(composite);
		
		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}
	
	protected void createDefaultsButton(Composite parent) {
		fDefaultsButton = new Button(parent, SWT.PUSH);
		fDefaultsButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.restore"));
		fDefaultsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fDefaultsButton);
		fDefaultsButton.addSelectionListener(new SelectionAdapter() {
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
		fBrowseButton.setText(PDEPlugin.getResourceString("BasicLauncherTab.browse"));
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseWorkspaceLocation();
				if (chosen != null) {
					fWorkspaceCombo.setText(chosen.toOSString());
					updateStatus();
				}
			}
		});
		SWTUtil.setButtonDimensionHint(fBrowseButton);

		fClearWorkspaceCheck = new Button(group, SWT.CHECK);
		fClearWorkspaceCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.clear"));
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
		fAskClearCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.askClear"));
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
		group.setText(PDEPlugin.getResourceString("BasicLauncherTab.commandLineSettings"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createApplicationSection(group);
		createJRESection(group);
		createVMArgsSection(group);
		createProgArgsSection(group);
		createDevEntriesSection(group);
	}
	
	protected void createApplicationSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("JUnitArgumentsTab.applicationName"));
		
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
		TreeSet result = new TreeSet();
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.applications")) {
					String id = extensions[j].getPluginBase().getId() + "." + extensions[j].getId();
					if (id != null && !id.startsWith("org.eclipse.pde.junit.runtime")){
						result.add(id);
					}
				}
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	protected String getApplicationAttribute() {
		return APPLICATION;
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
		
		fJreCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fJreCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fJreCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fJreSelectionStatus = validateJRESelection();
				updateStatus();
			}
		});
		
		Button button = new Button(composite, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("BasicLauncherTab.installedJREs"));
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
	
	protected void createVMArgsSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.vmArgs"));
		
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
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.programArgs"));

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
	
	protected void createDevEntriesSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BasicLauncherTab.classpath"));
		
		fClasspathText = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fClasspathText.setLayoutData(gd);
		fClasspathText.addModifyListener(new ModifyListener() {
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
			initializeApplicationSection(config);
			initializeVMArgsSection(config);
			initializeProgArgsSection(config);
			initializeDevEntriesSection(config);
				
			fWorkspaceSelectionStatus = validateWorkspaceSelection();
			fJreSelectionStatus = validateJRESelection();
			updateStatus();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			fBlockChanges = false;
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
				new StringTokenizer(config.getAttribute(PROGARGS, ""));
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equals("-application") && tokenizer.hasMoreTokens()) {
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
		fJreCombo.setItems(LauncherUtils.getVMInstallNames());
		String vmInstallName =
			config.getAttribute(VMINSTALL, LauncherUtils.getDefaultVMInstallName());
		fJreCombo.setText(vmInstallName);
		if (fJreCombo.getSelectionIndex() == -1)
			fJreCombo.setText(LauncherUtils.getDefaultVMInstallName());
	}
	
	protected void initializeVMArgsSection(ILaunchConfiguration config) throws CoreException {
		fVmArgsText.setText(config.getAttribute(VMARGS, ""));		
	}
	
	protected void initializeProgArgsSection(ILaunchConfiguration config) throws CoreException {
		fProgArgsText.setText(config.getAttribute(PROGARGS, LauncherUtils.getDefaultProgramArguments()));		
	}
	
	protected void initializeDevEntriesSection(ILaunchConfiguration config) throws CoreException {
		fClasspathText.setText(config.getAttribute(CLASSPATH_ENTRIES, getClasspathEntries()));		
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(LOCATION + "0", LauncherUtils.getDefaultWorkspace());
		config.setAttribute(DOCLEAR, false);
		config.setAttribute(ASKCLEAR, true);
		config.setAttribute(PROGARGS, LauncherUtils.getDefaultProgramArguments());
		config.setAttribute(VMARGS,"");
	}
	
	private String getClasspathEntries() {
		if (fCurrentClasspath == null)
			fCurrentClasspath = LauncherUtils.getBuildOutputFolders();
		return fCurrentClasspath;
	}

	protected void doRestoreDefaults() {
		fProgArgsText.setText(LauncherUtils.getDefaultProgramArguments());
		fVmArgsText.setText("");
		fWorkspaceCombo.setText(LauncherUtils.getDefaultWorkspace());
		fClasspathText.setText(getClasspathEntries());
		fClearWorkspaceCheck.setSelection(false);
		fAskClearCheck.setSelection(true);
		fAskClearCheck.setEnabled(false);
		fJreCombo.setText(LauncherUtils.getDefaultVMInstallName());	
		fApplicationCombo.setText(LauncherUtils.getDefaultApplicationName());
	}

	private void updateStatus() {
		updateStatus(
			getMoreSevere(fWorkspaceSelectionStatus, fJreSelectionStatus));
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		try {
			saveWorkspaceDataSection(config);
			saveApplicationSection(config);
			saveJRESection(config);
			saveVMArgsSection(config);
			saveProgArgsSection(config);
			saveDevEntriesSection(config);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected void saveWorkspaceDataSection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		config.setAttribute(LOCATION + String.valueOf(0), fWorkspaceCombo.getText());
		if (fWorkspaceCombo.getItemCount() > 1) {
			String[] items = fWorkspaceCombo.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				config.setAttribute(LOCATION + String.valueOf(i + 1), items[i]);
			}
		}

		config.setAttribute(DOCLEAR, fClearWorkspaceCheck.getSelection());
		config.setAttribute(ASKCLEAR, fAskClearCheck.getSelection());
	}
	
	protected void saveJRESection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
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
	
	protected void saveDevEntriesSection(ILaunchConfigurationWorkingCopy config)
		throws CoreException {
		String classpath = fClasspathText.getText().trim();
		if (config.getAttribute(CLASSPATH_ENTRIES, (String) null) != null) {
			config.setAttribute(CLASSPATH_ENTRIES, classpath);
		} else {
			config.setAttribute(
				CLASSPATH_ENTRIES,
				classpath.equals(getClasspathEntries()) ? null : classpath);
		}
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
		dialog.setText(PDEPlugin.getResourceString("BasicLauncherTab.workspace.title"));
		dialog.setMessage(PDEPlugin.getResourceString("BasicLauncherTab.workspace.message"));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	private IStatus validateJRESelection() {
		if (fJreCombo.getSelectionIndex() == -1) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString("BasicLauncherTab.noJRE"));
		}
		return createStatus(IStatus.OK, "");
	}

	private IStatus validateWorkspaceSelection() {
		String location = fWorkspaceCombo.getText().trim();
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
		return fImage;
	}
	
}
