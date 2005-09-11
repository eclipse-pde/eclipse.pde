/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.internal.ui.launcher.ILauncherSettings;
import org.eclipse.pde.internal.ui.launcher.InstalledJREsPreferenceNode;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class MainTab
	extends AbstractLauncherTab
	implements ILauncherSettings {

	private Combo fWorkspaceCombo;
	private Button fFileSystemButton;
	private Button fClearWorkspaceCheck;
	private Button fAskClearCheck;
	private Combo fJreCombo;
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
	private Button fWorkspaceButton;
	private Button fVariablesButton;

	public MainTab() {
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
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createWorkspaceDataSection(composite);
		createProgramSection(composite);
		createCommandLineSettingsSection(composite);
		
		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}
	
	private void createProgramSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEUIMessages.BasicLauncherTab_programToRun); 
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		createProductSection(group);		
		createApplicationSection(group);
	}

	protected void createWorkspaceDataSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEUIMessages.BasicLauncherTab_workspace); 
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.BasicLauncherTab_location); 

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
		
		Composite buttons = new Composite(group, SWT.NONE);
		layout = new GridLayout(4, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttons.setLayoutData(gd);
		
		fClearWorkspaceCheck = new Button(buttons, SWT.CHECK);
		fClearWorkspaceCheck.setText(PDEUIMessages.BasicLauncherTab_clear);
		fClearWorkspaceCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fClearWorkspaceCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAskClearCheck.setEnabled(fClearWorkspaceCheck.getSelection());
				updateLaunchConfigurationDialog();
			}
		});

		fWorkspaceButton = new Button(buttons, SWT.PUSH);
		fWorkspaceButton.setText("Workspace..."); 
		fWorkspaceButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fWorkspaceButton.addSelectionListener(new SelectionAdapter() {
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
		SWTUtil.setButtonDimensionHint(fWorkspaceButton);

		fFileSystemButton = new Button(buttons, SWT.PUSH);
		fFileSystemButton.setText("File System..."); 
		fFileSystemButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fFileSystemButton.addSelectionListener(new SelectionAdapter() {
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
		SWTUtil.setButtonDimensionHint(fFileSystemButton);

		fVariablesButton = new Button(buttons, SWT.PUSH);
		fVariablesButton.setText("Variables..."); 
		fVariablesButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fVariablesButton.addSelectionListener(new SelectionAdapter() {
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
		SWTUtil.setButtonDimensionHint(fVariablesButton);

		
		fAskClearCheck = new Button(group, SWT.CHECK);
		fAskClearCheck.setText(PDEUIMessages.BasicLauncherTab_askClear); 
		gd = new GridData();
		gd.horizontalSpan = 2;
		fAskClearCheck.setLayoutData(gd);
		fAskClearCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});	
	}
	
	protected void createCommandLineSettingsSection(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText(PDEUIMessages.MainTab_jreSection); 
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createJavaExecutableSection(group);
		createJRESection(group);
		createBootstrapEntriesSection(group);
	}
	
	protected void createProductSection(Composite parent) {
		fProductButton = new Button(parent, SWT.RADIO);
		fProductButton.setText(PDEUIMessages.BasicLauncherTab_runProduct); 
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
		fApplicationButton.setText(PDEUIMessages.BasicLauncherTab_runApplication); 
			
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
		label.setText(PDEUIMessages.BasicLauncherTab_jre); 

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
		button.setText(PDEUIMessages.BasicLauncherTab_installedJREs); 
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
		label.setText(PDEUIMessages.BasicLauncherTab_javaExec); 

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		layout.horizontalSpacing = 20;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fJavawButton = new Button(composite, SWT.RADIO);
		fJavawButton.setText(PDEUIMessages.BasicLauncherTab_javaExecDefault); // 
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
			
	private void createBootstrapEntriesSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEUIMessages.BasicLauncherTab_bootstrap); 
		
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
		String product = TargetPlatform.getDefaultProduct();
		if (product != null) {
			config.setAttribute(USE_PRODUCT, true);
			config.setAttribute(PRODUCT, product); 
		}
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
		dialog.setText(PDEUIMessages.BasicLauncherTab_workspace_title); 
		dialog.setMessage(PDEUIMessages.BasicLauncherTab_workspace_message); 
		String res = dialog.open();
		return res != null ? new Path(res) : null;
	}

	private IStatus validateJRESelection() {
		if (fJreCombo.getSelectionIndex() == -1) {
			return createStatus(
				IStatus.ERROR,
				PDEUIMessages.BasicLauncherTab_noJRE); 
		}
		return createStatus(IStatus.OK, ""); //$NON-NLS-1$
	}

	private IStatus validateWorkspaceSelection() {
		String location = fWorkspaceCombo.getText().trim();
		if (!Path.ROOT.isValidPath(location)) {
			return createStatus(
				IStatus.ERROR,
				PDEUIMessages.BasicLauncherTab_invalidWorkspace); 
		}
		
		IPath curr = new Path(location);
		if (curr.segmentCount() == 0 && curr.getDevice() == null) {
			return createStatus(
				IStatus.ERROR,
				PDEUIMessages.BasicLauncherTab_noWorkspace); 
		}

		return createStatus(IStatus.OK, ""); //$NON-NLS-1$
	}

	public String getName() {
		return PDEUIMessages.BasicLauncherTab_name;
	}
	
	public Image getImage() {
		return fImage;
	}
	
}
