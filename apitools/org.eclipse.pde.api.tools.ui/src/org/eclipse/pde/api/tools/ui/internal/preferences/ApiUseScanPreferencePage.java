/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.search.UseScanManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Preference page to allow users to add use scans. The use scans are analyzed
 * in the API Tools builder to see if any methods found in the scan have been
 * removed.
 *
 * @since 3.7
 */
public class ApiUseScanPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.eclipse.pde.api.tools.ui.apiusescan.prefpage"; //$NON-NLS-1$

	private IWorkingCopyManager fManager;
	private CheckboxTableViewer fTableViewer;
	private final Set<String> fLocationList = new HashSet<>();

	/**
	 * Column provider for the use scan table
	 */
	class TableColumnLabelProvider extends ColumnLabelProvider {

		Image archive = null;

		@Override
		public void dispose() {
			if (archive != null) {
				archive.dispose();
			}
			super.dispose();
		}

		@Override
		public Image getImage(Object element) {
			File file = new File(element.toString());
			if (file.isDirectory()) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER);
			}
			if (archive == null) {
				ImageDescriptor image = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(file.getName());
				archive = image.createImage();
			}
			return archive;
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiUseScanPreferencePage_0, 2, 250);
		SWTFactory.createVerticalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiUseScanPreferencePage_2, 2);

		Table table = new Table(comp, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridData gd = (GridData) table.getLayoutData();
		gd.widthHint = 250;
		table.addKeyListener(KeyListener.keyReleasedAdapter(e -> {
			if (e.stateMask == SWT.NONE && e.keyCode == SWT.DEL) {
				removeLocation();
			}
		}));
		fTableViewer = new CheckboxTableViewer(table);
		fTableViewer.setLabelProvider(new TableColumnLabelProvider());
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());

		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		Button button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_3, null);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> select(true)));
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_10, null);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> select(false)));

		SWTFactory.createHorizontalSpacer(bcomp, 1);

		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_4, null);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			String loc = getDirectory(null);
			if (loc != null) {
				String exactLocation = UseScanManager.getExactScanLocation(loc);
				if (exactLocation == null) {
					addLocation(loc);
				} else {
					addLocation(exactLocation);
				}
			}
		}));
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_5, null);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			String loc = getArchive(null);
			if (loc != null) {
				addLocation(loc);
			}
		}));

		Button editbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_1, null);
		editbutton.setEnabled(false);
		editbutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> edit()));
		Button remove = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiUseScanPreferencePage_6, null);
		remove.setEnabled(false);
		remove.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeLocation()));

		fTableViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = fTableViewer.getStructuredSelection();
			remove.setEnabled(!selection.isEmpty());
			editbutton.setEnabled(selection.size() == 1);
		});
		fTableViewer.addDoubleClickListener(event -> edit());

		Map<String, Object> linkdata = new HashMap<>();
		linkdata.put(ApiErrorsWarningsPreferencePage.INITIAL_TAB, Integer.valueOf(ApiErrorsWarningsConfigurationBlock.API_USE_SCANS_PAGE_ID));
		PreferenceLinkArea apiErrorLinkArea = new PreferenceLinkArea(comp, SWT.NONE, ApiErrorsWarningsPreferencePage.ID, PreferenceMessages.ApiUseScanPreferencePage_9, (IWorkbenchPreferenceContainer) getContainer(), linkdata);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.widthHint = 250;
		data.horizontalSpan = 2;
		apiErrorLinkArea.getControl().setLayoutData(data);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APIUSESCANS_PREF_PAGE);
		performInit();
		validateScans();
		Dialog.applyDialogFont(comp);
		return comp;
	}

	/**
	 * Selects (checks) all of the entries in the table
	 */
	void select(boolean checked) {
		fTableViewer.setAllChecked(checked);
		fTableViewer.refresh();
	}

	/**
	 * Allows users to select a directory with a use scan in it
	 *
	 * @return the new directory or <code>null</code> if the dialog was
	 *         cancelled
	 */
	String getDirectory(String prevLocation) {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(PreferenceMessages.ApiUseScanPreferencePage_7);
		if (prevLocation != null) {
			dialog.setFilterPath(prevLocation);
		}
		return dialog.open();
	}

	/**
	 * Allows the user to select an archive from the file system
	 *
	 * @param file a starting file
	 * @return the path to the new archive or <code>null</code> if cancelled
	 */
	String getArchive(File file) {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterNames(new String[] { PreferenceMessages.archives__zip });
		dialog.setFilterExtensions(new String[] { "*.zip;*.jar" }); //$NON-NLS-1$
		if (file != null) {
			dialog.setFilterPath(file.getParent());
			dialog.setFileName(file.getName());
		}
		return dialog.open();
	}

	/**
	 * Adds the given location to the table
	 */
	void addLocation(String location) {
		fLocationList.add(location);
		fTableViewer.refresh();
		fTableViewer.setChecked(location, true);
		fTableViewer.setSelection(new StructuredSelection(location));
		// do the whole pass in case you have more than one invalid location
		validateScans();
	}

	/**
	 * Allows you to edit the location of the scan
	 */
	void edit() {
		IStructuredSelection selection = fTableViewer.getStructuredSelection();
		String location = selection.getFirstElement().toString();
		File file = new File(location);
		String newloc = null;
		if (file.isDirectory()) {
			newloc = getDirectory(location);
		} else {
			newloc = getArchive(file);
		}
		if (newloc != null) {
			fLocationList.remove(location);
			addLocation(newloc);
		}
	}

	/**
	 * Removes the selected locations
	 */
	void removeLocation() {
		IStructuredSelection selection = fTableViewer.getStructuredSelection();
		fLocationList.removeAll(selection.toList());
		fTableViewer.refresh();
		validateScans();
	}

	/**
	 * Validates that the scan are all still valid
	 */
	private void validateScans() {
		for (String loc : fLocationList) {
			if (!UseScanManager.isValidScanLocation(loc)) {
				setErrorMessage(NLS.bind(PreferenceMessages.ApiUseScanPreferencePage_8, loc));
				setValid(false);
				return;
			}
		}
		setValid(true);
		setErrorMessage(null);
	}

	@Override
	public boolean performOk() {
		applyChanges();
		return true;
	}

	@Override
	protected void performApply() {
		applyChanges();
	}

	@Override
	protected void performDefaults() {
		fLocationList.clear();
		fTableViewer.refresh();
		setValid(true);
		setErrorMessage(null);
		super.performDefaults();
	}

	/**
	 * Initializes the page
	 */
	private void performInit() {
		if (getContainer() == null) {
			fManager = new WorkingCopyManager();
		} else {
			fManager = ((IWorkbenchPreferenceContainer) getContainer()).getWorkingCopyManager();
		}

		fLocationList.clear();

		String location = getStoredValue(IApiCoreConstants.API_USE_SCAN_LOCATION, null);

		List<String> checkedLocations = new ArrayList<>();
		if (location != null && location.length() > 0) {
			String[] locations = location.split(UseScanManager.ESCAPE_REGEX + UseScanManager.LOCATION_DELIM);
			for (String locationString : locations) {
				String[] values = locationString.split(UseScanManager.ESCAPE_REGEX + UseScanManager.STATE_DELIM);
				fLocationList.add(values[0]);
				if (Boolean.parseBoolean(values[1])) {
					checkedLocations.add(values[0]);
				}
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
		StringBuilder locations = new StringBuilder();
		for (String string : fLocationList) {
			Object location = string;
			locations.append(location);
			locations.append(UseScanManager.STATE_DELIM);
			locations.append(fTableViewer.getChecked(location));
			locations.append(UseScanManager.LOCATION_DELIM);
		}

		Object[] newCheckedLocations = fTableViewer.getCheckedElements();
		boolean hasLocationChanges = hasLocationsChanges(locations.toString());
		if (hasLocationChanges && newCheckedLocations.length != 0) {
			IProject[] projects = Util.getApiProjects();
			// If there are API projects in the workspace, ask the user if they
			// should be cleaned and built to run the new tooling
			if (projects != null && MessageDialog.openQuestion(getShell(),
					PreferenceMessages.ApiUseScanPreferencePage_11, PreferenceMessages.ApiUseScanPreferencePage_12)) {
				Util.getBuildJob(projects).schedule();
			}
		}
		if (hasLocationChanges && newCheckedLocations.length == 0) {
			findAndDeleteAPIUseScanMarkers();
		}

		setStoredValue(IApiCoreConstants.API_USE_SCAN_LOCATION, locations.toString());

		try {
			fManager.applyChanges();
		} catch (BackingStoreException e) {
			ApiUIPlugin.log(e);
		}
	}

	private void findAndDeleteAPIUseScanMarkers() {
		IProject[] apiProjects = Util.getApiProjects();
		if (apiProjects == null) {
			return;
		}
		for (IProject iProject : apiProjects) {
			try {
				iProject.deleteMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false,
						IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
			}
		}
	}

	/**
	 * Detects changes to the use scan locations
	 *
	 * @return if there have been changes to the use scan entries
	 */
	private boolean hasLocationsChanges(String newLocations) {
		String oldLocations = getStoredValue(IApiCoreConstants.API_USE_SCAN_LOCATION, null);

		if (oldLocations != null && oldLocations.equalsIgnoreCase(newLocations)) {
			return false;
		}

		List<String> oldCheckedElements = new ArrayList<>();
		if (oldLocations != null && oldLocations.length() > 0) {
			String[] locations = oldLocations.split(UseScanManager.ESCAPE_REGEX + UseScanManager.LOCATION_DELIM);
			for (String location : locations) {
				String[] values = location.split(UseScanManager.ESCAPE_REGEX + UseScanManager.STATE_DELIM);
				if (Boolean.parseBoolean(values[1])) {
					oldCheckedElements.add(values[0]);
				}
			}
		}
		Object[] newCheckedLocations = fTableViewer.getCheckedElements();
		if (newCheckedLocations.length != oldCheckedElements.size()) {
			return true;
		}
		for (Object newCheckedLocation : newCheckedLocations) {
			if (!oldCheckedElements.contains(newCheckedLocation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the value to the given preference key
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
	 * Retrieves the value for the given preference key or the returns the given
	 * default if it is not defined
	 *
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
	 * @return the root preference node for the API tools core plug-in
	 */
	private IEclipsePreferences getNode() {
		IEclipsePreferences node = (InstanceScope.INSTANCE).getNode(ApiPlugin.PLUGIN_ID);
		if (fManager != null) {
			return fManager.getWorkingCopy(node);
		}
		return node;
	}
}
