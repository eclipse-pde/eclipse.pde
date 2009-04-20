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
package org.eclipse.pde.api.tools.ui.internal.actions;

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Compares {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}s
 * to a given {@link IApiBaseline} to get a delta
 * 
 * @since 1.0.1
 */
public class CompareDialog extends Dialog {

	//widget state ids 
	static final String SETTINGS_SECTION = ApiUIPlugin.PLUGIN_ID + ".api.compare"; //$NON-NLS-1$
	static final String BASELINE_STATE = SETTINGS_SECTION + ".baseline"; //$NON-NLS-1$
	
	public String baseline = null;
	String title = null;;
	Combo baselinecombo = null;

	/**
	 * Constructor
	 * @param provider
	 * @param title
	 */
	public CompareDialog(IShellProvider provider, String title) {
		super(provider);
		this.title = title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(this.title);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		GridData gd = (GridData) comp.getLayoutData();
		gd.widthHint = 310;
		
		SWTFactory.createWrapLabel(comp, ActionMessages.SelectABaseline, 2);
		this.baselinecombo = SWTFactory.createCombo(comp, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY, 2, GridData.FILL_HORIZONTAL, null);
		this.baselinecombo.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				Combo combo = (Combo) e.widget;
				String[] baselineNames = (String[]) combo.getData();
				String selectedBaselineName = baselineNames[combo.getSelectionIndex()];
				CompareDialog.this.baseline = selectedBaselineName;
			}
		});
		initialize();
		return comp;
	}
	
	/**
	 * Initializes the controls
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
				this.baseline = currentBaselineName;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		saveWidgetState();
		super.okPressed();
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
			CompareDialog.this.baseline = value;
			// retrieve the right index in the combo
			int index = 0;
			int length = baselinesNames.length;
			for (int i = 0; i < length; i++) {
				String currentBaselineName = baselinesNames[i];
				if (value.equals(currentBaselineName)) {
					index = i;
					break;
				}
			}
			combo.select(index);
		}
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
		settings.put(BASELINE_STATE, this.baseline);
	}
}