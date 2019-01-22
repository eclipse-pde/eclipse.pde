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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.BaseBlock;
import org.eclipse.pde.internal.ui.shared.target.TargetStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This is the top level preference page for PDE.  It contains a random assortment of preferences that don't belong to other pages.
 *
 */
public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final class DefaultRuntimeWorkspaceBlock extends BaseBlock {

		DefaultRuntimeWorkspaceBlock() {
			super(null);
		}

		public void createControl(Composite parent) {
			Group group = SWTFactory.createGroup(parent, PDEUIMessages.MainPreferencePage_runtimeWorkspaceGroup, 2, 1, GridData.FILL_HORIZONTAL);
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

			Composite buttons = SWTFactory.createComposite(group, 3, 2, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL, 0, 0);
			createButtons(buttons, new String[] {PDEUIMessages.MainPreferencePage_runtimeWorkspace_workspace, PDEUIMessages.MainPreferencePage_runtimeWorkspace_fileSystem, PDEUIMessages.MainPreferencePage_runtimeWorkspace_variables});
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
			Group group = SWTFactory.createGroup(parent, PDEUIMessages.MainPreferencePage_junitWorkspaceGroup, 2, 1, GridData.FILL_HORIZONTAL);
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

			Composite buttons = SWTFactory.createComposite(group, 3, 2, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL, 0, 0);
			createButtons(buttons, new String[] {PDEUIMessages.MainPreferencePage_junitWorkspace_workspace, PDEUIMessages.MainPreferencePage_junitWorkspace_fileSystem, PDEUIMessages.MainPreferencePage_junitWorkspace_variables});
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

	public static final String ID = "org.eclipse.pde.ui.MainPreferencePage"; //$NON-NLS-1$

	private Button fUseID;
	private Button fUseName;
	private Button fAutoManage;
	private Button fOverwriteBuildFiles;
	private Button fShowSourceBundles;
	private Button fPromptOnRemove;
	private Button fAddToJavaSearch;
	private Button fShowTargetStatus;
	private Button fAlwaysPreferWorkspace;
	private Button fDisableAPIAnalysisBuilder;

	private Text fRuntimeWorkspaceLocation;
	private Button fRuntimeWorkspaceLocationRadio;
	private Button fRuntimeWorkspacesContainerRadio;

	private Text fJUnitWorkspaceLocation;
	private Button fJUnitWorkspaceLocationRadio;
	private Button fJUnitWorkspacesContainerRadio;

	private Text fTestPluginPatternText;

	public MainPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEUIMessages.Preferences_MainPage_Description);
	}

	@Override
	protected Control createContents(Composite parent) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();

		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		((GridLayout) composite.getLayout()).verticalSpacing = 15;
		((GridLayout) composite.getLayout()).marginTop = 15;

		Composite optionComp = SWTFactory.createComposite(composite, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);

		fOverwriteBuildFiles = new Button(optionComp, SWT.CHECK);
		fOverwriteBuildFiles.setText(PDEUIMessages.MainPreferencePage_promptBeforeOverwrite);
		fOverwriteBuildFiles.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT)));

		fAutoManage = new Button(optionComp, SWT.CHECK);
		fAutoManage.setText(PDEUIMessages.MainPreferencePage_updateStale);
		fAutoManage.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE));

		fPromptOnRemove = new Button(optionComp, SWT.CHECK);
		fPromptOnRemove.setText(PDEUIMessages.MainPreferencePage_promtBeforeRemove);
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		fPromptOnRemove.addSelectionListener(widgetSelectedAdapter(e -> PDEPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET, fPromptOnRemove.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS)));

		fAddToJavaSearch = new Button(optionComp, SWT.CHECK);
		fAddToJavaSearch.setText(PDEUIMessages.MainPreferencePage_addToJavaSearch);
		fAddToJavaSearch.setSelection(store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH));

		fShowTargetStatus = new Button(optionComp, SWT.CHECK);
		fShowTargetStatus.setText(PDEUIMessages.MainPreferencePage_ShowTargetStatus);
		fShowTargetStatus.setSelection(store.getBoolean(IPreferenceConstants.SHOW_TARGET_STATUS));

		fAlwaysPreferWorkspace = new Button(optionComp, SWT.CHECK);
		fAlwaysPreferWorkspace.setText(PDEUIMessages.MainPreferencePage_WorkspacePluginsOverrideTarget);
		fAlwaysPreferWorkspace.setSelection(store.getBoolean(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET));
		fAlwaysPreferWorkspace.setToolTipText(PDEUIMessages.MainPreferencePage_WorkspacePluginsOverrideTargetTooltip);


		fDisableAPIAnalysisBuilder = new Button(optionComp, SWT.CHECK);
		fDisableAPIAnalysisBuilder.setText(PDEUIMessages.MainPreferencePage_DisableAPIAnalysisBuilder);
		fDisableAPIAnalysisBuilder.setSelection(store.getBoolean(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER));

		Composite pathComposite = new Composite(optionComp, SWT.NONE);
		pathComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		GridLayout layout = new GridLayout(2, false);
		layout.marginRight = 12;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		pathComposite.setLayout(layout);
		pathComposite.setFont(optionComp.getFont());

		Group testGroup = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_test_plugin_pattern_group,
				2, 1,
				GridData.FILL_HORIZONTAL);
		Label testPluginPatternDescription = new Label(testGroup, SWT.LEFT);
		testPluginPatternDescription.setText(PDEUIMessages.MainPreferencePage_test_plugin_pattern_description);
		testPluginPatternDescription.setFont(JFaceResources.getDialogFont());
		GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
		gd2.widthHint = 200;
		gd2.horizontalSpan = 2;
		testPluginPatternDescription.setLayoutData(gd2);

		Label testPluginPatternLabel = new Label(testGroup, SWT.LEFT);
		testPluginPatternLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		testPluginPatternLabel.setText(PDEUIMessages.MainPreferencePage_test_plugin_pattern_label);
		testPluginPatternLabel.setFont(JFaceResources.getDialogFont());

		fTestPluginPatternText = new Text(testGroup, SWT.BORDER | SWT.SINGLE);
		// add some listeners for regex syntax checking
		fTestPluginPatternText.addModifyListener(e -> updateOKStatus());

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		// new GridData(SWT.FILL, SWT.CENTER, true, false)
		fTestPluginPatternText.setLayoutData(gd);
		fTestPluginPatternText.setText(store.getString(IPreferenceConstants.TEST_PLUGIN_PATTERN));

		TextContentAdapter contentAdapter = new TextContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
				true);
		ContentAssistCommandAdapter contentAssist = new ContentAssistCommandAdapter(fTestPluginPatternText,
				contentAdapter,
				findProposer, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true);
		contentAssist.setEnabled(true);
		Label testPluginPatternNote = new Label(testGroup, SWT.LEFT);
		testPluginPatternNote.setText(PDEUIMessages.MainPreferencePage_test_plugin_pattern_note);
		testPluginPatternNote.setFont(JFaceResources.getDialogFont());
		testPluginPatternNote.setLayoutData(gd2);

		Group group = SWTFactory.createGroup(composite, PDEUIMessages.Preferences_MainPage_showObjects, 2, 1, GridData.FILL_HORIZONTAL);
		fUseID = new Button(group, SWT.RADIO);
		fUseID.setText(PDEUIMessages.Preferences_MainPage_useIds);

		fUseName = new Button(group, SWT.RADIO);
		fUseName.setText(PDEUIMessages.Preferences_MainPage_useFullNames);

		fShowSourceBundles = SWTFactory.createCheckButton(group, PDEUIMessages.MainPreferencePage_showSourceBundles, null, store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES), 2);

		if (store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_IDS)) {
			fUseID.setSelection(true);
		} else {
			fUseName.setSelection(true);
		}

		new DefaultRuntimeWorkspaceBlock().createControl(composite);
		fRuntimeWorkspaceLocation.setText(launchingStore.getString(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION));
		boolean runtimeLocationIsContainer = launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER);
		fRuntimeWorkspaceLocationRadio.setSelection(!runtimeLocationIsContainer);
		fRuntimeWorkspacesContainerRadio.setSelection(runtimeLocationIsContainer);

		new DefaultJUnitWorkspaceBlock().createControl(composite);
		fJUnitWorkspaceLocation.setText(launchingStore.getString(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION));
		boolean jUnitLocationIsContainer = launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
		fJUnitWorkspaceLocationRadio.setSelection(!jUnitLocationIsContainer);
		fJUnitWorkspacesContainerRadio.setSelection(jUnitLocationIsContainer);

		return composite;
	}

	private boolean validateRegex() {
		try {
			Pattern.compile(fTestPluginPatternText.getText());
		} catch (PatternSyntaxException e) {
			String locMessage = e.getLocalizedMessage();
			int i = 0;
			while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
				i++;
			}
			setErrorMessage(locMessage.substring(0, i));
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	final void updateOKStatus() {
		boolean regexStatus = validateRegex();
		setValid(regexStatus);
	}

	@Override
	public void createControl(Composite composite) {
		super.createControl(composite);
		org.eclipse.jface.dialogs.Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.MAIN_PREFERENCE_PAGE);
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (fUseID.getSelection()) {
			store.setValue(IPreferenceConstants.PROP_SHOW_OBJECTS, IPreferenceConstants.VALUE_USE_IDS);
		} else {
			store.setValue(IPreferenceConstants.PROP_SHOW_OBJECTS, IPreferenceConstants.VALUE_USE_NAMES);
		}
		store.setValue(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT, fOverwriteBuildFiles.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS);
		store.setValue(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES, fShowSourceBundles.getSelection());

		boolean synchJavaSearch = fAddToJavaSearch.getSelection();
		if (store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH) != synchJavaSearch) {
			store.setValue(IPreferenceConstants.ADD_TO_JAVA_SEARCH, synchJavaSearch);
			try {
				if (synchJavaSearch) {
					ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
					if (service != null) {
						AddToJavaSearchJob.synchWithTarget(service.getWorkspaceTargetDefinition());
					}
				} else {
					AddToJavaSearchJob.clearAll();
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}

		boolean useWorkspace = fAlwaysPreferWorkspace.getSelection();
		if (store.getBoolean(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET) != useWorkspace) {
			store.setValue(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET, fAlwaysPreferWorkspace.getSelection());
			PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
			prefs.setValue(ICoreConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET, fAlwaysPreferWorkspace.getSelection());
			try {
				InstanceScope.INSTANCE.getNode(PDECore.PLUGIN_ID).flush();
			} catch (BackingStoreException e) {
			}
			PDECore.getDefault().getModelManager().targetReloaded(null);
		}

		String pluginPatternText = fTestPluginPatternText.getText();
		if (!pluginPatternText.equals(store.getString(IPreferenceConstants.TEST_PLUGIN_PATTERN))) {
			store.setValue(IPreferenceConstants.TEST_PLUGIN_PATTERN, pluginPatternText);
			PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
			prefs.setValue(ICoreConstants.TEST_PLUGIN_PATTERN, pluginPatternText);
		}

		boolean showTarget = fShowTargetStatus.getSelection();
		if (store.getBoolean(IPreferenceConstants.SHOW_TARGET_STATUS) != showTarget) {
			store.setValue(IPreferenceConstants.SHOW_TARGET_STATUS, showTarget);
			TargetStatus.refreshTargetStatus();
		}


		boolean disableAPIAnalysisBuilder = fDisableAPIAnalysisBuilder.getSelection();
		if (store.getBoolean(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER) != disableAPIAnalysisBuilder) {
			store.setValue(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER, fDisableAPIAnalysisBuilder.getSelection());
			PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
			prefs.setValue(ICoreConstants.DISABLE_API_ANALYSIS_BUILDER, fDisableAPIAnalysisBuilder.getSelection());
			IProject[] projects = BuildJob.getApiProjects();
			if (projects != null) {
				String message = PDEUIMessages.MainPreferencePage_askFullRebuild;
				int userInput = MessageDialog.open(MessageDialog.QUESTION, this.getShell(),
						PDEUIMessages.MainPreferencePage_settingChanged, message, SWT.NONE,
						PDEUIMessages.MainPreferencePage_build, PDEUIMessages.MainPreferencePage_notNow);
				if (Window.OK == userInput) {
					BuildJob.getBuildJob(projects).schedule();

				}
			}

		}

		PDEPlugin.getDefault().getPreferenceManager().savePluginPreferences();

		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, fAutoManage.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION, fRuntimeWorkspaceLocation.getText());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER, fRuntimeWorkspacesContainerRadio.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION, fJUnitWorkspaceLocation.getText());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER, fJUnitWorkspacesContainerRadio.getSelection());
		try {
			launchingStore.flush();
		} catch (BackingStoreException e) {
			PDEPlugin.log(e);
		}

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (store.getDefaultString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_IDS)) {
			fUseID.setSelection(true);
			fUseName.setSelection(false);
		} else {
			fUseID.setSelection(false);
			fUseName.setSelection(true);
		}
		fAutoManage.setSelection(false);
		fOverwriteBuildFiles.setSelection(true);
		fShowSourceBundles.setSelection(false);
		fPromptOnRemove.setSelection(true);

		fAddToJavaSearch.setSelection(store.getDefaultBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH));
		fShowTargetStatus.setSelection(store.getDefaultBoolean(IPreferenceConstants.SHOW_TARGET_STATUS));
		fAlwaysPreferWorkspace.setSelection(store.getDefaultBoolean(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET));
		fDisableAPIAnalysisBuilder.setSelection(store.getDefaultBoolean(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER));
		fTestPluginPatternText.setText(store.getDefaultString(IPreferenceConstants.TEST_PLUGIN_PATTERN));
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		boolean runtimeLocationIsContainer = launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER);
		fRuntimeWorkspaceLocationRadio.setSelection(!runtimeLocationIsContainer);
		fRuntimeWorkspacesContainerRadio.setSelection(runtimeLocationIsContainer);
		fRuntimeWorkspaceLocation.setText(launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION));

		boolean jUnitLocationIsContainer = launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
		fJUnitWorkspaceLocationRadio.setSelection(!jUnitLocationIsContainer);
		fJUnitWorkspacesContainerRadio.setSelection(jUnitLocationIsContainer);
		fJUnitWorkspaceLocation.setText(launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION));
	}

	@Override
	public void setVisible(boolean visible) {
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS.equals(PDEPlugin.getDefault().getPreferenceManager().getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		super.setVisible(visible);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
