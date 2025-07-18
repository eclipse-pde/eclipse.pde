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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.shared.target.TargetStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This is the top level preference page for PDE. It contains a random
 * assortment of preferences that don't belong to other pages.
 */
public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.eclipse.pde.ui.MainPreferencePage"; //$NON-NLS-1$

	private Button fUseID;
	private Button fUseName;
	private Button fOverwriteBuildFiles;
	private Button fShowSourceBundles;
	private Button fPromptOnRemove;
	private Button fAddToJavaSearch;
	private Button fShowTargetStatus;
	private Button fAlwaysPreferWorkspace;
	private Button fDisableAPIAnalysisBuilder;
	private Button fRunAPIAnalysisBuilderAsJob;
	private Text fTestPluginPatternText;


	public MainPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEUIMessages.Preferences_MainPage_Description);
	}

	@Override
	protected Control createContents(Composite parent) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		((GridLayout) composite.getLayout()).verticalSpacing = 15;
		((GridLayout) composite.getLayout()).marginTop = 15;

		Composite optionComp = SWTFactory.createComposite(composite, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);

		fOverwriteBuildFiles = new Button(optionComp, SWT.CHECK);
		fOverwriteBuildFiles.setText(PDEUIMessages.MainPreferencePage_promptBeforeOverwrite);
		fOverwriteBuildFiles.setSelection(!MessageDialogWithToggle.ALWAYS
				.equals(store.getString(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT)));

		fPromptOnRemove = new Button(optionComp, SWT.CHECK);
		fPromptOnRemove.setText(PDEUIMessages.MainPreferencePage_promtBeforeRemove);
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS
				.equals(store.getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		fPromptOnRemove.addSelectionListener(widgetSelectedAdapter(e -> PDEPlugin.getDefault().getPreferenceStore()
				.setValue(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET,
						fPromptOnRemove.getSelection() ? MessageDialogWithToggle.PROMPT
								: MessageDialogWithToggle.ALWAYS)));

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

		fRunAPIAnalysisBuilderAsJob = new Button(optionComp, SWT.CHECK);
		fRunAPIAnalysisBuilderAsJob.setText(PDEUIMessages.MainPreferencePage_RunAPIAnalysisBuilderAsJob);
		fRunAPIAnalysisBuilderAsJob.setSelection(
				PDECore.getDefault().getPreferencesManager().getBoolean(ICoreConstants.RUN_API_ANALYSIS_AS_JOB));

		fDisableAPIAnalysisBuilder.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			fRunAPIAnalysisBuilderAsJob.setEnabled(!fDisableAPIAnalysisBuilder.getSelection());
		}));

		Composite pathComposite = new Composite(optionComp, SWT.NONE);
		pathComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		GridLayout layout = new GridLayout(2, false);
		layout.marginRight = 12;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		pathComposite.setLayout(layout);
		pathComposite.setFont(optionComp.getFont());

		Group testGroup = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_test_plugin_pattern_group,
				2, 1, GridData.FILL_HORIZONTAL);
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
				contentAdapter, findProposer, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0],
				true);
		contentAssist.setEnabled(true);
		Label testPluginPatternNote = new Label(testGroup, SWT.LEFT);
		testPluginPatternNote.setText(PDEUIMessages.MainPreferencePage_test_plugin_pattern_note);
		testPluginPatternNote.setFont(JFaceResources.getDialogFont());
		testPluginPatternNote.setLayoutData(gd2);

		Group group = SWTFactory.createGroup(composite, PDEUIMessages.Preferences_MainPage_showObjects, 2, 1,
				GridData.FILL_HORIZONTAL);
		fUseID = new Button(group, SWT.RADIO);
		fUseID.setText(PDEUIMessages.Preferences_MainPage_useIds);

		fUseName = new Button(group, SWT.RADIO);
		fUseName.setText(PDEUIMessages.Preferences_MainPage_useFullNames);

		fShowSourceBundles = SWTFactory.createCheckButton(group, PDEUIMessages.MainPreferencePage_showSourceBundles,
				null, store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES), 2);

		if (store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_IDS)) {
			fUseID.setSelection(true);
		} else {
			fUseName.setSelection(true);
		}

		Group bundlePoolGp = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_BundlePoolPrefsGroup, 2,
				1, GridData.FILL_HORIZONTAL);
		WidgetFactory.label(SWT.WRAP)
				.layoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create())
				.text(PDEUIMessages.MainPreferencePage_BundlePoolPrefsCleanDesc).create(bundlePoolGp);
		WidgetFactory.button(SWT.PUSH).text(PDEUIMessages.MainPreferencePage_BundlePoolPrefsCleanBtn)
				.layoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).create()).create(bundlePoolGp)
				.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::handleClean));

		return composite;
	}

	private void handleClean(SelectionEvent event) {
		// User pushed the "Clean" button to clean the bundle pool
		// Disable the button first until we are done
		((Button) event.widget).setEnabled(false);

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> P2TargetUtils.garbageCollect());
		} catch (InterruptedException e) {
			// Nothing, let them cancel
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
			MessageDialog.openError(getShell(), PDEUIMessages.MainPreferencePage_BundlePoolPrefsError, MessageFormat
					.format(PDEUIMessages.MainPreferencePage_BundlePoolPrefsErrorCleaning, e.getMessage()));
		} finally {
			// Reenable the button so they can do it again
			((Button) event.widget).setEnabled(true);
		}
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
		store.setValue(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT,
				fOverwriteBuildFiles.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS);
		store.setValue(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES, fShowSourceBundles.getSelection());

		boolean synchJavaSearch = fAddToJavaSearch.getSelection();
		if (store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH) != synchJavaSearch) {
			store.setValue(IPreferenceConstants.ADD_TO_JAVA_SEARCH, synchJavaSearch);
			try {
				if (synchJavaSearch) {
					ITargetPlatformService service = TargetPlatformService.getDefault();
					AddToJavaSearchJob.synchWithTarget(service.getWorkspaceTargetDefinition());
				} else {
					AddToJavaSearchJob.clearAll();
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}

		boolean useWorkspace = fAlwaysPreferWorkspace.getSelection();
		if (store.getBoolean(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET) != useWorkspace) {
			store.setValue(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET,
					fAlwaysPreferWorkspace.getSelection());
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
			store.setValue(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER,
					fDisableAPIAnalysisBuilder.getSelection());
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

		boolean runAPIAnalysisAsJob = fRunAPIAnalysisBuilderAsJob.getSelection();
		if (PDECore.getDefault().getPreferencesManager()
				.getBoolean(ICoreConstants.RUN_API_ANALYSIS_AS_JOB) != runAPIAnalysisAsJob) {
			PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
			prefs.setValue(ICoreConstants.RUN_API_ANALYSIS_AS_JOB, runAPIAnalysisAsJob);
		}
		PDECore.getDefault().getPreferencesManager().savePluginPreferences();
		PDEPlugin.getDefault().getPreferenceManager().savePluginPreferences();
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
		fOverwriteBuildFiles.setSelection(true);
		fShowSourceBundles.setSelection(false);
		fPromptOnRemove.setSelection(true);

		fAddToJavaSearch.setSelection(store.getDefaultBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH));
		fShowTargetStatus.setSelection(store.getDefaultBoolean(IPreferenceConstants.SHOW_TARGET_STATUS));
		fAlwaysPreferWorkspace
				.setSelection(store.getDefaultBoolean(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET));
		fRunAPIAnalysisBuilderAsJob.setEnabled(true);
		fRunAPIAnalysisBuilderAsJob.setSelection(
				PDECore.getDefault().getPreferencesManager().getDefaultBoolean(ICoreConstants.RUN_API_ANALYSIS_AS_JOB));
		fDisableAPIAnalysisBuilder
				.setSelection(store.getDefaultBoolean(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER));
		fTestPluginPatternText.setText(store.getDefaultString(IPreferenceConstants.TEST_PLUGIN_PATTERN));
	}

	@Override
	public void setVisible(boolean visible) {
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS.equals(PDEPlugin.getDefault()
				.getPreferenceManager().getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		super.setVisible(visible);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}
