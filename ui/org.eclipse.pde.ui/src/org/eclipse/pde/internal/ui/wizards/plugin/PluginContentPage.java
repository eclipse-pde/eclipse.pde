/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.Locale;
import java.util.TreeSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Content wizard page for the New Plugin Project wizard (page 2)
 */
public class PluginContentPage extends ContentPage {

	private Text fClassText;
	protected Button fGenerateActivator;
	protected Button fUIPlugin;
	private Label fClassLabel;
	private Label fEELabel;
	private Button fExeEnvButton;
	private Combo fEEChoice;
	private Group fRCPGroup;
	protected Button fYesButton;
	protected Button fNoButton;

	/**
	 * Button to enable API analysis for the project during project creation
	 */
	private Button fApiAnalysisButton;

	/**
	 * Dialog settings constants
	 */
	private final static String S_GENERATE_ACTIVATOR = "generateActivator"; //$NON-NLS-1$
	private final static String S_UI_PLUGIN = "uiPlugin"; //$NON-NLS-1$
	private final static String S_RCP_PLUGIN = "rcpPlugin"; //$NON-NLS-1$
	private final static String S_API_ANALYSIS = "apiAnalysis"; //$NON-NLS-1$

	protected final static int P_CLASS_GROUP = 2;
	private final static String NO_EXECUTION_ENVIRONMENT = PDEUIMessages.PluginContentPage_noEE;

