/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.launcher.BaseBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This preference page contains all options for launching.
 */
public class LaunchingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final class DefaultRuntimeWorkspaceBlock extends BaseBlock {

		DefaultRuntimeWorkspaceBlock() {
			super(null);
		}

		public void createControl(Composite parent) {
			Group group = SWTFactory.createGroup(parent, PDEUIMessages.MainPreferencePage_runtimeWorkspaceGroup, 2, 1,
					GridData.FILL_HORIZONTAL);
			Composite radios = SWTFactory.createComposite(group, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);

			fRuntimeWorkspaceLocationRadio = new Button(radios, SWT.RADIO);
			fRuntimeWorkspaceLocationRadio.setText(PDEUIMessages.MainPreferencePage_runtimeWorkspace_asLocation);
			fRuntimeWorkspaceLocationRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			fRuntimeWorkspaceLocationRadio.setSelection(true);

			fRuntimeWorkspacesContainerRadio = new Button(radios, SWT.RADIO);
			fRuntimeWorkspacesContainerRadio.setText(PDEUIMessages.MainPreferencePage_runtimeWorkspace_asContainer);
			fRuntimeWorkspacesContainerRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			createText(group, PDEUIMessages.WorkspaceDataBlock_location, 0);
			((GridData) fLocationText.getLayoutData()).widthHint = 200;
			fRuntimeWorkspaceLocation = fLocationText;

			Composite buttons = SWTFactory.createComposite(group, 3, 2,
					GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL, 0, 0);
			createButtons(buttons,
					new String[] { PDEUIMessages.MainPreferencePage_runtimeWorkspace_workspace,
							PDEUIMessages.MainPreferencePage_runtimeWorkspace_fileSystem,
							PDEUIMessages.MainPreferencePage_runtimeWorkspace_variables });
		}

		@Override
		protected String getName() {
			return PDEUIMessages.WorkspaceDataBlock_name;
		}

