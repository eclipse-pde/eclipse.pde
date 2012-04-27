/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 179213
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewProjectCreationPage extends WizardNewProjectCreationPage {
	protected Button fJavaButton;
	private boolean fFragment;
	protected Label fSourceLabel;
	protected Text fSourceText;
	protected Label fOutputlabel;
	protected Text fOutputText;
	private AbstractFieldData fData;
	protected Button fEclipseButton;
	protected Combo fEclipseCombo;
	protected Combo fOSGiCombo;
	protected Button fOSGIButton;
	private IStructuredSelection fSelection;

	private static final String S_OSGI_PROJECT = "osgiProject"; //$NON-NLS-1$
	private static final String S_TARGET_NAME = "targetName"; //$NON-NLS-1$

	public NewProjectCreationPage(String pageName, AbstractFieldData data, boolean fragment, IStructuredSelection selection) {
		super(pageName);
		fFragment = fragment;
		fData = data;
		fSelection = selection;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite) getControl();
		GridLayout layout = new GridLayout();
		control.setLayout(layout);

		createProjectTypeGroup(control);
		createFormatGroup(control);
		createWorkingSetGroup(control, fSelection, new String[] {"org.eclipse.jdt.ui.JavaWorkingSetPage", //$NON-NLS-1$
				"org.eclipse.pde.ui.pluginWorkingSet", "org.eclipse.ui.resourceWorkingSetPage"}); //$NON-NLS-1$ //$NON-NLS-2$

		updateRuntimeDependency();

		Dialog.applyDialogFont(control);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, fFragment ? IHelpContextIds.NEW_FRAGMENT_STRUCTURE_PAGE : IHelpContextIds.NEW_PROJECT_STRUCTURE_PAGE);
		setControl(control);
	}

	protected void createProjectTypeGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.ProjectStructurePage_settings);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fJavaButton = createButton(group, SWT.CHECK, 2, 0);
		fJavaButton.setText(PDEUIMessages.ProjectStructurePage_java);
		fJavaButton.setSelection(true);
		fJavaButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fJavaButton.getSelection();
				fSourceLabel.setEnabled(enabled);
				fSourceText.setEnabled(enabled);
				fOutputlabel.setEnabled(enabled);
				fOutputText.setEnabled(enabled);
				setPageComplete(validatePage());
			}
		});

		fSourceLabel = createLabel(group, PDEUIMessages.ProjectStructurePage_source);
		fSourceText = createText(group);
		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		fSourceText.setText(store.getString(PreferenceConstants.SRCBIN_SRCNAME));

		fOutputlabel = createLabel(group, PDEUIMessages.ProjectStructurePage_output);
		fOutputText = createText(group);
		fOutputText.setText(store.getString(PreferenceConstants.SRCBIN_BINNAME));
	}

	protected void createFormatGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.NewProjectCreationPage_target);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NONE);
		if (fFragment)
			label.setText(PDEUIMessages.NewProjectCreationPage_ftarget);
		else
			label.setText(PDEUIMessages.NewProjectCreationPage_ptarget);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		IDialogSettings settings = getDialogSettings();
		boolean osgiProject = (settings == null) ? false : settings.getBoolean(S_OSGI_PROJECT);

		fEclipseButton = createButton(group, SWT.RADIO, 1, 30);
		fEclipseButton.setText(PDEUIMessages.NewProjectCreationPage_pDependsOnRuntime);
		fEclipseButton.setSelection(!osgiProject);
		fEclipseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateRuntimeDependency();
			}
		});

		fEclipseCombo = new Combo(group, SWT.READ_ONLY | SWT.SINGLE);
		fEclipseCombo.setItems(new String[] {PDEUIMessages.NewProjectCreationPage_target_version_range_3_5, ICoreConstants.TARGET34, ICoreConstants.TARGET33, ICoreConstants.TARGET32, ICoreConstants.TARGET31});

		String text = null;
		if (settings != null && !osgiProject) {
			text = settings.get(S_TARGET_NAME);
		}
		if (text == null && PDECore.getDefault().areModelsInitialized()) {
			text = TargetPlatformHelper.getTargetVersionString();
		}
		if (text == null || fEclipseCombo.indexOf(text) < 0) {
			text = PDEUIMessages.NewProjectCreationPage_target_version_range_3_5;
		}
		fEclipseCombo.setText(text);

		fOSGIButton = createButton(group, SWT.RADIO, 1, 30);
		fOSGIButton.setText(PDEUIMessages.NewProjectCreationPage_pPureOSGi);
		fOSGIButton.setSelection(osgiProject);

		fOSGiCombo = new Combo(group, SWT.READ_ONLY | SWT.SINGLE);
		fOSGiCombo.setItems(new String[] {ICoreConstants.EQUINOX, PDEUIMessages.NewProjectCreationPage_standard});

		text = null;
		if (settings != null && osgiProject) {
			text = settings.get(S_TARGET_NAME);
		}
		if (text == null || fOSGiCombo.indexOf(text) < 0) {
			text = ICoreConstants.EQUINOX;
		}
		fOSGiCombo.setText(text);
	}

	private void updateRuntimeDependency() {
		boolean depends = fEclipseButton.getSelection();
		fEclipseCombo.setEnabled(depends);
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

	private Label createLabel(Composite container, String text) {
		Label label = new Label(container, SWT.NONE);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalIndent = 30;
		label.setLayoutData(gd);
		return label;
	}

	private Text createText(Composite container) {
		Text text = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(validatePage());
			}
		});
		return text;
	}

	public void updateData() {
		fData.setSimple(!fJavaButton.getSelection());
		fData.setSourceFolderName(fSourceText.getText().trim());
		fData.setOutputFolderName(fOutputText.getText().trim());
		fData.setLegacy(false);

		// No project structure changes since 3.5, mark as latest version (though using any constant 3.5 or greater is equivalent)
		if (fEclipseCombo.getText().equals(PDEUIMessages.NewProjectCreationPage_target_version_range_3_5)) {
			fData.setTargetVersion(ICoreConstants.TARGET_VERSION_LATEST);
		} else {
			fData.setTargetVersion(fEclipseCombo.getText());
		}

		// No longer support 3.0 non-osgi bundles in wizard
		fData.setHasBundleStructure(true);
		fData.setOSGiFramework(fOSGIButton.getSelection() ? fOSGiCombo.getText() : null);
		fData.setWorkingSets(getSelectedWorkingSets());
	}

	protected boolean validatePage() {
		if (!super.validatePage())
			return false;

		String name = getProjectName();
		if (name.indexOf('%') >= 0) {
			setErrorMessage(PDEUIMessages.NewProjectCreationPage_invalidProjectName);
			return false;
		}

		String location = getLocationPath().toString();
		if (location.indexOf('%') >= 0) {
			setErrorMessage(PDEUIMessages.NewProjectCreationPage_invalidLocationPath);
			return false;
		}

		// this method can be called before controls are created, so ensure the
		// check box is not null
		if (fJavaButton != null && fJavaButton.getSelection()) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProject dmy = workspace.getRoot().getProject("project"); //$NON-NLS-1$
			IStatus status;
			if (fSourceText != null && fSourceText.getText().length() != 0) {
				status = workspace.validatePath(dmy.getFullPath().append(fSourceText.getText()).toString(), IResource.FOLDER);
				if (!status.isOK()) {
					setErrorMessage(status.getMessage());
					return false;
				}
			}
			if (fOutputText != null && fOutputText.getText().length() != 0) {
				status = workspace.validatePath(dmy.getFullPath().append(fOutputText.getText()).toString(), IResource.FOLDER);
				if (!status.isOK()) {
					setErrorMessage(status.getMessage());
					return false;
				}
			}
		}
		setErrorMessage(null);
		setMessage(null);
		return true;
	}

	public void saveSettings(IDialogSettings settings) {
		boolean eclipseSelected = fEclipseButton.getSelection();
		String targetName = eclipseSelected ? fEclipseCombo.getText() : fOSGiCombo.getText();
		settings.put(S_TARGET_NAME, (eclipseSelected && TargetPlatformHelper.getTargetVersionString().equals(targetName)) ? null : targetName);
		settings.put(S_OSGI_PROJECT, !eclipseSelected);
	}

}
