/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import java.util.TreeSet;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class JRESection extends PDESection {
	
	private Button fDefaultJREButton;
	private Button fNamedJREButton;
	private Button fExecEnvButton;
	private ComboPart fNamedJREsCombo;
	private ComboPart fExecEnvsCombo;
	private TreeSet fExecEnvChoices;
	private boolean fBlockChanges;
	private Button fConfigureJREButton;
	
	private static String JRE_PREF_PAGE_ID = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage"; //$NON-NLS-1$
	private static String EE_PREF_PAGE_ID = "org.eclipse.jdt.debug.ui.jreProfiles"; //$NON-NLS-1$

	public JRESection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.JRESection_title);
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
				if (!fBlockChanges)
					getRuntimeInfo().setDefaultJRE();
			}
		});
		
		fNamedJREButton = toolkit.createButton(client, PDEUIMessages.JRESection_JREName, SWT.RADIO);
		fNamedJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				if (!fBlockChanges)
					getRuntimeInfo().setNamedJRE(fNamedJREsCombo.getSelection());
			}
		});
		
		fNamedJREsCombo = new ComboPart();
		fNamedJREsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER);
		fNamedJREsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String[] installs = VMHelper.getVMInstallNames();
		fNamedJREsCombo.setItems(installs);
		fNamedJREsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					getRuntimeInfo().setNamedJRE(fNamedJREsCombo.getSelection());
			}
		});
		
		fConfigureJREButton = toolkit.createButton(client, PDEUIMessages.JRESection_jrePreference, SWT.PUSH);
		fConfigureJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openPreferencePage(JRE_PREF_PAGE_ID);
			}
		});
		
		fExecEnvButton = toolkit.createButton(client, PDEUIMessages.JRESection_ExecutionEnv, SWT.RADIO);
		fExecEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				if (!fBlockChanges)
					getRuntimeInfo().setExecutionEnvJRE(fExecEnvsCombo.getSelection());
			}
		});
		
		fExecEnvsCombo = new ComboPart();
		fExecEnvsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER );
		fExecEnvsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fExecEnvsCombo.setItems((String[])fExecEnvChoices.toArray(new String[fExecEnvChoices.size()]));
		fExecEnvsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					getRuntimeInfo().setExecutionEnvJRE(fExecEnvsCombo.getSelection());
			}
		});
		
		Button configureEEButton = toolkit.createButton(client, PDEUIMessages.JRESection_eePreference, SWT.PUSH);
		configureEEButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openPreferencePage(EE_PREF_PAGE_ID);
			}
		});
		configureEEButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		section.setClient(client);
		
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
 		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
 			handleModelEventWorldChanged(e);
 		}
	}
	
	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Perform the refresh
		refresh();
		// Note:  A deferred selection event is fired from radio buttons when
		// their value is toggled, the user switches to another page, and the
		// user switches back to the same page containing the radio buttons
		// This appears to be a result of a SWT bug.
		// If the radio button is the last widget to have focus when leaving 
		// the page, an event will be fired when entering the page again.
		// An event is not fired if the radio button does not have focus.
		// The solution is to redirect focus to a stable widget.
		getPage().setLastFocusControl(fConfigureJREButton);			
	}		

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		ITargetModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}	
	
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
	
	private ITargetJRE getRuntimeInfo() {
		ITargetJRE info = getTarget().getTargetJREInfo();
		if (info == null) {
			info = getModel().getFactory().createJREInfo();
			getTarget().setTargetJREInfo(info);
		}
		return info;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	public void refresh() {
		fBlockChanges = true;
		ITargetJRE info = getRuntimeInfo();

		int jreType = info.getJREType();		
		fDefaultJREButton.setSelection(jreType == ITargetJRE.TYPE_DEFAULT);
		fNamedJREButton.setSelection(jreType == ITargetJRE.TYPE_NAMED);
		fExecEnvButton.setSelection(jreType == ITargetJRE.TYPE_EXECUTION_ENV);
		
		String jreName = info.getJREName();
		if (jreType == ITargetJRE.TYPE_NAMED) {
			if (fNamedJREsCombo.indexOf(jreName) < 0)
				fNamedJREsCombo.add(jreName);
			fNamedJREsCombo.setText(jreName);
		} else if (jreType == ITargetJRE.TYPE_EXECUTION_ENV) {
			if (fExecEnvsCombo.indexOf(jreName) < 0)
				fExecEnvsCombo.add(jreName);
			fExecEnvsCombo.setText(jreName);
		} 
		
		if (fExecEnvsCombo.getSelectionIndex() == -1)
			fExecEnvsCombo.setText(fExecEnvChoices.first().toString());
		
		if (fNamedJREsCombo.getSelectionIndex() == -1)
			fNamedJREsCombo.setText(VMHelper.getDefaultVMInstallName());	
		
		updateWidgets();
		super.refresh();
		fBlockChanges = false;
	}
	
	private void openPreferencePage(String pageID) {
		fBlockChanges = true;
		PreferencesUtil.createPreferenceDialogOn(getPage().getEditor().getEditorSite().getShell(), pageID, new String[] { pageID }, null).open();
		// reset JRE select because either JDT preference page allows user to add/remove JREs
		fNamedJREsCombo.setItems(VMHelper.getVMInstallNames());
		fBlockChanges = false;
	}
	
}
