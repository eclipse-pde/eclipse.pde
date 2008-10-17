/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.p2;

import java.io.File;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.PropertyDialogAction;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * Wizard page allowing users to select which IUs they would like to download
 * and where they would like them to be downloaded to.
 * 
 * @since 3.4
 * @see P2TargetProvisionerWizard
 */
public class P2TargetProvisionerWizardPage extends WizardPage {

	/**
	 * Default location to download IUs to, will be created if it does not exist.
	 * Will be appended to the current workspace.
	 */
	private static final String DEFAULT_DIR_NAME = ".metadata/.plugins/org.eclipse.pde.core/target_plugins"; //$NON-NLS-1$;
	//	state constants
	private static final String USE_DEFAULT = "useDefault"; //$NON-NLS-1$
	private static final String CLEAR_CONTENTS = "clearContents"; //$NON-NLS-1$
	private static final String FOLDER_NAME = "folderName"; //$NON-NLS-1$

	static final IStatus BAD_IU_SELECTION = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), ProvisionerMessages.P2TargetProvisionerWizardPage_1);
	private static final IStatus DIR_DOES_NOT_EXIST = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), ProvisionerMessages.P2TargetProvisionerWizardPage_3);

	private IStatus fDirectoryStatus = Status.OK_STATUS;
	IStatus fSelectedIUStatus = BAD_IU_SELECTION;

	private String previousLocation;
	IInstallableUnit[] fUnits;

	private Link fLocationLink;
	private Text fInstallLocation;
	private Button fUseDefaultsButton;
	private Button fWorkspaceButton;
	private Button fFileSystemButton;
	private Button fClearContentsButton;

	AvailableIUGroup fAvailableIUGroup;
	Button fPropertiesButton;
	private IAction fPropertyAction;

	protected P2TargetProvisionerWizardPage(String pageName) {
		super(pageName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		createInstallFolderArea(composite);
		createAvailableIUArea(composite);

		setPageComplete(false);
		restoreWidgetState();
		setControl(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.P2_PROVISIONING_PAGE);
	}

	/**
	 * Create the UI area where the user will choose what directory to download
	 * bundles to.  The user can choose to use the default area and they can choose
	 * to delete the contents of the folder before downloading.
	 * 
	 * @param composite parent composite
	 */
	private void createInstallFolderArea(Composite composite) {
		Composite locationComp = new Composite(composite, SWT.NONE);
		locationComp.setLayout(new GridLayout(2, false));
		locationComp.setFont(composite.getFont());
		locationComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fUseDefaultsButton = new Button(locationComp, SWT.CHECK | SWT.RIGHT);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fUseDefaultsButton.setLayoutData(gd);
		fUseDefaultsButton.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_4);
		fUseDefaultsButton.setSelection(true);
		fUseDefaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.getSource();
				if (button.getSelection()) {
					fInstallLocation.setEnabled(false);
					fFileSystemButton.setEnabled(false);
					fWorkspaceButton.setEnabled(false);
					previousLocation = fInstallLocation.getText();
					fInstallLocation.setText(getDefaultDownloadLocation());
				} else {
					fInstallLocation.setEnabled(true);
					fFileSystemButton.setEnabled(true);
					fWorkspaceButton.setEnabled(true);
					fInstallLocation.setText(previousLocation);
				}

			}
		});
		fUseDefaultsButton.setSelection(true);

		fLocationLink = new Link(locationComp, SWT.NONE);
		fLocationLink.setText("<a>" + ProvisionerMessages.P2TargetProvisionerWizardPage_0 + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		fLocationLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					File file = getDownloadLocation(true);
					if (file != null && file.isDirectory())
						Program.launch(file.getCanonicalPath());
					else
						setErrorMessage(ProvisionerMessages.P2TargetProvisionerWizardPage_5);
				} catch (Exception ex) {
					setErrorMessage(ProvisionerMessages.P2TargetProvisionerWizardPage_6 + ex.getMessage());
				}
			}
		});

		fInstallLocation = new Text(locationComp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fInstallLocation.setLayoutData(gd);
		previousLocation = getDefaultDownloadLocation();
		fInstallLocation.setText(previousLocation);
		fInstallLocation.setEnabled(false);
		fInstallLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				File location = getDownloadLocation(false);
				if (location == null || (!location.exists() && !fUseDefaultsButton.getSelection())) {
					fDirectoryStatus = DIR_DOES_NOT_EXIST;
				} else {
					fDirectoryStatus = Status.OK_STATUS;
				}
				pageChanged();
			}
		});

		Composite buttons = new Composite(locationComp, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttons.setLayoutData(gd);

		fClearContentsButton = new Button(buttons, SWT.CHECK | SWT.RIGHT);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL);
		fClearContentsButton.setLayoutData(gd);
		fClearContentsButton.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_8);
		fClearContentsButton.setSelection(false);

		fWorkspaceButton = new Button(buttons, SWT.PUSH);
		fWorkspaceButton.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_2);
		fWorkspaceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleLocationWorkspaceButtonPressed();
			}
		});
		fWorkspaceButton.setEnabled(false);
		fWorkspaceButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fWorkspaceButton);

		fFileSystemButton = new Button(buttons, SWT.PUSH);
		fFileSystemButton.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_7);
		fFileSystemButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleLocationFileSystemButtonPressed();
			}
		});
		fFileSystemButton.setEnabled(false);
		fFileSystemButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fFileSystemButton);

	}

	/**
	 * Allow user to select from existing workspace locations.
	 */
	protected void handleLocationWorkspaceButtonPressed() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(fInstallLocation.getShell(), null, true, ProvisionerMessages.P2TargetProvisionerWizardPage_14);
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 0)
				return;
			IPath path = (IPath) result[0];
			fInstallLocation.setText(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(path).toOSString());
		}
	}

	/**
	 * Create the UI area where the user will be able to select which IUs they
	 * would like to download.  There will also be buttons to see properties for
	 * the selection and open the manage sites dialog.
	 * 
	 * @param composite parent composite
	 */
	private void createAvailableIUArea(Composite composite) {
		Group mainGroup = new Group(composite, SWT.NONE);
		mainGroup.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_9);
		mainGroup.setLayout(new GridLayout(2, false));
		mainGroup.setFont(composite.getFont());
		mainGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		fAvailableIUGroup = new AvailableIUGroup(mainGroup);
		fAvailableIUGroup.getCheckboxTreeViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				fUnits = fAvailableIUGroup.getCheckedLeafIUs();
				fPropertiesButton.setEnabled(fUnits.length > 0);
				if (fUnits.length > 0) {
					fSelectedIUStatus = Status.OK_STATUS;
				} else {
					fSelectedIUStatus = BAD_IU_SELECTION;
				}
				pageChanged();
			}
		});
		fAvailableIUGroup.setUseBoldFontForFilteredItems(true);
		GridData data = (GridData) fAvailableIUGroup.getStructuredViewer().getControl().getLayoutData();
		data.heightHint = 200;

		Composite iuButtonComp = new Composite(mainGroup, SWT.NONE);
		iuButtonComp.setLayout(new GridLayout(1, false));
		iuButtonComp.setFont(mainGroup.getFont());
		iuButtonComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		fPropertiesButton = new Button(iuButtonComp, SWT.PUSH);
		fPropertiesButton.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_10);
		fPropertiesButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPropertiesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fPropertyAction.run();
			}
		});
		fPropertyAction = new PropertyDialogAction(new SameShellProvider(getShell()), fAvailableIUGroup.getStructuredViewer());

		Button editReposButton = new Button(iuButtonComp, SWT.PUSH);
		editReposButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		editReposButton.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_11);
		editReposButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Policy.getDefault().getRepositoryManipulator().manipulateRepositories(getShell());
			}
		});
	}

	/**
	 * Checks if the page is complete, updating messages and finish button.
	 */
	void pageChanged() {
		if (fDirectoryStatus.getSeverity() == IStatus.ERROR) {
			setErrorMessage(fDirectoryStatus.getMessage());
			setPageComplete(false);
		} else if (fSelectedIUStatus.getSeverity() == IStatus.ERROR) {
			setErrorMessage(fSelectedIUStatus.getMessage());
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	/**
	 * Determines the default download folder location based on the workspace
	 * location.
	 */
	private String getDefaultDownloadLocation() {
		IPath path = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		if (path != null) {
			return new File(path.toFile(), DEFAULT_DIR_NAME).toString();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Open a directory dialog to select a folder.
	 */
	private void handleLocationFileSystemButtonPressed() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(fInstallLocation.getText());
		dialog.setText(ProvisionerMessages.P2TargetProvisionerWizardPage_16);
		dialog.setMessage(ProvisionerMessages.P2TargetProvisionerWizardPage_17);
		String result = dialog.open();
		if (result != null)
			fInstallLocation.setText(result);
	}

	/**
	 * Save the state of the widgets select, for successive invocations of the wizard
	 */
	void saveWidgetState() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			if (fUseDefaultsButton.getSelection()) {
				settings.put(USE_DEFAULT, true);
			} else {
				settings.put(USE_DEFAULT, false);
				settings.put(FOLDER_NAME, fInstallLocation.getText());
			}
			settings.put(CLEAR_CONTENTS, fClearContentsButton.getSelection());
		}
	}

	/**
	 * Restores the state of the wizard from previous invocations
	 */
	private void restoreWidgetState() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String location = settings.get(FOLDER_NAME);
			if (location != null && location.length() > 0) {
				previousLocation = location;
			}
			String useDefaults = settings.get(USE_DEFAULT);
			if (useDefaults != null) {
				boolean useDefaultsBoolean = Boolean.valueOf(useDefaults).booleanValue();
				fUseDefaultsButton.setSelection(useDefaultsBoolean);
				if (!useDefaultsBoolean) {
					fInstallLocation.setText(previousLocation);
					fInstallLocation.setEnabled(true);
					fFileSystemButton.setEnabled(true);
					fWorkspaceButton.setEnabled(true);

				}
			}
			fClearContentsButton.setSelection(Boolean.valueOf(settings.get(CLEAR_CONTENTS)).booleanValue());
		}
	}

	/**
	 * @return the list of installable units to download on finish
	 */
	public IInstallableUnit[] getUnits() {
		return fUnits;
	}

	/**
	 * @param createIfDefault whether to create the directory structure if it doesn't exist, only occurs if default is used
	 * @return the location where installable units should be downloaded to
	 */
	public File getDownloadLocation(boolean createIfDefault) {
		if (fInstallLocation.getText().trim().length() > 0) {
			File file = new File(fInstallLocation.getText());
			if (createIfDefault && fUseDefaultsButton.getSelection() && !file.isDirectory()) {
				file.mkdirs();
			}
			return file;
		}
		return null;
	}

	/**
	 * @return whether contents of the download location should be deleted before starting the download
	 */
	public boolean isClearContentsBeforeDownloading() {
		return fClearContentsButton.getSelection();
	}

}
