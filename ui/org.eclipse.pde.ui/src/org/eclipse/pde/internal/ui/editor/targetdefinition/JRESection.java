/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.core.target.ITargetDefinition;

import java.util.TreeSet;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing the JRE path in the target definition editor
 * @see EnvironmentPage
 * @see TargetEditor
 */
public class JRESection extends SectionPart {

	private Button fDefaultJREButton;
	private Button fNamedJREButton;
	private Button fExecEnvButton;
	private ComboPart fNamedJREsCombo;
	private ComboPart fExecEnvsCombo;
	private TreeSet fExecEnvChoices;
	private boolean fBlockChanges;
	private Button fConfigureJREButton;
	private TargetEditor fEditor;

	private static String JRE_PREF_PAGE_ID = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage"; //$NON-NLS-1$
	private static String EE_PREF_PAGE_ID = "org.eclipse.jdt.debug.ui.jreProfiles"; //$NON-NLS-1$

	public JRESection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 * 
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.EnvironmentBlock_jreTitle);
		section.setDescription(PDEUIMessages.JRESection_description);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));

		initializeValues();

		fDefaultJREButton = toolkit.createButton(client, PDEUIMessages.JRESection_defaultJRE, SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fDefaultJREButton.setLayoutData(gd);
		fDefaultJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				if (!fBlockChanges) {
					getTarget().setJREContainer(null);
					markDirty();
				}
			}
		});

		fNamedJREButton = toolkit.createButton(client, PDEUIMessages.JRESection_JREName, SWT.RADIO);
		fNamedJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				if (!fBlockChanges) {
					getTarget().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getSelection())));
					markDirty();
				}
			}
		});

		fNamedJREsCombo = new ComboPart();
		fNamedJREsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fNamedJREsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String[] installs = VMUtil.getVMInstallNames();
		fNamedJREsCombo.setItems(installs);
		fNamedJREsCombo.setVisibleItemCount(30);
		fNamedJREsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges) {
					getTarget().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getSelection())));
					markDirty();
				}
			}
		});

		fConfigureJREButton = toolkit.createButton(client, PDEUIMessages.JRESection_jrePreference, SWT.PUSH);
		fConfigureJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openPreferencePage(JRE_PREF_PAGE_ID);
			}
		});
		fConfigureJREButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTFactory.setButtonDimensionHint(fConfigureJREButton);

		fExecEnvButton = toolkit.createButton(client, PDEUIMessages.JRESection_ExecutionEnv, SWT.RADIO);
		fExecEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				if (!fBlockChanges) {
					getTarget().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getSelection())));
					markDirty();
				}
			}
		});

		fExecEnvsCombo = new ComboPart();
		fExecEnvsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fExecEnvsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fExecEnvsCombo.setItems((String[]) fExecEnvChoices.toArray(new String[fExecEnvChoices.size()]));
		fExecEnvsCombo.setVisibleItemCount(30);
		fExecEnvsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges) {
					getTarget().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getSelection())));
					markDirty();
				}
			}
		});

		Button configureEEButton = toolkit.createButton(client, PDEUIMessages.JRESection_eePreference, SWT.PUSH);
		configureEEButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openPreferencePage(EE_PREF_PAGE_ID);
			}
		});
		configureEEButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTFactory.setButtonDimensionHint(configureEEButton);

		refresh();
		section.setClient(client);
	}

	/**
	 * Initializes the combo with possible execution enviroments
	 */
	protected void initializeValues() {
		fExecEnvChoices = new TreeSet();
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		for (int i = 0; i < envs.length; i++)
			fExecEnvChoices.add(envs[i].getId());
	}

	protected void updateWidgets() {
		fNamedJREsCombo.setEnabled(fNamedJREButton.getSelection());
		fExecEnvsCombo.setEnabled(fExecEnvButton.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fBlockChanges = true;
		IPath jrePath = getTarget().getJREContainer();
		if (jrePath == null || jrePath.equals(JavaRuntime.newDefaultJREContainerPath())) {
			fDefaultJREButton.setSelection(true);
			fExecEnvButton.setSelection(false);
			fNamedJREButton.setSelection(false);
		} else {
			String ee = JavaRuntime.getExecutionEnvironmentId(jrePath);
			if (ee != null) {
				fExecEnvButton.setSelection(true);
				fDefaultJREButton.setSelection(false);
				fNamedJREButton.setSelection(false);
				fExecEnvsCombo.select(fExecEnvsCombo.indexOf(ee));
			} else {
				String vm = JavaRuntime.getVMInstallName(jrePath);
				if (vm != null) {
					fNamedJREButton.setSelection(true);
					fDefaultJREButton.setSelection(false);
					fExecEnvButton.setSelection(false);
					fNamedJREsCombo.select(fNamedJREsCombo.indexOf(vm));
				}
			}
		}

		if (fExecEnvsCombo.getSelectionIndex() == -1)
			fExecEnvsCombo.setText(fExecEnvChoices.first().toString());

		if (fNamedJREsCombo.getSelectionIndex() == -1)
			fNamedJREsCombo.setText(VMUtil.getDefaultVMInstallName());

		updateWidgets();
		super.refresh();
		fBlockChanges = false;
	}

	/**
	 * Opens a preference page and refreshes the combo choices when closed
	 * @param pageID the preference page ID to open
	 */
	private void openPreferencePage(String pageID) {
		fBlockChanges = true;
		PreferencesUtil.createPreferenceDialogOn(fEditor.getEditorSite().getShell(), pageID, new String[] {pageID}, null).open();
		// reset JRE select because either JDT preference page allows user to add/remove JREs
		fNamedJREsCombo.setItems(VMUtil.getVMInstallNames());
		refresh();
		fBlockChanges = false;
	}

}
