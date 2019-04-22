/*******************************************************************************
 * Copyright (c) 2012, 2019 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 490062
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

public class DSAnnotationPropertyPage extends PropertyPage implements IWorkbenchPreferencePage {

	private static final int OPTIONS_INDENT = LayoutConstants.getIndent();

	private Link workspaceLink;

	private Button projectCheckbox;

	private Control configBlockControl;

	private ControlEnableState configBlockEnableState;

	private Button enableCheckbox;

	private Composite optionBlockControl;

	private ControlEnableState optionBlockEnableState;

	private Text pathText;

	private Button classpathCheckbox;

	private Combo specVersionCombo;

	private Combo errorLevelCombo;

	private Combo missingUnbindMethodCombo;

	private Button enableBAPLGeneration;

	private IWorkingCopyManager wcManager;

	@Override
	public void init(IWorkbench workbench) {
		// do nothing
	}

	@Override
	public void setContainer(IPreferencePageContainer container) {
		super.setContainer(container);

		if (wcManager == null) {
			if (container instanceof IWorkbenchPreferenceContainer) {
				wcManager = ((IWorkbenchPreferenceContainer) container).getWorkingCopyManager();
			} else {
				wcManager = new WorkingCopyManager();
			}
		}
	}

	@Override
	protected Label createDescriptionLabel(Composite parent) {
		if (isProjectPreferencePage()) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setFont(parent.getFont());
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			projectCheckbox = new Button(composite, SWT.CHECK);
			projectCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));
			projectCheckbox.setText(Messages.DSAnnotationPropertyPage_projectCheckbox_text);
			projectCheckbox.setFont(JFaceResources.getDialogFont());
			projectCheckbox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					enableProjectSpecificSettings(projectCheckbox.getSelection());
					refreshWidgets();
				}
			});

			workspaceLink = createLink(composite, Messages.DSAnnotationPropertyPage_workspaceLink_text);
			workspaceLink.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));

			Label horizontalLine = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
			horizontalLine.setFont(composite.getFont());
		}

		return super.createDescriptionLabel(parent);
	}

	private Link createLink(Composite composite, String text) {
		Link link = new Link(composite, SWT.NONE);
		link.setFont(composite.getFont());
		link.setText("<A>" + text + "</A>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (PreferencesUtil.createPreferenceDialogOn(getShell(), Activator.PLUGIN_ID, new String[] { Activator.PLUGIN_ID }, null).open() == Window.OK)
					refreshWidgets();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		return link;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());

		configBlockControl = createPreferenceContent(composite);
		configBlockControl.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (isProjectPreferencePage()) {
			boolean useProjectSettings = hasProjectSpecificOptions(getProject());
			enableProjectSpecificSettings(useProjectSettings);
		}

		refreshWidgets();

		Dialog.applyDialogFont(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDSAnnotationHelpContextIds.DS_ANNOTATION_PAGE);

		return composite;
	}

	private Control createPreferenceContent(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());

		enableCheckbox = new Button(composite, SWT.CHECK);
		enableCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		enableCheckbox.setText(Messages.DSAnnotationPropertyPage_enableCheckbox_text);
		enableCheckbox.setFont(JFaceResources.getDialogFont());
		enableCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableOptions(enableCheckbox.getSelection());
			}
		});

		optionBlockControl = new Composite(composite, SWT.NONE);
		optionBlockControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout = new GridLayout(2, false);
		layout.marginLeft = OPTIONS_INDENT;
		layout.marginWidth = 0;
		optionBlockControl.setLayout(layout);
		optionBlockControl.setFont(composite.getFont());

		Composite pathComposite = new Composite(optionBlockControl, SWT.NONE);
		pathComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		pathComposite.setLayout(layout);
		pathComposite.setFont(optionBlockControl.getFont());

		Label pathLabel = new Label(pathComposite, SWT.LEFT);
		pathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		pathLabel.setText(Messages.DSAnnotationPropertyPage_pathLabel_text);
		pathLabel.setFont(JFaceResources.getDialogFont());

		pathText = new Text(pathComposite, SWT.BORDER | SWT.SINGLE);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		classpathCheckbox = new Button(optionBlockControl, SWT.CHECK);
		classpathCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		classpathCheckbox.setText(Messages.DSAnnotationPropertyPage_classpathCheckbox_text);
		classpathCheckbox.setFont(JFaceResources.getDialogFont());

		Label specVersionLabel = new Label(optionBlockControl, SWT.LEFT);
		specVersionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		specVersionLabel.setText(Messages.DSAnnotationPropertyPage_specVersionLabel_text);
		specVersionLabel.setFont(JFaceResources.getDialogFont());

		specVersionCombo = new Combo(optionBlockControl, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		specVersionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		specVersionCombo.setFont(JFaceResources.getDialogFont());
		specVersionCombo.add("1.3"); //$NON-NLS-1$
		specVersionCombo.add("1.2"); //$NON-NLS-1$
		specVersionCombo.select(0);

		Label errorLevelLabel = new Label(optionBlockControl, SWT.LEFT);
		errorLevelLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		errorLevelLabel.setText(Messages.DSAnnotationPropertyPage_errorLevelLabel_text);
		errorLevelLabel.setFont(JFaceResources.getDialogFont());

		errorLevelCombo = new Combo(optionBlockControl, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		errorLevelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		errorLevelCombo.setFont(JFaceResources.getDialogFont());
		errorLevelCombo.add(Messages.DSAnnotationPropertyPage_errorLevelError);
		errorLevelCombo.add(Messages.DSAnnotationPropertyPage_errorLevelWarning);
		errorLevelCombo.add(Messages.DSAnnotationPropertyPage_errorLevelIgnore);
		errorLevelCombo.select(0);

		Label missingUnbindMethodLabel = new Label(optionBlockControl, SWT.LEFT);
		missingUnbindMethodLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		missingUnbindMethodLabel.setText(Messages.DSAnnotationPropertyPage_missingUnbindMethodLevelLabel_text);
		missingUnbindMethodLabel.setFont(JFaceResources.getDialogFont());

		missingUnbindMethodCombo = new Combo(optionBlockControl, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		missingUnbindMethodCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		missingUnbindMethodCombo.setFont(JFaceResources.getDialogFont());
		missingUnbindMethodCombo.add(Messages.DSAnnotationPropertyPage_errorLevelError);
		missingUnbindMethodCombo.add(Messages.DSAnnotationPropertyPage_errorLevelWarning);
		missingUnbindMethodCombo.add(Messages.DSAnnotationPropertyPage_errorLevelIgnore);
		missingUnbindMethodCombo.select(0);

		enableBAPLGeneration = new Button(optionBlockControl, SWT.CHECK);
		enableBAPLGeneration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		enableBAPLGeneration.setText(Messages.DSAnnotationPropertyPage_enableBAPLGenerationLabel_text);
		enableBAPLGeneration.setFont(JFaceResources.getDialogFont());

		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void refreshWidgets() {
		IEclipsePreferences prefs = wcManager.getWorkingCopy(InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID));

		boolean enableValue = prefs.getBoolean(Activator.PREF_ENABLED, false);
		String pathValue = prefs.get(Activator.PREF_PATH, Activator.DEFAULT_PATH);
		boolean classpathValue = prefs.getBoolean(Activator.PREF_CLASSPATH, true);
		String specVersion = prefs.get(Activator.PREF_SPEC_VERSION, DSAnnotationVersion.V1_3.name());
		String errorLevel = prefs.get(Activator.PREF_VALIDATION_ERROR_LEVEL, ValidationErrorLevel.error.name());
		String missingUnbindMethodLevel = prefs.get(Activator.PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL, errorLevel);
		boolean generateBAPL = prefs.getBoolean(Activator.PREF_GENERATE_BAPL, true);

		if (useProjectSettings()) {
			IScopeContext scopeContext = new ProjectScope(getProject());
			prefs = wcManager.getWorkingCopy(scopeContext.getNode(Activator.PLUGIN_ID));

			enableValue = prefs.getBoolean(Activator.PREF_ENABLED, enableValue);
			pathValue = prefs.get(Activator.PREF_PATH, pathValue);
			classpathValue = prefs.getBoolean(Activator.PREF_CLASSPATH, classpathValue);
			specVersion = prefs.get(Activator.PREF_SPEC_VERSION, specVersion);
			errorLevel = prefs.get(Activator.PREF_VALIDATION_ERROR_LEVEL, errorLevel);
			missingUnbindMethodLevel = prefs.get(Activator.PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL, missingUnbindMethodLevel);
			generateBAPL = prefs.getBoolean(Activator.PREF_GENERATE_BAPL, generateBAPL);
		}

		enableCheckbox.setSelection(enableValue);
		enableOptions(enableValue && configBlockEnableState == null);
		pathText.setText(pathValue);
		classpathCheckbox.setSelection(classpathValue);

		DSAnnotationVersion specVersionEnum;
		try {
			specVersionEnum = DSAnnotationVersion.valueOf(specVersion);
		} catch (IllegalArgumentException e) {
			specVersionEnum = DSAnnotationVersion.V1_3;
		}

		specVersionCombo.select(DSAnnotationVersion.V1_3.ordinal() - specVersionEnum.ordinal());
		errorLevelCombo.select(getEnumIndex(errorLevel, ValidationErrorLevel.values(), 0));
		missingUnbindMethodCombo.select(getEnumIndex(missingUnbindMethodLevel, ValidationErrorLevel.values(), 0));
		enableBAPLGeneration.setSelection(generateBAPL);

		setErrorMessage(null);
	}

	private <E extends Enum<E>> int getEnumIndex(String property, E[] values, int defaultIndex) {
		for (int i = 1; i < values.length; ++i) {
			if (property.equals(values[i].name())) {
				return i;
			}
		}

		return defaultIndex;
	}

	private boolean hasProjectSpecificOptions(IProject project) {
		return new ProjectScope(project).getNode(Activator.PLUGIN_ID).get(Activator.PREF_ENABLED, null) != null;
	}

	private boolean useProjectSettings() {
		return isProjectPreferencePage() && projectCheckbox != null && projectCheckbox.getSelection();
	}

	private boolean isProjectPreferencePage() {
		return getElement() != null;
	}

	private void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
		projectCheckbox.setSelection(useProjectSpecificSettings);
		enablePreferenceContent(useProjectSpecificSettings);
		updateLinkVisibility();
	}

	private void enablePreferenceContent(boolean enable) {
		if (enable) {
			if (configBlockEnableState != null) {
				configBlockEnableState.restore();
				configBlockEnableState = null;
			}
		} else {
			if (configBlockEnableState == null) {
				configBlockEnableState = ControlEnableState.disable(configBlockControl, Arrays.asList(optionBlockControl));
			}
		}
	}

	private void enableOptions(boolean enable) {
		if (enable) {
			if (optionBlockEnableState != null) {
				optionBlockEnableState.restore();
				optionBlockEnableState = null;
			}
		} else {
			if (optionBlockEnableState == null) {
				optionBlockEnableState = ControlEnableState.disable(optionBlockControl);
			}
		}
	}

	private void updateLinkVisibility() {
		if (workspaceLink == null || workspaceLink.isDisposed()) {
			return;
		}

		workspaceLink.setEnabled(!useProjectSettings());
	}

	private IProject getProject() {
		IAdaptable element = getElement();
		if (element == null) {
			return null;
		}

		if (element instanceof IProject) {
			return (IProject) element;
		}

		return element.getAdapter(IProject.class);
	}

	@Override
	protected void performDefaults() {
		IScopeContext scopeContext;
		if (useProjectSettings()) {
			enableProjectSpecificSettings(false);
			scopeContext = new ProjectScope(getProject());
		} else {
			scopeContext = InstanceScope.INSTANCE;
		}

		IEclipsePreferences prefs = wcManager.getWorkingCopy(scopeContext.getNode(Activator.PLUGIN_ID));
		try {
			for (String key : prefs.keys()) {
				prefs.remove(key);
			}
		} catch (BackingStoreException e) {
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to restore default values.", e)); //$NON-NLS-1$
		}

		refreshWidgets();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		IEclipsePreferences prefs;
		if (isProjectPreferencePage()) {
			IProject project = getProject();
			prefs = wcManager.getWorkingCopy(new ProjectScope(project).getNode(Activator.PLUGIN_ID));
			if (useProjectSettings()) {
				Activator.getDefault().listenForClasspathPreferenceChanges(JavaCore.create(project));
			} else {
				try {
					prefs.clear();
				} catch (BackingStoreException e) {
					Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to reset project preferences.", e)); //$NON-NLS-1$
				}

				prefs = null;
			}
		} else {
			prefs = wcManager.getWorkingCopy(InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID));
		}

		if (prefs != null) {
			String path = pathText.getText().trim();
			if (!Path.EMPTY.isValidPath(path)) {
				setErrorMessage(String.format(Messages.DSAnnotationPropertyPage_errorMessage_path));
				return false;
			}

			prefs.putBoolean(Activator.PREF_ENABLED, enableCheckbox.getSelection());
			prefs.put(Activator.PREF_PATH, new Path(path).toString());
			prefs.putBoolean(Activator.PREF_CLASSPATH, classpathCheckbox.getSelection());

			DSAnnotationVersion[] versions = DSAnnotationVersion.values();
			int specVersionIndex = Math.max(Math.min(specVersionCombo.getSelectionIndex(), DSAnnotationVersion.V1_3.ordinal()), 0);
			prefs.put(Activator.PREF_SPEC_VERSION, versions[DSAnnotationVersion.V1_3.ordinal() - specVersionIndex].name());

			ValidationErrorLevel[] levels = ValidationErrorLevel.values();
			int errorLevelIndex = errorLevelCombo.getSelectionIndex();
			prefs.put(Activator.PREF_VALIDATION_ERROR_LEVEL, levels[Math.max(Math.min(errorLevelIndex, levels.length - 1), 0)].name());

			errorLevelIndex = missingUnbindMethodCombo.getSelectionIndex();
			prefs.put(Activator.PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL, levels[Math.max(Math.min(errorLevelIndex, levels.length - 1), 0)].name());

			prefs.putBoolean(Activator.PREF_GENERATE_BAPL, enableBAPLGeneration.getSelection());
		}

		try {
			wcManager.applyChanges();
		} catch (BackingStoreException e) {
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to save preferences.", e)); //$NON-NLS-1$
			return false;
		}

		return true;
	}
}