	/**
	 * default tText modify listener
	 */
	private ModifyListener classListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (fInitialized)
				fChangedGroups |= P_CLASS_GROUP;
			validatePage();
		}
	};

	/**
	 * Constructor
	 * @param pageName
	 * @param provider
	 * @param page
	 * @param data
	 */
	public PluginContentPage(String pageName, IProjectProvider provider, NewProjectCreationPage page, AbstractFieldData data) {
		super(pageName, provider, page, data);
		setTitle(PDEUIMessages.ContentPage_title);
		setDescription(PDEUIMessages.ContentPage_desc);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		createPluginPropertiesGroup(container);
		createPluginClassGroup(container);
		createRCPQuestion(container, 2);

		Dialog.applyDialogFont(container);
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_PROJECT_REQUIRED_DATA);
	}

	/**
	 * Creates all of the plugin properties widgets
	 * @param container
	 */
	private void createPluginPropertiesGroup(Composite container) {
		Group propertiesGroup = SWTFactory.createGroup(container, PDEUIMessages.ContentPage_pGroup, 3, 1, GridData.FILL_HORIZONTAL);

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pid);
		fIdText = createText(propertiesGroup, propertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pversion);
		fVersionText = createText(propertiesGroup, propertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pname);
		fNameText = createText(propertiesGroup, propertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pprovider);
		fProviderCombo = createProviderCombo(propertiesGroup, propertiesListener, 2);

		createExecutionEnvironmentControls(propertiesGroup);
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

	/**
	 * Creates all of the plugin options widgets
	 * @param container
	 */
	protected void createPluginClassGroup(Composite container) {
		Group classGroup = SWTFactory.createGroup(container, PDEUIMessages.ContentPage_pClassGroup, 2, 1, GridData.FILL_HORIZONTAL);

		IDialogSettings settings = getDialogSettings();

		fGenerateActivator = SWTFactory.createCheckButton(classGroup, PDEUIMessages.ContentPage_generate, null, (settings != null) ? !settings.getBoolean(S_GENERATE_ACTIVATOR) : true, 2);
		fGenerateActivator.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fClassLabel.setEnabled(fGenerateActivator.getSelection());
				fClassText.setEnabled(fGenerateActivator.getSelection());
				updateData();
				validatePage();
			}
		});

		fClassLabel = new Label(classGroup, SWT.NONE);
		fClassLabel.setText(PDEUIMessages.ContentPage_classname);
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		fClassLabel.setLayoutData(gd);
		fClassText = createText(classGroup, classListener);

		fUIPlugin = SWTFactory.createCheckButton(classGroup, PDEUIMessages.ContentPage_uicontribution, null, (settings != null) ? !settings.getBoolean(S_UI_PLUGIN) : true, 2);
		fUIPlugin.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateData();
				validatePage();
			}
		});

		fApiAnalysisButton = SWTFactory.createCheckButton(classGroup, PDEUIMessages.PluginContentPage_enable_api_analysis, null, false, 2);
		fApiAnalysisButton.setSelection((settings != null) ? settings.getBoolean(S_API_ANALYSIS) : false);
		fApiAnalysisButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateData();
				validatePage();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#updateData()
	 */
	public void updateData() {
		super.updateData();
		PluginFieldData data = (PluginFieldData) fData;
		data.setClassname(fClassText.getText().trim());
		data.setUIPlugin(fUIPlugin.getSelection());
		data.setDoGenerateClass(fGenerateActivator.getSelection());
		data.setRCPApplicationPlugin(!fData.isSimple() && !isPureOSGi() && fYesButton.getSelection());
		// Don't turn on API analysis if disabled (no java project available)
		data.setEnableAPITooling(fApiAnalysisButton.isEnabled() && fApiAnalysisButton.getSelection());
		if (fEEChoice.isEnabled() && !fEEChoice.getText().equals(NO_EXECUTION_ENVIRONMENT)) {
			fData.setExecutionEnvironment(fEEChoice.getText().trim());
		} else {
			fData.setExecutionEnvironment(null);
		}
	}

	/**
	 * Creates the RCP questions 
	 * @param parent
	 * @param horizontalSpan
	 */
	protected void createRCPQuestion(Composite parent, int horizontalSpan) {
		fRCPGroup = SWTFactory.createGroup(parent, PDEUIMessages.PluginContentPage_rcpGroup, 2, 1, GridData.FILL_HORIZONTAL);
		Composite comp = new Composite(fRCPGroup, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		comp.setLayoutData(gd);

		Label label = new Label(comp, SWT.NONE);
		label.setText(PDEUIMessages.PluginContentPage_appQuestion);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IDialogSettings settings = getDialogSettings();
		boolean rcpApp = (settings != null) ? settings.getBoolean(S_RCP_PLUGIN) : false;

		fYesButton = new Button(comp, SWT.RADIO);
		fYesButton.setText(PDEUIMessages.PluginContentPage_yes);
		fYesButton.setSelection(rcpApp);
		gd = new GridData();
		gd.widthHint = SWTFactory.getButtonWidthHint(fYesButton, 50);
		fYesButton.setLayoutData(gd);
		fYesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateData();
				getContainer().updateButtons();
			}
		});

		fNoButton = new Button(comp, SWT.RADIO);
		fNoButton.setText(PDEUIMessages.PluginContentPage_no);
		fNoButton.setSelection(!rcpApp);
		gd = new GridData();
		gd.widthHint = SWTFactory.getButtonWidthHint(fNoButton, 50);
		fNoButton.setLayoutData(gd);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			fMainPage.updateData();
			fGenerateActivator.setSelection(!fData.isSimple());
			fGenerateActivator.setEnabled(!fData.isSimple());
			fClassLabel.setEnabled(!fData.isSimple() && fGenerateActivator.getSelection());
			fClassText.setEnabled(!fData.isSimple() && fGenerateActivator.getSelection());
			boolean wasUIPluginEnabled = fUIPlugin.isEnabled();
			fUIPlugin.setEnabled(!fData.isSimple() && !isPureOSGi());
			// if fUIPlugin is disabled, set selection to false
			if (!fUIPlugin.isEnabled()) {
				fUIPlugin.setSelection(false);
			}
			// if the fUIPlugin was disabled and is now enabled, then set the selection to true
			else if (!wasUIPluginEnabled) {
				fUIPlugin.setSelection(true);
			}

			// plugin class group
			if (((fChangedGroups & P_CLASS_GROUP) == 0)) {
				int oldfChanged = fChangedGroups;
				fClassText.setText(computeId().replaceAll("-", "_").toLowerCase(Locale.ENGLISH) + ".Activator"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				fChangedGroups = oldfChanged;
			}

			boolean allowEESelection = !fData.isSimple() && fData.hasBundleStructure();
			fEELabel.setEnabled(allowEESelection);
			fEEChoice.setEnabled(allowEESelection);
			fExeEnvButton.setEnabled(allowEESelection);
			// API Tools only works for osgi bundles with java natures
			fApiAnalysisButton.setEnabled(allowEESelection);

			fRCPGroup.setVisible(!fData.isSimple() && !isPureOSGi());
		}
		super.setVisible(visible);
	}

	/**
	 * @return if the field data is using the OSGi framework
	 */
	private boolean isPureOSGi() {
		return ((PluginFieldData) fData).getOSGiFramework() != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#validatePage()
	 */
	protected void validatePage() {
		String errorMessage = validateProperties();
		if (errorMessage == null && fGenerateActivator.getSelection()) {
			IStatus status = JavaConventions.validateJavaTypeName(fClassText.getText().trim(), PDEJavaHelper.getJavaSourceLevel(null), PDEJavaHelper.getJavaComplianceLevel(null));
			if (status.getSeverity() == IStatus.ERROR) {
				errorMessage = status.getMessage();
			} else if (status.getSeverity() == IStatus.WARNING) {
				setMessage(status.getMessage(), IMessageProvider.WARNING);
			}
		}
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
		setPageComplete(errorMessage == null);
	}

	/**
	 * Saves the current state of widgets of interest in the dialog settings for the wizard
	 * @param settings
	 */
	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		settings.put(S_GENERATE_ACTIVATOR, !fGenerateActivator.getSelection());
		if (fUIPlugin.isEnabled()) {
			settings.put(S_UI_PLUGIN, !fUIPlugin.getSelection());
		}
		if (fApiAnalysisButton.isEnabled()) {
			settings.put(S_API_ANALYSIS, fApiAnalysisButton.getSelection());
		}
		settings.put(S_RCP_PLUGIN, fYesButton.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		if (getNextPage() instanceof TemplateListSelectionPage) {
			TemplateListSelectionPage templatePage = (TemplateListSelectionPage) getNextPage();
			return super.canFlipToNextPage() && templatePage.isAnyTemplateAvailable();
		}
		return super.canFlipToNextPage();
	}
}
