/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Tab for an API use scan
 * @since 1.1
 */
public class ApiUseScanTab extends AbstractLaunchConfigurationTab {
	
	static final String[] EXTENSIONS = new String[] {"*.txt"}; //$NON-NLS-1$
	
	/**
	 * Default selection adapter for updating the launch dialog
	 */
	SelectionAdapter selectionadapter = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			updateDialog();
		};
	};
	
	/**
	 * Default modify adapter for updating the launch dialog
	 */
	ModifyListener modifyadapter = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			updateDialog();
		}
	};
	
	boolean initializing = false;
	Combo baseline, targetCombo, reportTypeCombo;
	Button radioBaseline = null, 
		   radioTarget = null, 
		   radioInstall = null,
		   radioReportOnly = null,
		   baselinesButton = null, 
		   targetsButton = null, 
		   installButton = null,
		   considerapi = null,
		   considerinternal = null,
		   consideruse = null,
		   createhtml = null,
		   openreport = null,
		   cleanreportlocation = null,
		   cleanhtmllocation = null;
	ITargetHandle[] targetHandles = new ITargetHandle[0];
	Text installLocation = null,
		 searchScope = null,
		 targetScope = null,
		 reportlocation = null,
		 description = null;
	Group searchForGroup = null,
		  searchInGroup = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		GridLayout layout = (GridLayout) comp.getLayout();
		layout.makeColumnsEqualWidth = true;
		
		Group reportGroup = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_analuze, 3, 3, GridData.FILL_HORIZONTAL);
		this.radioBaseline = SWTFactory.createRadioButton(reportGroup, Messages.ApiUseScanTab_api_baseline);
		this.radioBaseline.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		this.baseline = SWTFactory.createCombo(reportGroup, 
				SWT.BORDER | SWT.FLAT | SWT.READ_ONLY, 
				1, 
				GridData.BEGINNING | GridData.FILL_HORIZONTAL, 
				null);
		GridData gd = (GridData) this.baseline.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		this.baseline.addSelectionListener(selectionadapter);
		this.baselinesButton = SWTFactory.createPushButton(reportGroup, Messages.ApiUseScanTab_baselines, null);
		this.baselinesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int bef = ApiUseScanTab.this.baseline.getSelectionIndex();
				String name = null;
				if (bef >= 0) {
					name = ApiUseScanTab.this.baseline.getItem(bef);
				}
				SWTFactory.showPreferencePage(getTabShell(), "org.eclipse.pde.api.tools.ui.apiprofiles.prefpage", null); //$NON-NLS-1$
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
				updateDialog();
			}
		});
		
		this.radioTarget = SWTFactory.createRadioButton(reportGroup, Messages.ApiUseScanTab_target_definitions);
		this.radioTarget.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		this.targetCombo = SWTFactory.createCombo(reportGroup, 
				SWT.BORDER | SWT.FLAT | SWT.READ_ONLY, 
				1, 
				GridData.BEGINNING | GridData.FILL_HORIZONTAL, 
				null);
		gd = (GridData) this.targetCombo.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		this.targetCombo.addSelectionListener(selectionadapter);
		this.targetsButton = SWTFactory.createPushButton(reportGroup, Messages.ApiUseScanTab_targets, null);
		this.targetsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = ApiUseScanTab.this.targetCombo.getSelectionIndex();
				ITargetHandle handle = null;
				if (index >= 0) {
					handle = ApiUseScanTab.this.targetHandles[index];
				}
				SWTFactory.showPreferencePage(getTabShell(), "org.eclipse.pde.ui.TargetPlatformPreferencePage", null); //$NON-NLS-1$
				updateAvailableTargets();
				if (handle != null) {
					for (int i = 0; i < targetHandles.length; i++) {
						if (handle.equals(targetHandles[i])) {
							targetCombo.select(i);
							break;
						}
					}
				}
				updateDialog();
			}
		});
		
		this.radioInstall = SWTFactory.createRadioButton(reportGroup, Messages.ApiUseScanTab_install_location);
		this.radioInstall.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		this.installLocation = SWTFactory.createText(reportGroup, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		gd = (GridData) this.installLocation.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		this.installLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				scheduleUpdate();
			}
		});
		
		this.installButton = SWTFactory.createPushButton(reportGroup, Messages.ApiUseScanTab_browse, null);
		this.installButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				handleFolderBrowse(ApiUseScanTab.this.installLocation, Messages.ApiUseScanTab_select_install_location);
				updateDialog();
			}
		});
		this.radioReportOnly = SWTFactory.createRadioButton(reportGroup, Messages.ApiUseScanTab_generate_html_only);
		gd = (GridData)this.radioReportOnly.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		this.radioReportOnly.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTarget();
			}
		});
		
		searchForGroup = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_search_for, 2, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel(searchForGroup, Messages.ApiUseScanTab_references_to, 1);
		this.targetScope = SWTFactory.createText(searchForGroup, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.targetScope.addModifyListener(modifyadapter);		
		this.considerapi = SWTFactory.createCheckButton(searchForGroup, Messages.ApiUseScanTab_api_references, null, true, 2);
		this.considerapi.addSelectionListener(selectionadapter);
		this.considerinternal = SWTFactory.createCheckButton(searchForGroup, Messages.ApiUseScanTab_internal_references, null, true, 2);
		this.considerinternal.addSelectionListener(selectionadapter);		
		this.consideruse = SWTFactory.createCheckButton(searchForGroup, Messages.ApiUseScanTab_illegal_api_use, null, true, 2);
		this.consideruse.addSelectionListener(selectionadapter);
		
		searchInGroup = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_search_in, 2, 1, GridData.FILL_HORIZONTAL);
		gd = (GridData)searchInGroup.getLayoutData();
		gd.verticalAlignment = SWT.FILL;
		SWTFactory.createLabel(searchInGroup, Messages.ApiUseScanTab_bundles_matching, 1);
		this.searchScope = SWTFactory.createText(searchInGroup, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.searchScope.addModifyListener(modifyadapter);
		
		reportGroup = SWTFactory.createGroup(comp, Messages.ApiUseScanTab_reporting, 2, 2, GridData.FILL_HORIZONTAL);
		
		Composite reportTypeComp = SWTFactory.createComposite(reportGroup, 2, 2, GridData.BEGINNING, 0, 0);
		SWTFactory.createLabel(reportTypeComp, Messages.ApiUseScanTab_reportType, 1);
		reportTypeCombo = SWTFactory.createCombo(reportTypeComp, SWT.READ_ONLY, 1, GridData.FILL_BOTH, new String[] {Messages.ApiUseScanTab_referencedBundlesReport, Messages.ApiUseScanTab_referencingBundlesReport});
		reportTypeCombo.addSelectionListener(selectionadapter);
		
		SWTFactory.createLabel(reportGroup, Messages.ApiUseScanTab_report_location, 2);
		this.reportlocation = SWTFactory.createText(reportGroup, SWT.SINGLE | SWT.FLAT | SWT.BORDER, 1, GridData.FILL_HORIZONTAL);
		this.reportlocation.addModifyListener(modifyadapter);
		gd = (GridData) this.reportlocation.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		Button browse = SWTFactory.createPushButton(reportGroup, Messages.ApiUseScanTab_brows_e_, null);
		browse.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				handleFolderBrowse(ApiUseScanTab.this.reportlocation, Messages.ApiUseScanTab_SelectDirectory);
			}
		});
		this.cleanreportlocation = SWTFactory.createCheckButton(reportGroup, Messages.ApiUseScanTab_clean_report_dir, null, false, 2);
		gd = (GridData) this.cleanreportlocation.getLayoutData();
		this.cleanreportlocation.addSelectionListener(selectionadapter);
		gd.horizontalIndent = 10;
		
		this.createhtml = SWTFactory.createCheckButton(reportGroup, Messages.ApiUseScanTab_create_html_report, null, false, 2);
		this.createhtml.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateReportOptions();
				updateDialog();
			}
		});
		this.cleanhtmllocation = SWTFactory.createCheckButton(reportGroup, Messages.ApiUseScanTab_clean_html_report_dir, null, false, 2);
		gd = (GridData) this.cleanhtmllocation.getLayoutData();
		gd.horizontalIndent = 10;
		this.cleanhtmllocation.addSelectionListener(selectionadapter);
		this.openreport = SWTFactory.createCheckButton(reportGroup, Messages.ApiUseScanTab_open_report, null, false, 2);
		gd = (GridData) this.openreport.getLayoutData();
		gd.horizontalIndent = 10;
		this.openreport.setEnabled(false);
		this.openreport.addSelectionListener(selectionadapter);
		SWTFactory.createLabel(reportGroup, Messages.ApiUseScanTab_description, 1);
		this.description = SWTFactory.createText(reportGroup, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP, 2, GridData.FILL_HORIZONTAL);
		gd = (GridData) this.description.getLayoutData();
		gd.heightHint = 40;
		this.description.addModifyListener(modifyadapter);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_USE_SCAN_TAB);
		setControl(comp);
	}
	
	/**
	 * Avoid synthetic accessor
	 */
	void updateDialog() {
		updateLaunchConfigurationDialog();
	}
	
	/**
	 * Avoid synthetic accessor
	 */
	void scheduleUpdate(){
		scheduleUpdateJob();
	}
	
	/**
	 * Avoid synthetic accessor
	 */
	Shell getTabShell() {
		return getShell();
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
				this.createhtml.setEnabled(true);
				this.cleanreportlocation.setEnabled(true);
				this.description.setEnabled(true);
				setGroupEnablement(this.searchForGroup, true);
				setGroupEnablement(this.searchInGroup, true);
				break;
			}
			case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION: {
				baseline.setEnabled(false);
				baselinesButton.setEnabled(false);
				targetCombo.setEnabled(true);
				targetsButton.setEnabled(true);
				installLocation.setEnabled(false);
				installButton.setEnabled(false);
				this.createhtml.setEnabled(true);
				this.cleanreportlocation.setEnabled(true);
				this.description.setEnabled(true);
				setGroupEnablement(this.searchForGroup, true);
				setGroupEnablement(this.searchInGroup, true);
				break;
			}
			case ApiUseLaunchDelegate.KIND_INSTALL_PATH: {
				baseline.setEnabled(false);
				baselinesButton.setEnabled(false);
				targetCombo.setEnabled(false);
				targetsButton.setEnabled(false);
				installLocation.setEnabled(true);
				installButton.setEnabled(true);
				this.createhtml.setEnabled(true);
				this.cleanreportlocation.setEnabled(true);
				this.description.setEnabled(true);
				setGroupEnablement(this.searchForGroup, true);
				setGroupEnablement(this.searchInGroup, true);
				break;
			}
			case ApiUseLaunchDelegate.KIND_HTML_ONLY: {
				baseline.setEnabled(false);
				baselinesButton.setEnabled(false);
				targetCombo.setEnabled(false);
				targetsButton.setEnabled(false);
				installLocation.setEnabled(false);
				installButton.setEnabled(false);
				this.createhtml.setSelection(true);
				this.createhtml.setEnabled(false);
				this.cleanreportlocation.setEnabled(false);
				this.description.setEnabled(false);
				setGroupEnablement(this.searchForGroup, false);
				setGroupEnablement(this.searchInGroup, false);
				break;
			}
		}
		updateReportOptions();
		updateLaunchConfigurationDialog();
	}
	
	/**
	 * Updates the report options
	 */
	void updateReportOptions() {
		boolean enabled = this.createhtml.getSelection();
		this.openreport.setEnabled(enabled);
		this.cleanhtmllocation.setEnabled(enabled);
	}
	
	/**
	 * Sets the enabled state of all of the child of the given group
	 * @param group
	 * @param enabled
	 */
	void setGroupEnablement(Group group, boolean enabled) {
		Control[] children = group.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].setEnabled(enabled);
		}
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
		} else if(this.radioReportOnly.getSelection()) {
			return ApiUseLaunchDelegate.KIND_HTML_ONLY;
		} else {
			return -1;
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
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_SETUP_APITOOLS);
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
					
					final String name1 = d1.getName();
					final String name2 = d2.getName();
					if (name1 == null) {
						if (name2 == null) {
							return d1.getHandle().toString().compareTo(d2.getHandle().toString());
						}
						return -1;
					} else if (name2 == null) {
						return 1;
					}
					return name1.compareTo(name2);
				}
			});
			targetHandles = new ITargetHandle[defs.size()];
			for (int i = 0; i < defs.size(); i++) {
				ITargetDefinition def = (ITargetDefinition) defs.get(i);
				final ITargetHandle handle = def.getHandle();
				targetHandles[i] = handle;
				final String name = def.getName();
				names.add(name == null ? handle.toString() : name);
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
			restoreComboSelection(this.baseline, configuration.getAttribute(ApiUseLaunchDelegate.BASELINE_NAME, (String)null));
			String memento = configuration.getAttribute(ApiUseLaunchDelegate.TARGET_HANDLE, (String)null);
			if (memento != null) {
				ITargetPlatformService service = getTargetService();
				if (service != null) {
					ITargetHandle handle = service.getTarget(memento);
					for (int i = 0; i < this.targetHandles.length; i++) {
						if (handle.equals(this.targetHandles[i])) {
							this.targetCombo.select(i);
							break;
						}
					}
				}
			}
			if(this.targetCombo.getSelectionIndex() < 0) {
				this.targetCombo.select(0);
			}
			this.installLocation.setText(configuration.getAttribute(ApiUseLaunchDelegate.INSTALL_PATH, IApiToolsConstants.EMPTY_STRING)); 
			this.considerapi.setSelection(isSpecified(ApiUseLaunchDelegate.MOD_API_REFERENCES, configuration));
			this.considerinternal.setSelection(isSpecified(ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES, configuration));
			this.consideruse.setSelection(isSpecified(ApiUseLaunchDelegate.MOD_ILLEGAL_USE, configuration));
			
			int reportType = configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TYPE, ApiUseLaunchDelegate.REPORT_KIND_PRODUCER);
			if (reportType == ApiUseLaunchDelegate.REPORT_KIND_CONSUMER){
				this.reportTypeCombo.select(1);
			} else {
				this.reportTypeCombo.select(0);
			}
			
			this.reportlocation.setText(configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATH, IApiToolsConstants.EMPTY_STRING)); 
			this.cleanreportlocation.setSelection(isSpecified(ApiUseLaunchDelegate.CLEAN_XML, configuration));
			boolean enabled = isSpecified(ApiUseLaunchDelegate.CREATE_HTML, configuration);
			this.createhtml.setSelection(enabled);
			this.openreport.setEnabled(enabled);
			this.cleanhtmllocation.setEnabled(enabled);
			this.openreport.setSelection(isSpecified(ApiUseLaunchDelegate.DISPLAY_REPORT, configuration));
			this.cleanhtmllocation.setSelection(isSpecified(ApiUseLaunchDelegate.CLEAN_HTML, configuration));
			this.searchScope.setText(configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, IApiToolsConstants.EMPTY_STRING)); 
			this.targetScope.setText(configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, IApiToolsConstants.EMPTY_STRING)); 
			this.description.setText(configuration.getAttribute(ApiUseLaunchDelegate.DESCRIPTION, IApiToolsConstants.EMPTY_STRING)); 
			updateTarget();
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
				radioReportOnly.setSelection(false);
				break;
			}
			case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION: {
				radioBaseline.setSelection(false);
				radioTarget.setSelection(true);
				radioInstall.setSelection(false);
				radioReportOnly.setSelection(false);
				break;
			}
			case ApiUseLaunchDelegate.KIND_INSTALL_PATH: {
				radioBaseline.setSelection(false);
				radioTarget.setSelection(false);
				radioInstall.setSelection(true);
				radioReportOnly.setSelection(false);
				break;
			}
			case ApiUseLaunchDelegate.KIND_HTML_ONLY: {
				radioBaseline.setSelection(false);
				radioTarget.setSelection(false);
				radioInstall.setSelection(false);
				radioReportOnly.setSelection(true);
				break;
			}
			default: {
				radioBaseline.setSelection(true);
				radioTarget.setSelection(false);
				radioInstall.setSelection(false);
				radioReportOnly.setSelection(false);
			}
		}
	}
	
	/**
	 * Returns <code>true</code> if the given modifier is set in the configuration
	 * @param modifier
	 * @param configuration
	 * @return
	 * @throws CoreException
	 */
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
		else {
			combo.select(0);
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
		modifiers = consider(this.consideruse, ApiUseLaunchDelegate.MOD_ILLEGAL_USE, modifiers);
		modifiers = consider(this.createhtml, ApiUseLaunchDelegate.CREATE_HTML, modifiers);
		configuration.setAttribute(ApiUseLaunchDelegate.SEARCH_MODIFIERS, modifiers);
		IPath path = new Path(this.reportlocation.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.REPORT_PATH, path.toPortableString());
		configuration.setAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, this.searchScope.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, this.targetScope.getText().trim());
		configuration.setAttribute(ApiUseLaunchDelegate.DESCRIPTION, this.description.getText().trim());
		if (reportTypeCombo.getSelectionIndex() == 1){
			configuration.setAttribute(ApiUseLaunchDelegate.REPORT_TYPE, ApiUseLaunchDelegate.REPORT_KIND_CONSUMER);
		} else {
			// TODO This will likely make the config dirty
			configuration.setAttribute(ApiUseLaunchDelegate.REPORT_TYPE, ApiUseLaunchDelegate.REPORT_KIND_PRODUCER);
		}
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
		configuration.setAttribute(ApiUseLaunchDelegate.REPORT_TYPE, ApiUseLaunchDelegate.REPORT_KIND_PRODUCER);
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
		if(!this.considerapi.getSelection() && !this.considerinternal.getSelection() && !this.consideruse.getSelection()) {
			setErrorMessage(Messages.ApiUseScanTab_must_search_something);
			return false;
		}
		text = this.searchScope.getText().trim();
		try {
			Pattern.compile(text);
		}
		catch(PatternSyntaxException pse) {
			setErrorMessage(NLS.bind(Messages.ApiUseScanTab_regex_problem, pse.getPattern()));
			return false;
		}
		text = this.targetScope.getText().trim();
		try {
			Pattern.compile(text);
		}
		catch(PatternSyntaxException pse) {
			setErrorMessage(NLS.bind(Messages.ApiUseScanTab_regex_problem, pse.getPattern()));
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