		@Override
		protected boolean isFile() {
			return false;
		}
	}

	private final class DefaultJUnitWorkspaceBlock extends BaseBlock {

		DefaultJUnitWorkspaceBlock() {
			super(null);
		}

		public void createControl(Composite parent) {
			Group group = SWTFactory.createGroup(parent, PDEUIMessages.MainPreferencePage_junitWorkspaceGroup, 2, 1,
					GridData.FILL_HORIZONTAL);
			SWTFactory.createLabel(group, PDEUIMessages.PluginsTab_launchWith, 1);
			fJunitLaunchWithCombo = SWTFactory.createCombo(group, SWT.READ_ONLY | SWT.BORDER, 1,
					GridData.HORIZONTAL_ALIGN_BEGINNING,
					new String[] { PDEUIMessages.MainPreferencePage_optionTestPlugin, PDEUIMessages.PluginsTab_allPlugins });
			SWTFactory.createLabel(group, "", 1); //$NON-NLS-1$
			Composite options = SWTFactory.createComposite(group, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);
			fJunitAutoIncludeRequirementsButton = SWTFactory.createCheckButton(options,
					PDEUIMessages.AdvancedLauncherTab_autoIncludeRequirements_plugins, null, true, 1);
			fJunitIncludeOptionalButton = SWTFactory.createCheckButton(options,
					PDEUIMessages.AdvancedLauncherTab_includeOptional_plugins, null, true, 1);
			fJunitAddWorkspaceButton = SWTFactory.createCheckButton(options,
					PDEUIMessages.AdvancedLauncherTab_addNew_plugins, null, false, 1);
			fJunitAutoValidate = SWTFactory.createCheckButton(options,
					PDEUIMessages.PluginsTabToolBar_auto_validate_plugins, null, true, 1);
			Composite radios = SWTFactory.createComposite(group, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);

			fJUnitWorkspaceLocationRadio = new Button(radios, SWT.RADIO);
			fJUnitWorkspaceLocationRadio.setText(PDEUIMessages.MainPreferencePage_junitWorkspace_asLocation);
			fJUnitWorkspaceLocationRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			fJUnitWorkspaceLocationRadio.setSelection(true);

			fJUnitWorkspacesContainerRadio = new Button(radios, SWT.RADIO);
			fJUnitWorkspacesContainerRadio.setText(PDEUIMessages.MainPreferencePage_junitWorkspace_asContainer);
			fJUnitWorkspacesContainerRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			createText(group, PDEUIMessages.WorkspaceDataBlock_location, 0);
			((GridData) fLocationText.getLayoutData()).widthHint = 200;
			fJUnitWorkspaceLocation = fLocationText;

			Composite buttons = SWTFactory.createComposite(group, 3, 2,
					GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL, 0, 0);
			createButtons(buttons,
					new String[] { PDEUIMessages.MainPreferencePage_junitWorkspace_workspace,
							PDEUIMessages.MainPreferencePage_junitWorkspace_fileSystem,
							PDEUIMessages.MainPreferencePage_junitWorkspace_variables });
		}

		@Override
		protected String getName() {
			return PDEUIMessages.DefaultJUnitWorkspaceBlock_name;
		}

		@Override
		protected boolean isFile() {
			return false;
		}
	}

	public static final String ID = "org.eclipse.pde.ui.LaunchingPreferencePage"; //$NON-NLS-1$

	private Button fAutoManage;

	private Button fAddSwtNonDisposalReporting;

	private Text fRuntimeWorkspaceLocation;
	private Button fRuntimeWorkspaceLocationRadio;
	private Button fRuntimeWorkspacesContainerRadio;

	private Text fJUnitWorkspaceLocation;
	private Button fJUnitWorkspaceLocationRadio;
	private Button fJUnitWorkspacesContainerRadio;

	private Combo fJunitLaunchWithCombo;
	private Button fJunitAutoIncludeRequirementsButton;
	private Button fJunitIncludeOptionalButton;
	private Button fJunitAddWorkspaceButton;
	private Button fJunitAutoValidate;

	private Button fFragmentsPlatformButton;

	private Button fFragmentsExtensibleApiButton;

	public LaunchingPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEUIMessages.LaunchingPreferencePage_description);
	}

	@Override
	protected Control createContents(Composite parent) {
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();

		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		((GridLayout) composite.getLayout()).verticalSpacing = 15;
		((GridLayout) composite.getLayout()).marginTop = 15;

		Composite optionComp = SWTFactory.createComposite(composite, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);

		fAutoManage = new Button(optionComp, SWT.CHECK);
		fAutoManage.setText(PDEUIMessages.MainPreferencePage_updateStale);
		fAutoManage.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE));

		fAddSwtNonDisposalReporting = new Button(optionComp, SWT.CHECK);
		fAddSwtNonDisposalReporting.setText(PDEUIMessages.MainPreferencePage_AddSwtNonDisposedToVMArguments);
		fAddSwtNonDisposalReporting
				.setToolTipText(PDEUIMessages.MainPreferencePage_AddSwtNonDisposedToVMArgumentsToolTop);
		fAddSwtNonDisposalReporting
				.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.ADD_SWT_NON_DISPOSAL_REPORTING));

		new DefaultRuntimeWorkspaceBlock().createControl(composite);
		fRuntimeWorkspaceLocation
				.setText(launchingStore.getString(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION));
		boolean runtimeLocationIsContainer = launchingStore
				.getBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER);
		fRuntimeWorkspaceLocationRadio.setSelection(!runtimeLocationIsContainer);
		fRuntimeWorkspacesContainerRadio.setSelection(runtimeLocationIsContainer);

		new DefaultJUnitWorkspaceBlock().createControl(composite);
		Group group = SWTFactory.createGroup(composite, PDEUIMessages.LaunchingPreferencePage_GroupComputingOptions, 1,
				1,
				GridData.FILL_HORIZONTAL);
		fFragmentsPlatformButton = SWTFactory.createCheckButton(group, PDEUIMessages.LaunchingPreferencePage_IncludePlatformFragments, null, false,
				1);
		fFragmentsExtensibleApiButton = SWTFactory.createCheckButton(group,
				PDEUIMessages.LaunchingPreferencePage_IncludeExtensibleFragments, null, false, 1);
		fFragmentsPlatformButton.setSelection(
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE_PLATFORM_FRAGMENTS));
		fFragmentsExtensibleApiButton.setSelection(
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE_EXTENSIBLE_FRAGMENTS));
		fJUnitWorkspaceLocation
				.setText(launchingStore.getString(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION));
		boolean jUnitLocationIsContainer = launchingStore
				.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
		fJUnitWorkspaceLocationRadio.setSelection(!jUnitLocationIsContainer);
		fJUnitWorkspacesContainerRadio.setSelection(jUnitLocationIsContainer);
		fJunitLaunchWithCombo.select(ILaunchingPreferenceConstants.VALUE_JUNIT_LAUNCH_WITH_TESTPLUGIN
				.equals(launchingStore.getString(ILaunchingPreferenceConstants.PROP_JUNIT_LAUNCH_WITH)) ? 0 : 1);
		fJunitAutoIncludeRequirementsButton
				.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_AUTO_INCLUDE));
		fJunitIncludeOptionalButton
				.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_INCLUDE_OPTIONAL));
		fJunitAddWorkspaceButton.setSelection(
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_ADD_NEW_WORKSPACE_PLUGINS));
		fJunitAutoValidate
				.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_VALIDATE_LAUNCH));
		return composite;
	}


	@Override
	public void createControl(Composite composite) {
		super.createControl(composite);
		org.eclipse.jface.dialogs.Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.MAIN_PREFERENCE_PAGE);
	}

	@Override
	public boolean performOk() {
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, fAutoManage.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION,
				fRuntimeWorkspaceLocation.getText());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER,
				fRuntimeWorkspacesContainerRadio.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION,
				fJUnitWorkspaceLocation.getText());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER,
				fJUnitWorkspacesContainerRadio.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_LAUNCH_WITH,
				fJunitLaunchWithCombo.getSelectionIndex() == 0
						? ILaunchingPreferenceConstants.VALUE_JUNIT_LAUNCH_WITH_TESTPLUGIN
						: ILaunchingPreferenceConstants.VALUE_JUNIT_LAUNCH_WITH_ALL);
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_ADD_NEW_WORKSPACE_PLUGINS,
				fJunitAddWorkspaceButton.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_AUTO_INCLUDE,
				fJunitAutoIncludeRequirementsButton.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_INCLUDE_OPTIONAL,
				fJunitIncludeOptionalButton.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_VALIDATE_LAUNCH,
				fJunitAutoValidate.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.ADD_SWT_NON_DISPOSAL_REPORTING,
				fAddSwtNonDisposalReporting.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE_EXTENSIBLE_FRAGMENTS,
				fFragmentsExtensibleApiButton.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE_PLATFORM_FRAGMENTS,
				fFragmentsPlatformButton.getSelection());
		try {
			launchingStore.flush();
		} catch (BackingStoreException e) {
			PDEPlugin.log(e);
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		fAutoManage.setSelection(launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE));
		fAddSwtNonDisposalReporting.setSelection(
				launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.ADD_SWT_NON_DISPOSAL_REPORTING));
		boolean runtimeLocationIsContainer = launchingStore
				.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER);
		fRuntimeWorkspaceLocationRadio.setSelection(!runtimeLocationIsContainer);
		fRuntimeWorkspacesContainerRadio.setSelection(runtimeLocationIsContainer);
		fRuntimeWorkspaceLocation.setText(
				launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION));

		boolean jUnitLocationIsContainer = launchingStore
				.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
		fJUnitWorkspaceLocationRadio.setSelection(!jUnitLocationIsContainer);
		fJUnitWorkspacesContainerRadio.setSelection(jUnitLocationIsContainer);
		fJUnitWorkspaceLocation
				.setText(launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION));
		fJunitLaunchWithCombo.select(ILaunchingPreferenceConstants.VALUE_JUNIT_LAUNCH_WITH_TESTPLUGIN
				.equals(launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_JUNIT_LAUNCH_WITH)) ? 0 : 1);
		fJunitAutoIncludeRequirementsButton
				.setSelection(launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_AUTO_INCLUDE));
		fJunitIncludeOptionalButton.setSelection(
				launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_INCLUDE_OPTIONAL));
		fJunitAddWorkspaceButton.setSelection(
				launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_ADD_NEW_WORKSPACE_PLUGINS));
		fJunitAutoValidate.setSelection(
				launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_VALIDATE_LAUNCH));
		fFragmentsExtensibleApiButton.setSelection(
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE_EXTENSIBLE_FRAGMENTS));
		fFragmentsPlatformButton.setSelection(
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE_PLATFORM_FRAGMENTS));
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
