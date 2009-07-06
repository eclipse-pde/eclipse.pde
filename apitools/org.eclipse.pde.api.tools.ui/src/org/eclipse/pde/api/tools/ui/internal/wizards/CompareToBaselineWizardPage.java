/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.actions.ActionMessages;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiBaselinePreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;


/**
 * Wizard page for the compare UI elements
 * 
 * @since 1.0.1
 */
public class CompareToBaselineWizardPage extends WizardPage {
	
	//widget state ids 
	static final String SETTINGS_SECTION = ApiUIPlugin.PLUGIN_ID + ".api.compare"; //$NON-NLS-1$
	static final String BASELINE_STATE = SETTINGS_SECTION + ".baseline"; //$NON-NLS-1$
	
	private IStructuredSelection selection = null;
	private Combo baselinecombo = null;
	String baselineName = null;
	private Link link = null;
	
	/**
	 * Constructor
	 * @param selection
	 * @param pageName
	 */
	protected CompareToBaselineWizardPage(IStructuredSelection selection) {
		super(ActionMessages.CompareDialogTitle);
		this.selection = selection;
		setTitle(ActionMessages.CompareToBaselineWizardPage_compare_with_baseline);
		setMessage(ActionMessages.CompareToBaselineWizardPage_compare_with_selected_baseline);
		setImageDescriptor(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_WIZBAN_COMPARE_TO_BASELINE));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		setControl(comp);
		
		SWTFactory.createLabel(comp, ActionMessages.SelectABaseline, 1);
		this.baselinecombo = SWTFactory.createCombo(comp, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL, null);
		this.baselinecombo.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				Combo combo = (Combo) e.widget;
				String[] baselineNames = (String[]) combo.getData();
				String selectedBaselineName = baselineNames[combo.getSelectionIndex()];
				CompareToBaselineWizardPage.this.baselineName = selectedBaselineName;
				setPageComplete(isPageComplete());
			}
		});
		this.link = SWTFactory.createLink(comp, ActionMessages.AddNewBaseline, JFaceResources.getDialogFont(), 1, GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IAdaptable element = getAdaptable();
				if(element == null) {
					return;
				}
				CompareToBaselineWizardPage.this.baselineName = null;
				SWTFactory.showPreferencePage(getShell(), ApiBaselinePreferencePage.ID, element);
				initialize();
				setPageComplete(isPageComplete());
			}
		});
		link.setToolTipText(ActionMessages.CompareToBaselineWizardPage_open_baseline_pref_page);
		//do initialization
		initialize();
		getShell().pack();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_COMPARE_WIZARD_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		if(this.baselineName == null || Util.EMPTY_STRING.equals(this.baselineName)) {
			setMessage(ActionMessages.CompareToBaselineWizardPage_create_baseline);
			this.link.forceFocus();
			return false;
		}
		setMessage(ActionMessages.CompareToBaselineWizardPage_compare_with_selected_baseline);
		this.baselinecombo.setFocus();
		return true;
	}
	
	/**
	 * Initialize the page controls, etc
	 */
	void initialize() {
		IApiBaselineManager apiBaselineManager = ApiPlugin.getDefault().getApiBaselineManager();
		IApiBaseline defaultBaseline = apiBaselineManager.getDefaultApiBaseline();
		String defaultBaselineName = defaultBaseline != null ? defaultBaseline.getName() : null;
		IApiBaseline[] baselines = apiBaselineManager.getApiBaselines();
		int length = baselines.length;
		String[] baselinesItems = new String[length];
		String[] baselinesNames = new String[length];
		// set the names
		for (int i = 0; i < length; i++) {
			String currentBaselineName = baselines[i].getName();
			baselinesNames[i] = currentBaselineName;
		}
		Arrays.sort(baselinesNames);
		// set the labels
		int index = 0;
		for (int i = 0; i < length; i++) {
			String currentBaselineName = baselinesNames[i];
			if (defaultBaselineName != null && defaultBaselineName.equals(currentBaselineName)) {
				baselinesItems[i] = NLS.bind(ActionMessages.SetAsDefault, currentBaselineName);
				this.baselineName = currentBaselineName;
				index = i;
			} else {
				baselinesItems[i] = currentBaselineName;
			}
		}
		this.baselinecombo.setItems(baselinesItems);
		this.baselinecombo.setData(baselinesNames);
		this.baselinecombo.select(index);
		
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if(settings != null) {
			restoreComboSelection(this.baselinecombo, BASELINE_STATE, settings, baselinesNames);
		}
	}
	
	/**
	 * Restores the selected item for the given combo based on the stored value from the 
	 * dialog settings
	 * 
	 * @param combo
	 * @param id
	 * @param settings
	 */
	private void restoreComboSelection(Combo combo, String id, IDialogSettings settings, String[] baselinesNames) {
		String value = settings.get(id);
		if(value != null) {
			// retrieve the right index in the combo
			int index = 0;
			int length = baselinesNames.length;
			for (int i = 0; i < length; i++) {
				String currentBaselineName = baselinesNames[i];
				if (value.equals(currentBaselineName)) {
					this.baselineName = value;
					index = i;
					break;
				}
			}
			combo.select(index);
		}
	}
	
	/**
	 * Returns the {@link IAdaptable} from the current selection context
	 * @param selection
	 * @return the {@link IAdaptable} for the current selection context
	 */
	IAdaptable getAdaptable() {
		Object o = this.selection.getFirstElement();
		if(o instanceof IAdaptable) {
			IAdaptable adapt = (IAdaptable) o;
			IResource resource = (IResource) adapt.getAdapter(IResource.class);
			if(resource != null) {
				return (resource instanceof IProject ? resource : resource.getProject());
			}
		}
		return null;
	}
	
	/**
	 * Performs the finishing actions from this page
	 */
	boolean finish() {
		saveWidgetState();
		final IApiBaseline baseline = ApiBaselineManager.getManager().getApiBaseline(this.baselineName);
		if (baseline == null) {
			return false;
		}
		CompareOperation op = new CompareOperation(baseline, this.selection);
		op.setSystem(false);
		op.setPriority(Job.LONG);
		op.schedule();
		return true;
	}
	
	/**
	 * Saves the state of the widgets on the page
	 */
	void saveWidgetState() {
		IDialogSettings rootsettings = ApiUIPlugin.getDefault().getDialogSettings();
		IDialogSettings settings = rootsettings.getSection(SETTINGS_SECTION);
		if(settings == null) {
			settings = rootsettings.addNewSection(SETTINGS_SECTION);
		}
		settings.put(BASELINE_STATE, this.baselineName);
	}
}
