/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.search.UseScanManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Preference page to allow users to add use scans.  The use scans are analyzed in the 
 * API Tools builder to see if any methods found in the scan have been removed.
 * 
 * @since 3.7
 */
public class ApiUseScanPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.eclipse.pde.api.tools.ui.apiusescan.prefpage"; //$NON-NLS-1$

	private IWorkingCopyManager fManager;
	CheckboxTableViewer fTableViewer;
	List fLocationList = new ArrayList();
	private Spinner fSpinner;
	Button remove = null;
	Button editbutton = null;

	/**
	 * Column provider for the use scan table
	 */
	class TableColumnLabelProvider extends ColumnLabelProvider {
		
		Image archive = null;
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
		 */
		public void dispose() {
			if(archive != null) {
				archive.dispose();
			}
			super.dispose();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			File file = new File(element.toString());
			if (file.isDirectory()) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER);
			}
			if(archive == null) {
				ImageDescriptor image = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(file.getName());
				archive = image.createImage();
			}
			return archive;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiUseScanPreferencePage_0, 2, 200);
		SWTFactory.createVerticalSpacer(comp, 1);
		
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiUseScanPreferencePage_2, 2);

		Table table = new Table(comp, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridData gd = (GridData) table.getLayoutData();
		gd.widthHint = 350;
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.stateMask == SWT.NONE && e.keyCode == SWT.DEL) {
					removeLocation();
				}
			}
		});
		fTableViewer = new CheckboxTableViewer(table);
		fTableViewer.setLabelProvider(new TableColumnLabelProvider());
		fTableViewer.setContentProvider(new ArrayContentProvider());

		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		Button button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_3, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				select(true);
			}
		});
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_10, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				select(false);
			}
		});
		
		SWTFactory.createHorizontalSpacer(bcomp, 1);
		
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_4, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addDirectory(null);
			}
		});
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_5, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addArchive(null);
			}
		});
		
		editbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_1, null);
		editbutton.setEnabled(false);
		editbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		remove = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_6, null);
		remove.setEnabled(false);
		remove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeLocation();
			}
		});
		
		Group optiongrp = SWTFactory.createGroup(comp, PreferenceMessages.options, 2, 2, GridData.FILL_HORIZONTAL);
		Label lbl = SWTFactory.createLabel(optiongrp, PreferenceMessages.ApiUseScanPreferencePage_9, 1);
		gd = (GridData) lbl.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		fSpinner = new Spinner(optiongrp, SWT.BORDER);
		
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
				remove.setEnabled(!selection.isEmpty());
				editbutton.setEnabled(selection.size() == 1);
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		performInit(0, null);
		validateScans();
		Dialog.applyDialogFont(comp);
		return comp;
	}

	/**
	 * Selects (checks) all of the entries in the table
	 * @param checked
	 */
	void select(boolean checked) {
		fTableViewer.setAllChecked(checked);
		fTableViewer.refresh();
	}
	
	/**
	 * Adds a directory to a use scan to the table
	 * @param prevLocation
	 * @return if the location was actually added
			 */
	boolean addDirectory(String prevLocation) {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(PreferenceMessages.ApiUseScanPreferencePage_7);
		if (prevLocation != null) {
			dialog.setFilterPath(prevLocation);
		}
		String location = dialog.open();
		if (location == null || location.length() == 0)
			return false;
		if (prevLocation != null && !prevLocation.equalsIgnoreCase(location)) {
			setMessage(null);
		}
		addLocation(location);
		return true;
	}
	
	/**
	 * Adds an archive file to the table
	 * @param file
	 * @return if the archive was actually added
			 */
	boolean addArchive(File file) {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterNames(new String[] {"Archives (*.zip)", "Jars (*.jar)"}); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterExtensions(new String[] {"*.zip", "*.jar"}); //$NON-NLS-1$ //$NON-NLS-2$				 
		if (file != null) {
			dialog.setFilterPath(file.getParent());
			dialog.setFileName(file.getName());
		}
		String location = dialog.open();
		if (location == null || location.length() == 0) {
			return false;
		}
		addLocation(location);
		return true;
	}
	
	/**
	 * Adds the given location to the table
	 * @param location
	 */
	void addLocation(String location) {
		if (fLocationList.contains(location)) {
			return;
		}
		if (!isValidScanLocation(location)) {
			setErrorMessage(NLS.bind(PreferenceMessages.ApiUseScanPreferencePage_8, location));
		}
		fLocationList.add(location);
		fTableViewer.refresh();
		fTableViewer.setChecked(location, true);		
	}

	/**
	 * Allows you to edit the location of the scan
	 */
	void edit() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		String location = selection.getFirstElement().toString();			
		File file = new File(location);
		boolean editted = false;			
		if (file.isDirectory()) {
			editted = addDirectory(location);
		} else {
			editted = addArchive(file);
		}
		if (editted) {				
			fLocationList.remove(location);
			fTableViewer.refresh();
		}
	}
	
	/**
	 * Removes the selected locations
	 */
	void removeLocation() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		fLocationList.removeAll(selection.toList());
		fTableViewer.refresh();
		validateScans();
	}

	/**
	 * Validates that the scan are all still valid
	 */
	private void validateScans() {
		if (fLocationList.size() == 0) {
			return; //nothing to validate
		}
		String badLocation = null;
		for (Iterator iterator = fLocationList.iterator(); iterator.hasNext();) {
			badLocation = (String) iterator.next();
			if (!isValidScanLocation(badLocation)) {
				setErrorMessage(NLS.bind(PreferenceMessages.ApiUseScanPreferencePage_8, badLocation));
				setValid(false);
				return;
			}
		}
		setErrorMessage(null);
	}
	
	/**
	 * Returns if the scan if a valid API use scan
	 * @param location
	 * @return true if the scan is valid false otherwise
	 */
	public boolean isValidScanLocation(String location) {
		if (location != null && location.length() > 0) {
			IPath path = new Path(location);		
			File file = path.toFile();
			if (file.exists()) {
				if (Util.isArchive(file.getName())) {
					//TODO validate the archive contains a scan
					return true;
				}
				File root = null;
				if (path.lastSegment().equalsIgnoreCase(IApiCoreConstants.XML)) {
					root = path.toFile();
				}
				if(root == null) {
					path = path.append(IApiCoreConstants.XML);
					root = path.toFile();
					if(!root.exists()) {
						root = null;
					}
				}
				return root.isDirectory();
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		applyChanges();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		applyChanges();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		performInit(1000, ""); //$NON-NLS-1$
		applyChanges();
	}
	
	/**
	 * Initializes the page
	 * @param cacheSizeValue
	 * @param locationValue
	 */
	private void performInit(int cacheSizeValue, String locationValue) {
		int cacheSize = 0;

		try {
			cacheSize = cacheSizeValue != 0 ? cacheSizeValue : Integer.parseInt(getStoredValue(IApiCoreConstants.API_USE_SCAN_REFERENCE_CACHE_SIZE, String.valueOf(1000)));
		} catch (NumberFormatException e2) {
		}

		fSpinner.setValues(cacheSize, 0, Integer.MAX_VALUE, 0, 100, 1000);
		
		if (getContainer() == null) {
			fManager = new WorkingCopyManager();
		} else {
			fManager = ((IWorkbenchPreferenceContainer) getContainer()).getWorkingCopyManager();
		}

		fLocationList.clear();

		String location = locationValue != null ? locationValue :  getStoredValue(IApiCoreConstants.API_USE_SCAN_LOCATION, null);
		
		ArrayList checkedLocations = new ArrayList();
		if (location != null && location.length() > 0) {
			String[] locations = location.split(UseScanManager.ESCAPE_REGEX + UseScanManager.LOCATION_DELIM);
			for (int i = 0; i < locations.length; i++) {
				String values[] = locations[i].split(UseScanManager.ESCAPE_REGEX + UseScanManager.STATE_DELIM);
				fLocationList.add(values[0]);
				if (Boolean.valueOf(values[1]).booleanValue())
					checkedLocations.add(values[0]);
			}			
			fLocationList.remove(""); //$NON-NLS-1$
		}
		fTableViewer.setInput(fLocationList);
		fTableViewer.setCheckedElements(checkedLocations.toArray(new String[checkedLocations.size()]));
		fTableViewer.refresh();
		
		setErrorMessage(null);
	}

	/**
	 * Save changes to the preferences
	 */
	private void applyChanges() {
		StringBuffer locations = new StringBuffer();
		for (Iterator iterator = fLocationList.iterator(); iterator.hasNext();) {
			Object location = iterator.next();
			locations.append(location);
			locations.append(UseScanManager.STATE_DELIM);
			locations.append(fTableViewer.getChecked(location));
			locations.append(UseScanManager.LOCATION_DELIM);
		}
		
		if (hasLocationsChanges(locations.toString())) {
			IProject[] projects = Util.getApiProjects();
			// If there are API projects in the workspace, ask the user if they should be cleaned and built to run the new tooling
			if (projects != null){
				if(MessageDialog.openQuestion(getShell(), PreferenceMessages.ApiUseScanPreferencePage_11, PreferenceMessages.ApiUseScanPreferencePage_12)) {
					Util.getBuildJob(projects).schedule();
				}
			}
		}
		
		setStoredValue(IApiCoreConstants.API_USE_SCAN_LOCATION, locations.toString());
		setStoredValue(IApiCoreConstants.API_USE_SCAN_REFERENCE_CACHE_SIZE, String.valueOf(fSpinner.getSelection()));	
			
		try {
			fManager.applyChanges();
		} catch (BackingStoreException e) {
			ApiUIPlugin.log(e);
		}
	}

	/**
	 * Detects changes to the use scan locations
	 * @param newLocations
	 * @return if there have been changes to the use scan entries
	 */
	private boolean hasLocationsChanges(String newLocations) {
		String oldLocations = getStoredValue(IApiCoreConstants.API_USE_SCAN_LOCATION, null);
		
		if (oldLocations != null && oldLocations.equalsIgnoreCase(newLocations)) {
			return false;
		}
			
		ArrayList oldCheckedElements = new ArrayList();
		if (oldLocations != null && oldLocations.length() > 0) {
			String[] locations = oldLocations.split(UseScanManager.ESCAPE_REGEX + UseScanManager.LOCATION_DELIM);
			for (int i = 0; i < locations.length; i++) {
				String values[] = locations[i].split(UseScanManager.ESCAPE_REGEX + UseScanManager.STATE_DELIM);
				if (Boolean.valueOf(values[1]).booleanValue()) {
					oldCheckedElements.add(values[0]);
				}
			}			
		}
		Object[] newCheckedLocations = fTableViewer.getCheckedElements();
		if (newCheckedLocations.length != oldCheckedElements.size())
			return true;
		
		for (int i = 0; i < newCheckedLocations.length; i++) {
			if (!oldCheckedElements.contains(newCheckedLocations[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the value to the given preference key
	 * @param key
	 * @param value
	 */
	public void setStoredValue(String key, String value) {
		IEclipsePreferences node = getNode();
		if (value != null) {
			node.put(key, value);
		} else {
			node.remove(key);
		}
	}

	/**
	 * Retrieves the value for the given preference key or the returns the given default if it is not defined
	 * @param key
	 * @param defaultValue
	 * @return the stored value or the specified default 
	 */
	public String getStoredValue(String key, String defaultValue) {
		IEclipsePreferences node = getNode();
		if (node != null) {
			return node.get(key, defaultValue);
		}
		return defaultValue;
	}

	/**
	 * @return the root preference node for the API tools core plugin
	 */
	private IEclipsePreferences getNode() {
		IEclipsePreferences node = (new InstanceScope()).getNode(ApiPlugin.PLUGIN_ID);
		if (fManager != null) {
			return fManager.getWorkingCopy(node);
		}
		return node;
	}
}
