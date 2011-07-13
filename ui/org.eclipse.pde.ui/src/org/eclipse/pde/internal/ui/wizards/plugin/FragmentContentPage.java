/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Charlie Fats <charlie.fats@gmail.com> - bug 219848
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.TreeSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.parts.PluginVersionPart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.framework.Version;

public class FragmentContentPage extends ContentPage {

	private Text fPluginIdText_newV;
	private Text fPluginIdText_oldV;
	private Text fPluginVersion;
	private Combo fMatchCombo;
	private boolean fNewVersion;
	private Composite fNotebook;
	private StackLayout fNotebookLayout;
	private Composite fOldComp;
	private Composite fNewComp;
	private PluginVersionPart fVersionPart;
	private Label fEELabel;
	private Button fExeEnvButton;
	private Combo fEEChoice;
	private final static String NO_EXECUTION_ENVIRONMENT = PDEUIMessages.PluginContentPage_noEE;

	protected ModifyListener listener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};

	public FragmentContentPage(String pageName, IProjectProvider provider, NewProjectCreationPage page, AbstractFieldData data) {
		super(pageName, provider, page, data);
		setTitle(PDEUIMessages.ContentPage_ftitle);
		setDescription(PDEUIMessages.ContentPage_fdesc);
		updateVersion(false);
		fVersionPart = new PluginVersionPart(fNewVersion);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);

		createFragmentPropertiesGroup(container);
		createParentPluginGroup(container);

		Dialog.applyDialogFont(container);
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_FRAGMENT_REQUIRED_DATA);
	}

	private void createFragmentPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(3, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEUIMessages.ContentPage_fGroup);

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fid);
		fIdText = createText(propertiesGroup, propertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fversion);
		fVersionText = createText(propertiesGroup, propertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fname);
		fNameText = createText(propertiesGroup, propertiesListener, 2);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_fprovider);
		fProviderCombo = createProviderCombo(propertiesGroup, propertiesListener, 2);

		createExecutionEnvironmentControls(propertiesGroup);
	}

	private void createParentPluginGroup(Composite container) {
		Group parentGroup = new Group(container, SWT.NONE);
		parentGroup.setLayout(new GridLayout(2, false));
		parentGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		parentGroup.setText(PDEUIMessages.ContentPage_parentPluginGroup);

		fNotebook = new Composite(parentGroup, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fNotebook.setLayoutData(gd);
		fNotebookLayout = new StackLayout();
		fNotebook.setLayout(fNotebookLayout);

		fNewComp = createNewVersionComp(fNotebook);
		fOldComp = createOldVersionComp(fNotebook);
		fNotebookLayout.topControl = fNewVersion ? fNewComp : fOldComp;

	}

	private Composite createNewVersionComp(Composite notebook) {
		Composite comp = new Composite(notebook, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText(PDEUIMessages.FragmentContentPage_pid);
		fPluginIdText_newV = createPluginIdContainer(comp, true, 2);

		fVersionPart.createVersionFields(comp, false, true);
		fVersionPart.addListeners(listener, listener);
		return comp;
	}

	private Composite createOldVersionComp(Composite notebook) {
		Composite comp = new Composite(notebook, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText(PDEUIMessages.FragmentContentPage_pid);

		Composite container = new Composite(comp, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPluginIdText_oldV = createPluginIdContainer(container, false, 1);

		label = new Label(comp, SWT.NONE);
		label.setText(PDEUIMessages.FragmentContentPage_pversion);
		fPluginVersion = createText(comp, listener);

		label = new Label(comp, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_matchRule);

		fMatchCombo = new Combo(comp, SWT.READ_ONLY | SWT.BORDER);
		fMatchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMatchCombo.setItems(new String[] {"", //$NON-NLS-1$
				PDEUIMessages.ManifestEditor_MatchSection_equivalent, PDEUIMessages.ManifestEditor_MatchSection_compatible, PDEUIMessages.ManifestEditor_MatchSection_perfect, PDEUIMessages.ManifestEditor_MatchSection_greater});
		fMatchCombo.setText(fMatchCombo.getItem(0));
		return comp;
	}

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

	private Text createPluginIdContainer(Composite parent, final boolean validateRange, int span) {
		final Text pluginText = createText(parent, listener);

		Button browse = new Button(parent, SWT.PUSH);
		browse.setText(PDEUIMessages.ContentPage_browse);
		browse.setLayoutData(new GridData());
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(pluginText.getDisplay(), new Runnable() {
					public void run() {
						PluginSelectionDialog dialog = new PluginSelectionDialog(pluginText.getShell(), false, false);
						dialog.create();
						if (dialog.open() == Window.OK) {
							IPluginModel model = (IPluginModel) dialog.getFirstResult();
							IPlugin plugin = model.getPlugin();
							String version = computeInitialPluginVersion(plugin.getVersion());
							if (validateRange) {
								fVersionPart.setVersion(version);
								fVersionPart.preloadFields();
							} else {
								fPluginVersion.setText(version);
							}
							pluginText.setText(plugin.getId());
						}
					}
				});
			}
		});
		SWTUtil.setButtonDimensionHint(browse);
		return pluginText;
	}

	private String computeInitialPluginVersion(String pluginVersion) {
		if (pluginVersion != null && VersionUtil.validateVersion(pluginVersion).isOK()) {
			Version pvi = Version.parseVersion(pluginVersion);
			return pvi.getMajor() + "." + pvi.getMinor() //$NON-NLS-1$
					+ "." + pvi.getMicro(); //$NON-NLS-1$
		}

		return pluginVersion;
	}

	public void updateData() {
		super.updateData();
		String version;
		if (fNewVersion) {
			version = fVersionPart.getVersion();
			((FragmentFieldData) fData).setPluginId(fPluginIdText_newV.getText().trim());
		} else {
			version = fPluginVersion.getText().trim();
			((FragmentFieldData) fData).setPluginId(fPluginIdText_oldV.getText().trim());
			((FragmentFieldData) fData).setMatch(fMatchCombo.getSelectionIndex());
		}

		if (fEEChoice.isEnabled() && !fEEChoice.getText().equals(NO_EXECUTION_ENVIRONMENT)) {
			fData.setExecutionEnvironment(fEEChoice.getText().trim());
		} else {
			fData.setExecutionEnvironment(null);
		}

		((FragmentFieldData) fData).setPluginVersion(version);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#validatePage()
	 */
	protected void validatePage() {
		String errorMessage = validateProperties();

		if (errorMessage == null) {
			String pluginID = fNewVersion ? fPluginIdText_newV.getText().trim() : fPluginIdText_oldV.getText().trim();
			if (pluginID.length() == 0) {
				errorMessage = PDEUIMessages.ContentPage_nopid;
			} else if (!(PluginRegistry.findModel(pluginID) instanceof IPluginModel)) {
				errorMessage = PDEUIMessages.ContentPage_pluginNotFound;
			} else {
				if (fNewVersion) {
					IStatus status = fVersionPart.validateFullVersionRangeText(false);
					if (status.getSeverity() != IStatus.OK) {
						errorMessage = status.getMessage();
					}
				} else {
					errorMessage = validateVersion(fPluginVersion);
				}
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
		if (fInitialized)
			setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			fMainPage.updateData();
			updateVersion(true);
		}
		super.setVisible(visible);
	}

	private void updateVersion(boolean updateComposite) {
		fNewVersion = Double.parseDouble(fData.getTargetVersion()) > 3.0;
		if (updateComposite) {
			Control oldPage = fNotebookLayout.topControl;
			if (fNewVersion)
				fNotebookLayout.topControl = fNewComp;
			else
				fNotebookLayout.topControl = fOldComp;
			if (oldPage != fNotebookLayout.topControl)
				fNotebook.layout();
		}
	}
}
