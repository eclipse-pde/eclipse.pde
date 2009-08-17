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
package org.eclipse.pde.api.tools.ui.internal.use;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * Tab for an API use scan
 */
public class ApiUseScanTab extends AbstractLaunchConfigurationTab {
	
	static final String[] EXTENSIONS = new String[] {"*.txt"}; //$NON-NLS-1$
	
	/**
	 * Default selection adapter for updating the launch dialog
	 */
	SelectionAdapter selectionadapter = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			updateLaunchConfigurationDialog();
		};
	};
	
	/**
	 * Default modify adapter for updating the launch dialog
	 */
	ModifyListener modifyadapter = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	};
	
	boolean initializing = false;
	Combo baseline, targetCombo;
	Button radioBaseline, radioTarget, radioInstall, radioWorkspace;
	Button baselinesButton, targetsButton, installButton;
	ITargetHandle[] targetHandles = new ITargetHandle[0];
	Text installLocation;
	Text searchScope = null;
	Text targetScope = null;
	Text reportlocation = null;
	Text htmllocation = null;
	Button includesystemlibs = null,
		   considerapi = null,
		   considerinternal = null,
		   createhtml = null,
		   browsehtmllocation = null,
		   openreport = null,
		   cleanreportlocation = null,
		   cleanhtmllocation = null;

	/**
	 * Image handle so it can be disposed
	 */
	private Image image = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		
		Group group = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_analuze, 3, 3, GridData.FILL_HORIZONTAL);
		this.radioBaseline = SWTFactory.createRadioButton(group, Messages.ApiUseScanTab_api_baseline);
		this.radioBaseline.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		this.baseline = SWTFactory.createCombo(group, 
				SWT.BORDER | SWT.FLAT | SWT.READ_ONLY, 
				1, 
				GridData.BEGINNING | GridData.FILL_HORIZONTAL, 
				null);
		GridData gd = (GridData) this.baseline.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		this.baseline.addSelectionListener(selectionadapter);
		this.baselinesButton = SWTFactory.createPushButton(group, Messages.ApiUseScanTab_baselines, null);
		this.baselinesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int bef = ApiUseScanTab.this.baseline.getSelectionIndex();
				String name = null;
				if (bef >= 0) {
					name = ApiUseScanTab.this.baseline.getItem(bef);
				}
				SWTFactory.showPreferencePage(getShell(), "org.eclipse.pde.api.tools.ui.apiprofiles.prefpage", null); //$NON-NLS-1$
				updateAvailableBaselines();
				if (name != null) {
					String[] items = ApiUseScanTab.this.baseline.getItems();
					for (int i = 0; i < items.length; i++) {
						if (name.equals(items[i])) {
							ApiUseScanTab.this.baseline.select(i);
							break;
						}
					}
				}
				updateLaunchConfigurationDialog();
			}
		});
		
		this.radioTarget = SWTFactory.createRadioButton(group, Messages.ApiUseScanTab_target_definitions);
		this.radioTarget.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		this.targetCombo = SWTFactory.createCombo(group, 
				SWT.BORDER | SWT.FLAT | SWT.READ_ONLY, 
				1, 
				GridData.BEGINNING | GridData.FILL_HORIZONTAL, 
				null);
		gd = (GridData) this.targetCombo.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		this.targetCombo.addSelectionListener(selectionadapter);
		this.targetsButton = SWTFactory.createPushButton(group, Messages.ApiUseScanTab_targets, null);
		this.targetsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = ApiUseScanTab.this.targetCombo.getSelectionIndex();
				ITargetHandle handle = null;
				if (index >= 0) {
					handle = ApiUseScanTab.this.targetHandles[index];
				}
				SWTFactory.showPreferencePage(getShell(), "org.eclipse.pde.ui.TargetPlatformPreferencePage", null); //$NON-NLS-1$
				updateAvailableTargets();
				if (handle != null) {
					for (int i = 0; i < targetHandles.length; i++) {
						if (handle.equals(targetHandles[i])) {
							targetCombo.select(i);
							break;
						}
					}
				}
				updateLaunchConfigurationDialog();
			}
		});
		
		this.radioInstall = SWTFactory.createRadioButton(group, Messages.ApiUseScanTab_install_location);
		this.radioInstall.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		this.installLocation = SWTFactory.createText(group, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		gd = (GridData) this.installLocation.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		this.installButton = SWTFactory.createPushButton(group, Messages.ApiUseScanTab_browse, null);
		this.installButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				handleFolderBrowse(ApiUseScanTab.this.installLocation, Messages.ApiUseScanTab_select_install_location);
				updateLaunchConfigurationDialog();
			}
		});
		
		this.radioWorkspace = SWTFactory.createRadioButton(group, Messages.ApiUseScanTab_workspace_projects);
		this.radioWorkspace.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		gd = (GridData) this.radioWorkspace.getLayoutData();
		gd.horizontalSpan = 3;
		
		group = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_search_for, 2, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel(group, Messages.ApiUseScanTab_references_to, 1);
		this.targetScope = SWTFactory.createText(group, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.targetScope.addModifyListener(modifyadapter);		
		this.considerapi = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_api_references, null, true, 2);
		this.considerapi.addSelectionListener(selectionadapter);
		this.considerinternal = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_internal_references, null, true, 2);
		this.considerinternal.addSelectionListener(selectionadapter);		
		
		group = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_search_in, 2, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel(group, Messages.ApiUseScanTab_bundles_matching, 1);
		this.searchScope = SWTFactory.createText(group, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.searchScope.addModifyListener(modifyadapter);
		this.includesystemlibs = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_system_libs, null, false, 2);
		this.includesystemlibs.addSelectionListener(selectionadapter);
		
		group = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_reporting, 2, 2, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel(group, Messages.ApiUseScanTab_report_location, 2);
		this.reportlocation = SWTFactory.createText(group, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.reportlocation.addModifyListener(modifyadapter);
		gd = (GridData) this.reportlocation.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		Button browse = SWTFactory.createPushButton(group, Messages.ApiUseScanTab_brows_e_, null);
		browse.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				handleFolderBrowse(ApiUseScanTab.this.reportlocation, "Select the location to write the report to"); //$NON-NLS-1$
			}
		});
		this.cleanreportlocation = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_clean_report_dir, null, false, 2);
		gd = (GridData) this.cleanreportlocation.getLayoutData();
		this.cleanreportlocation.addSelectionListener(selectionadapter);
		gd.horizontalIndent = 10;
		
		this.createhtml = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_create_html_report, null, false, 2);
		this.createhtml.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = ((Button)e.widget).getSelection();
				ApiUseScanTab.this.htmllocation.setEnabled(enabled);
				ApiUseScanTab.this.browsehtmllocation.setEnabled(enabled);
				ApiUseScanTab.this.cleanhtmllocation.setEnabled(enabled);
				ApiUseScanTab.this.openreport.setEnabled(enabled);
				updateLaunchConfigurationDialog();
			}
		});
		this.htmllocation = SWTFactory.createText(group, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.htmllocation.addModifyListener(modifyadapter);
		this.htmllocation.setEnabled(false);
		gd = (GridData) this.htmllocation.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalIndent = 10;
		this.browsehtmllocation = SWTFactory.createPushButton(group, Messages.ApiUseScanTab__b_rowse, null);
		this.browsehtmllocation.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				handleFolderBrowse(ApiUseScanTab.this.htmllocation, "Select the location to write the HTML report to"); //$NON-NLS-1$
			}
		});
		this.browsehtmllocation.setEnabled(false);
		this.cleanhtmllocation = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_clean_html_report_dir, null, false, 2);
		gd = (GridData) this.cleanhtmllocation.getLayoutData();
		gd.horizontalIndent = 10;
		this.cleanhtmllocation.addSelectionListener(selectionadapter);
		this.openreport = SWTFactory.createCheckButton(group, Messages.ApiUseScanTab_open_report, null, false, 2);
		gd = (GridData) this.openreport.getLayoutData();
		gd.horizontalIndent = 10;
		this.openreport.setEnabled(false);
		this.openreport.addSelectionListener(selectionadapter);
		setControl(comp);
	}
	
	/**
	 * The selected target has changed (radio selection). Update control enabled state and dialog.
	 */
	void updateTarget() {
		switch (getTargetKind()) {
			case ApiUseLaunchDelegate.KIND_API_BASELINE: {
				baseline.setEnabled(true);
				baselinesButton.setEnabled(true);
				targetCombo.setEnabled(false);
				targetsButton.setEnabled(false);
				installLocation.setEnabled(false);
				installButton.setEnabled(false);
				break;
			}
			case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION: {
				baseline.setEnabled(false);
				baselinesButton.setEnabled(false);
				targetCombo.setEnabled(true);
				targetsButton.setEnabled(true);
				installLocation.setEnabled(false);
				installButton.setEnabled(false);
				break;
			}
			case ApiUseLaunchDelegate.KIND_INSTALL_PATH: {
				baseline.setEnabled(false);
				baselinesButton.setEnabled(false);
				targetCombo.setEnabled(false);
				targetsButton.setEnabled(false);
				installLocation.setEnabled(true);
				installButton.setEnabled(true);
				break;
			}
			case ApiUseLaunchDelegate.KIND_WORKSPACE: {
				baseline.setEnabled(false);
				baselinesButton.setEnabled(false);
				targetCombo.setEnabled(false);
				targetsButton.setEnabled(false);
				installLocation.setEnabled(false);
				installButton.setEnabled(false);
				break;
			}
		}
		updateLaunchConfigurationDialog();
	}
	
	/**
	 * @return the kind of target selected for the scan
	 */
	private int getTargetKind() {
		if (this.radioBaseline.getSelection()) {
			return ApiUseLaunchDelegate.KIND_API_BASELINE;
		} else if (this.radioTarget.getSelection()) {
			return ApiUseLaunchDelegate.KIND_TARGET_DEFINITION;
		} else if (this.radioInstall.getSelection()) {
			return ApiUseLaunchDelegate.KIND_INSTALL_PATH;
		} else {
			return ApiUseLaunchDelegate.KIND_WORKSPACE;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return Messages.ApiUseScanTab_api_use_report;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		if(this.image == null) {
			this.image = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_SETUP_APITOOLS).createImage();
		}
		return this.image;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		if(this.image != null) {
			this.image.dispose();
		}
		super.dispose();
	}
	/**
	 * Updates available target definitions.
	 */
	void updateAvailableTargets() {
		List names = new ArrayList();
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			ITargetHandle[] handles = service.getTargets(null);
			List defs = new ArrayList();
			for (int i = 0; i < handles.length; i++) {
				try {
					defs.add(handles[i].getTargetDefinition());
				} catch (CoreException e) {
					// Suppress
				}
			}
			Collections.sort(defs, new Comparator() {
				public int compare(Object o1, Object o2) {
					ITargetDefinition d1 = (ITargetDefinition) o1;
					ITargetDefinition d2 = (ITargetDefinition) o2;
					return d1.getName().compareTo(d2.getName());
				}
			});
			targetHandles = new ITargetHandle[defs.size()];
			for (int i = 0; i < defs.size(); i++) {
				ITargetDefinition def = (ITargetDefinition) defs.get(i);
				targetHandles[i] = def.getHandle();
				names.add(def.getName());
			}
		}
		this.targetCombo.setItems((String[]) names.toArray(new String[names.size()]));
	}

	/**
	 * Returns the target service or <code>null</code>
	 * 
	 * @return service or <code>null</code>
	 */
	private ITargetPlatformService getTargetService() {
		return (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
	}

	/**
	 * Updates available baselines.
	 */
	void updateAvailableBaselines() {
		HashSet ids = new HashSet();
		IApiBaseline[] baselines = ApiPlugin.getDefault().getApiBaselineManager().getApiBaselines();
		for (int i = 0; i < baselines.length; i++) {
			ids.add(baselines[i].getName());
		}
		this.baseline.setItems((String[]) ids.toArray(new String[ids.size()]));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		this.initializing = true;
		try {
			updateAvailableBaselines();
			updateAvailableTargets();
			updateRadioButtons(configuration);
			updateTarget();
			restoreComboSelection(this.baseline, configuration.getAttribute(ApiUseLaunchDelegate.BASELINE_NAME, (String)null));
			String memento = configuration.getAttribute(ApiUseLaunchDelegate.TARGET_HANDLE, (String)null);
			if (memento != null) {
				ITargetPlatformService service = getTargetService();
				if (service != null) {
					ITargetHandle handle = service.getTarget(memento);
					for (int i = 0; i < this.targetHandles.length; i++) {
						ITargetHandle th = this.targetHandles[i];
						if (handle.equals(th)) {
							this.targetCombo.select(i);
							break;
						}
					}
				}
			}
			this.installLocation.setText(configuration.getAttribute(ApiUseLaunchDelegate.INSTALL_PATH, "")); //$NON-NLS-1$
			this.considerapi.setSelection(isSpecified(ApiUseLaunchDelegate.MOD_API_REFERENCES, configuration));
			this.considerinternal.setSelection(isSpecified(ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES, configuration));
			this.includesystemlibs.setSelection(isSpecified(ApiUseLaunchDelegate.MOD_SYSTEM_LIBS, configuration));
			this.reportlocation.setText(configuration.getAttribute(ApiUseLaunchDelegate.XML_PATH, "")); //$NON-NLS-1$
			this.cleanreportlocation.setSelection(isSpecified(ApiUseLaunchDelegate.CLEAN_XML, configuration));
			boolean enabled = isSpecified(ApiUseLaunchDelegate.CREATE_HTML, configuration);
			this.createhtml.setSelection(enabled);
			this.openreport.setEnabled(enabled);
			this.cleanhtmllocation.setEnabled(enabled);
			this.htmllocation.setEnabled(enabled);
			this.browsehtmllocation.setEnabled(enabled);
			this.htmllocation.setText(configuration.getAttribute(ApiUseLaunchDelegate.HTML_PATH, "")); //$NON-NLS-1$
			this.openreport.setSelection(isSpecified(ApiUseLaunchDelegate.DISPLAY_REPORT, configuration));
			this.cleanhtmllocation.setSelection(isSpecified(ApiUseLaunchDelegate.CLEAN_HTML, configuration));
			this.searchScope.setText(configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, "")); //$NON-NLS-1$
			this.targetScope.setText(configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, "")); //$NON-NLS-1$
		} catch (CoreException e) {
			setErrorMessage(e.getStatus().getMessage());
		}
		finally {
			this.initializing = false;
		}
	}
	
	/**
	 * Updates the radio button group in {@link #initializeFrom(ILaunchConfiguration)} to ensure
	 * the radio group stays up-to-date when a revert is performed
	 * 
	 * @param configuration
	 */
	void updateRadioButtons(ILaunchConfiguration configuration) throws CoreException {
		int kind = configuration.getAttribute(ApiUseLaunchDelegate.TARGET_KIND, 0);
		switch (kind) {
		case ApiUseLaunchDelegate.KIND_API_BASELINE: {
			radioBaseline.setSelection(true);
			radioTarget.setSelection(false);
			radioInstall.setSelection(false);
			radioWorkspace.setSelection(false);
			break;
		}
		case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION: {
			radioBaseline.setSelection(false);
			radioTarget.setSelection(true);
			radioInstall.setSelection(false);
			radioWorkspace.setSelection(false);
			break;
		}
		case ApiUseLaunchDelegate.KIND_INSTALL_PATH: {
			radioBaseline.setSelection(false);
			radioTarget.setSelection(false);
			radioInstall.setSelection(true);
			radioWorkspace.setSelection(false);
			break;
		}
		case ApiUseLaunchDelegate.KIND_WORKSPACE: {
			radioBaseline.setSelection(false);
			radioTarget.setSelection(false);
			radioInstall.setSelection(false);
			radioWorkspace.setSelection(true);
			break;
		}
		}
	}
	
	private boolean isSpecified(int modifier, ILaunchConfiguration configuration) throws CoreException {
		int modifiers = configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_MODIFIERS, 0);
		return (modifiers & modifier) > 0;
	}
	
	/**
	 * Restores the selected item for the given combo based on the stored value from the 
	 * configuration
	 * 
	 * @param combo
	 * @param value
	 * @param settings
	 */
	private void restoreComboSelection(Combo combo, String value) {
		int idx = -1;
		if(value != null) {
			idx = combo.indexOf(value);
			if(idx > -1) {
				combo.select(idx);
			}
			else {
				combo.select(0);
			}
		}
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ApiUseLaunchDelegate.TARGET_KIND, getTargetKind());
		configuration.setAttribute(ApiUseLaunchDelegate.BASELINE_NAME, this.baseline.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.INSTALL_PATH, this.installLocation.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.TARGET_HANDLE, getTargetMemento());
		int modifiers = 0;
		modifiers = consider(this.openreport, ApiUseLaunchDelegate.DISPLAY_REPORT, modifiers);
		modifiers = consider(this.cleanhtmllocation, ApiUseLaunchDelegate.CLEAN_HTML, modifiers);
		modifiers = consider(this.cleanreportlocation, ApiUseLaunchDelegate.CLEAN_XML, modifiers);
		modifiers = consider(this.considerapi, ApiUseLaunchDelegate.MOD_API_REFERENCES, modifiers);
		modifiers = consider(this.considerinternal, ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES, modifiers);
		modifiers = consider(this.createhtml, ApiUseLaunchDelegate.CREATE_HTML, modifiers);
		modifiers = consider(this.includesystemlibs, ApiUseLaunchDelegate.MOD_SYSTEM_LIBS, modifiers);
		configuration.setAttribute(ApiUseLaunchDelegate.SEARCH_MODIFIERS, modifiers);
		configuration.setAttribute(ApiUseLaunchDelegate.HTML_PATH, this.htmllocation.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.XML_PATH, this.reportlocation.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, this.searchScope.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, this.targetScope.getText().trim());
	}
	
	/**
	 * Returns the memento for the selected target definition or <code>null</code> if none.
	 * 
	 * @return memento or <code>null</code>
	 */
	private String getTargetMemento() {
		ITargetHandle handle = getTargetHandle();
		if (handle == null) {
			return null;
		}
		try {
			return handle.getMemento();
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Returns the handle of the selected target or <code>null</code> if none.
	 * 
	 * @return target handle or <code>null</code>
	 */
	private ITargetHandle getTargetHandle() {
		int index = this.targetCombo.getSelectionIndex();
		if (index >= 0) {
			return this.targetHandles[index];
		}
		return null;
	}
	
	private int consider(Button button, int mask, int modifiers) {
		if (button.getSelection()) {
			return modifiers | mask;
		}
		return modifiers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		int modifiers = ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES;
		configuration.setAttribute(ApiUseLaunchDelegate.SEARCH_MODIFIERS, modifiers);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		if(this.initializing) {
			return true;
		}
		setErrorMessage(null);
		
		String text = this.reportlocation.getText().trim();
		if(IApiToolsConstants.EMPTY_STRING.equals(text)) {
			setErrorMessage(Messages.ApiUseScanTab_enter_report_location);
			return false;
		}
		return true;
	}

	/**
	 * Handles browsing for a file with a given set of valid extensions
	 * @param text
	 * @param message
	 */
	void handleFileBrowse(Text text, String message, String[] extensions) {
		FileDialog dialog = new FileDialog(getShell());
		dialog.setFilterExtensions(extensions);
		String loctext = text.getText().trim();
		if (loctext.length() > 0) {
			dialog.setFilterPath(loctext);
		}
		String newpath = dialog.open();
		if(newpath != null) {
			text.setText(newpath);
		}
	}
	
	/**
	 * Handles the Browse... button being selected
	 * @param text
	 */
	void handleFolderBrowse(Text text, String message) {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(message);
		String loctext = text.getText().trim();
		if (loctext.length() > 0) {
			dialog.setFilterPath(loctext);
		}
		String newpath = dialog.open();
		if(newpath != null) {
			text.setText(newpath);
		}
	}	
}
