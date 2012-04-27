/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *     Les Jones <lesojones@gmail.com> - Bug 214457
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.TreeSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.util.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewLibraryPluginCreationPage extends WizardNewProjectCreationPage {

	private Label fEELabel;
	private Button fExeEnvButton;
	private Combo fEEChoice;

	private final static String NO_EXECUTION_ENVIRONMENT = PDEUIMessages.PluginContentPage_noEE;

	class PropertiesListener implements ModifyListener {
		private boolean fBlocked = false;

		private boolean fChanged = false;

		public boolean isChanged() {
			return fChanged;
		}

		public void modifyText(ModifyEvent e) {
			if (!fBlocked) {
				fChanged = true;
				setPageComplete(validatePage());
			}
		}

		public void setBlocked(boolean blocked) {
			this.fBlocked = blocked;
		}
	}

	private LibraryPluginFieldData fData;
	protected NewLibraryPluginCreationPage fMainPage;
	protected IProjectProvider fProjectProvider;
	protected PropertiesListener fPropertiesListener = new PropertiesListener();
	protected Text fIdText;
	protected Text fNameText;
	protected Text fProviderText;
	protected Text fVersionText;
	private Button fEclipseButton;
	private Button fOSGIButton;
	private Combo fOSGiCombo;
	private Combo fTargetCombo;
	protected Button fJarredCheck;
	protected Button fFindDependencies;
	private Button fUpdateRefsCheck;
	private IStructuredSelection fSelection;

	public NewLibraryPluginCreationPage(String pageName, LibraryPluginFieldData data, IStructuredSelection selection) {
		super(pageName);
		fData = data;
		fSelection = selection;
		setTitle(PDEUIMessages.NewLibraryPluginCreationPage_title);
		setDescription(PDEUIMessages.NewLibraryPluginCreationPage_desc);
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite) getControl();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		control.setLayout(layout);

		createPluginPropertiesGroup(control);

		createFormatGroup(control);

		createWorkingSetGroup(control, fSelection, new String[] {"org.eclipse.jdt.ui.JavaWorkingSetPage", //$NON-NLS-1$
				"org.eclipse.pde.ui.pluginWorkingSet", //$NON-NLS-1$
				"org.eclipse.ui.resourceWorkingSetPage"}); //$NON-NLS-1$

		updateRuntimeDependency();

		Dialog.applyDialogFont(control);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.NEW_LIBRARY_PROJECT_STRUCTURE_PAGE);
		setControl(control);
	}

	private void createFormatGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.NewProjectCreationPage_target);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.NewProjectCreationPage_ptarget);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		fEclipseButton = createButton(group, SWT.RADIO, 1, 30);
		fEclipseButton.setText(PDEUIMessages.NewProjectCreationPage_pDependsOnRuntime);
		fEclipseButton.setSelection(fData.getOSGiFramework() == null);
		fEclipseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateRuntimeDependency();
			}
		});

		fTargetCombo = new Combo(group, SWT.READ_ONLY | SWT.SINGLE);
		fTargetCombo.setItems(new String[] {PDEUIMessages.NewProjectCreationPage_target_version_range_3_5, ICoreConstants.TARGET34, ICoreConstants.TARGET33, ICoreConstants.TARGET32, ICoreConstants.TARGET31});
		fTargetCombo.setText(PDEUIMessages.NewProjectCreationPage_target_version_range_3_5);
		if (PDECore.getDefault().areModelsInitialized()) {
			String text = TargetPlatformHelper.getTargetVersionString();
			if (fTargetCombo.indexOf(text) >= 0) {
				fTargetCombo.setText(text);
			}
		}

		fOSGIButton = createButton(group, SWT.RADIO, 1, 30);
		fOSGIButton.setText(PDEUIMessages.NewProjectCreationPage_pPureOSGi);
		fOSGIButton.setSelection(fData.getOSGiFramework() != null);

		fOSGiCombo = new Combo(group, SWT.READ_ONLY | SWT.SINGLE);
		fOSGiCombo.setItems(new String[] {ICoreConstants.EQUINOX, PDEUIMessages.NewProjectCreationPage_standard});
		fOSGiCombo.setText(ICoreConstants.EQUINOX);

		fJarredCheck = new Button(group, SWT.CHECK);
		fJarredCheck.setText(PDEUIMessages.NewLibraryPluginCreationPage_jarred);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fJarredCheck.setLayoutData(gd);
		// Defaults to checked for plug-ins 3.1 and greater
		fJarredCheck.setSelection(true);
		fUpdateRefsCheck = new Button(group, SWT.CHECK);
		fUpdateRefsCheck.setText(PDEUIMessages.NewLibraryPluginCreationPage_UpdateReferences_button);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fUpdateRefsCheck.setLayoutData(gd);
		//enable by default
		fUpdateRefsCheck.setSelection(false);
		fUpdateRefsCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (getNextPage() instanceof NewLibraryPluginCreationUpdateRefPage) {
					((NewLibraryPluginCreationUpdateRefPage) getNextPage()).setEnable(fUpdateRefsCheck.getSelection());
				}
				getContainer().updateButtons();
			}

		});
	}

	/**
	 * Creates all of the EE widgets
	 * @param container
	 */
	private void createExecutionEnvironmentControls(Composite container) {
		// Create label
		fEELabel = new Label(container, SWT.NONE);
		fEELabel.setText(PDEUIMessages.NewProjectCreationPage_executionEnvironments_label);

		// Create combo
		fEEChoice = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		fEEChoice.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Gather EEs 
		IExecutionEnvironment[] exeEnvs = VMUtil.getExecutionEnvironments();
		TreeSet availableEEs = new TreeSet();
		for (int i = 0; i < exeEnvs.length; i++) {
			availableEEs.add(exeEnvs[i].getId());
		}
		availableEEs.add(NO_EXECUTION_ENVIRONMENT);

		// Set data 
		fEEChoice.setItems((String[]) availableEEs.toArray(new String[availableEEs.size() - 1]));
		fEEChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				validatePage();
			}
		});

		// Set default EE based on strict match to default VM
		IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();
		String[] EEChoices = fEEChoice.getItems();
		for (int i = 0; i < EEChoices.length; i++) {
			if (!EEChoices[i].equals(NO_EXECUTION_ENVIRONMENT)) {
				if (VMUtil.getExecutionEnvironment(EEChoices[i]).isStrictlyCompatible(defaultVM)) {
					fEEChoice.select(i);
					break;
				}
			}
		}

		// Create button
		fExeEnvButton = new Button(container, SWT.PUSH);
		fExeEnvButton.setLayoutData(new GridData());
		fExeEnvButton.setText(PDEUIMessages.NewProjectCreationPage_environmentsButton);
		fExeEnvButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				PreferencesUtil.createPreferenceDialogOn(getShell(), "org.eclipse.jdt.debug.ui.jreProfiles", //$NON-NLS-1$
						new String[] {"org.eclipse.jdt.debug.ui.jreProfiles"}, null).open(); //$NON-NLS-1$ 
			}
		});
	}

	private void createPluginPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(3, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEUIMessages.NewLibraryPluginCreationPage_pGroup);

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pid);
		fIdText = createText(propertiesGroup, fPropertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pversion);
		fVersionText = createText(propertiesGroup, fPropertiesListener, 2);
		fPropertiesListener.setBlocked(true);
		fVersionText.setText("1.0.0"); //$NON-NLS-1$
		fPropertiesListener.setBlocked(false);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pname);
		fNameText = createText(propertiesGroup, fPropertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pprovider);
		fProviderText = createText(propertiesGroup, fPropertiesListener, 2);

		fFindDependencies = new Button(propertiesGroup, SWT.CHECK);
		fFindDependencies.setText(PDEUIMessages.NewLibraryPluginCreationPage_pdependencies);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		fFindDependencies.setLayoutData(data);

		createExecutionEnvironmentControls(propertiesGroup);
	}

	protected Text createText(Composite parent, ModifyListener listener, int horizSpan) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = horizSpan;
		text.setLayoutData(data);
		text.addModifyListener(listener);
		return text;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		updateData();
		return super.getNextPage();
	}

	protected boolean isVersionValid(String version) {
		return VersionUtil.validateVersion(version).getSeverity() == IStatus.OK;
	}

	public void updateData() {
		fData.setSimple(false);
		fData.setSourceFolderName(null);
		fData.setOutputFolderName(null);
		fData.setLegacy(false);

		// No project structure changes since 3.5, mark as latest version (though using any constant 3.5 or greater is equivalent)
		if (fTargetCombo.getText().equals(PDEUIMessages.NewProjectCreationPage_target_version_range_3_5)) {
			fData.setTargetVersion(ICoreConstants.TARGET_VERSION_LATEST);
		} else {
			fData.setTargetVersion(fTargetCombo.getText());
		}

		// No longer support 3.0 non-osgi bundles in wizard
		fData.setHasBundleStructure(true);

		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderText.getText().trim());
		fData.setLibraryName(null);
		fData.setOSGiFramework(fOSGIButton.getSelection() ? fOSGiCombo.getText() : null);
		fData.setUnzipLibraries(fJarredCheck.isEnabled() && fJarredCheck.getSelection());
		fData.setFindDependencies(fFindDependencies.getSelection());
		fData.setUpdateReferences(fUpdateRefsCheck.getSelection());
		fData.setWorkingSets(getSelectedWorkingSets());

		PluginFieldData data = fData;
		data.setClassname(null);
		data.setUIPlugin(false);
		data.setDoGenerateClass(false);
		data.setRCPApplicationPlugin(false);

		if (fEEChoice.isEnabled() && !fEEChoice.getText().equals(NO_EXECUTION_ENVIRONMENT)) {
			fData.setExecutionEnvironment(fEEChoice.getText().trim());
		} else {
			fData.setExecutionEnvironment(null);
		}
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEUIMessages.NewLibraryPluginCreationPage_noid;

		if (!IdUtil.isValidCompositeID3_0(id)) {
			return PDEUIMessages.NewLibraryPluginCreationPage_invalidId;
		}
		return null;
	}

	protected boolean validatePage() {
		String id = IdUtil.getValidId(getProjectName());

		// properties group
		if (!fPropertiesListener.isChanged() && fIdText != null) {
			fPropertiesListener.setBlocked(true);
			fIdText.setText(id);
			fNameText.setText(IdUtil.getValidName(id));
			fPropertiesListener.setBlocked(false);
		}

		if (!super.validatePage())
			return false;
		setMessage(null);

		String errorMessage = validateProperties();
		if (errorMessage == null) {
			String eeid = fEEChoice.getText();
			if (fEEChoice.isEnabled()) {
				IExecutionEnvironment ee = VMUtil.getExecutionEnvironment(eeid);
				if (ee != null && ee.getCompatibleVMs().length == 0) {
					errorMessage = PDEUIMessages.NewProjectCreationPage_invalidEE;
				}
			}
		}
		setErrorMessage(errorMessage);
		return errorMessage == null;
	}

	protected String validateProperties() {
		String errorMessage = validateId();
		if (errorMessage != null)
			return errorMessage;

		if (fVersionText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.NewLibraryPluginCreationPage_noversion;
		} else if (!isVersionValid(fVersionText.getText().trim())) {
			errorMessage = PDEUIMessages.ContentPage_badversion;
		} else if (fNameText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.NewLibraryPluginCreationPage_noname;
		}

		if (errorMessage != null)
			return errorMessage;

		return errorMessage;
	}

	private void updateRuntimeDependency() {
		boolean depends = fEclipseButton.getSelection();
		fTargetCombo.setEnabled(depends);
		fOSGiCombo.setEnabled(!depends);
	}

	private Button createButton(Composite container, int style, int span, int indent) {
		Button button = new Button(container, style);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		button.setLayoutData(gd);
		return button;
	}

	public void addSelectionListener(SelectionListener listener) {
		if (fUpdateRefsCheck != null) {
			fUpdateRefsCheck.addSelectionListener(listener);
		}
	}

	public void removeSelectionListener(SelectionListener listener) {
		if (fUpdateRefsCheck != null) {
			fUpdateRefsCheck.removeSelectionListener(listener);
		}
	}

	public boolean canFlipToNextPage() {
		return isPageComplete() && fUpdateRefsCheck.getSelection();
	}
}
