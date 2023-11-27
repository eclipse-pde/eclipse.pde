/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.internal.ui.preferences.ConfigurationBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This block is used to add the API Tools profile notification settings UI to a
 * parent control
 *
 * @since 1.0.0
 */
public class ApiBaselinesConfigurationBlock extends ConfigurationBlock {
	/**
	 * Provides data information for created controls
	 */
	protected static class ControlData {
		Key key;
		private final String[] values;

		/**
		 * Constructor
		 */
		public ControlData(Key key, String[] values) {
			this.key = key;
			this.values = values;
		}

		public Key getKey() {
			return key;
		}

		public String getValue(boolean selection) {
			int index = selection ? 0 : 1;
			return values[index];
		}

		public String getValue(int index) {
			return values[index];
		}

		public int getSelection(String value) {
			if (value != null) {
				for (int i = 0; i < values.length; i++) {
					if (value.equals(values[i])) {
						return i;
					}
				}
			}
			return values.length - 1; // assume the last option is the least
										// severe
		}
	}

	/**
	 * Provides management for changed/stored values for a given preference key
	 */
	protected static class Key {

		private final String qualifier;
		private final String key;

		/**
		 * Constructor
		 */
		public Key(String qualifier, String key) {
			this.qualifier = qualifier;
			this.key = key;
		}

		/**
		 * Returns the {@link IEclipsePreferences} node for the given context
		 * and {@link IWorkingCopyManager}
		 *
		 * @return the {@link IEclipsePreferences} node or <code>null</code>
		 */
		private IEclipsePreferences getNode(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = context.getNode(qualifier);
			if (manager != null) {
				return manager.getWorkingCopy(node);
			}
			return node;
		}

		/**
		 * Returns the value stored in the {@link IEclipsePreferences} node from
		 * the given context and working copy manager
		 *
		 * @return the value from the {@link IEclipsePreferences} node or
		 *         <code>null</code>
		 */
		public String getStoredValue(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if (node != null) {
				return node.get(key, null);
			}
			return null;
		}

		/**
		 * Returns the stored value of this {@link IEclipsePreferences} node
		 * using a given lookup order, and allowing the top scope to be ignored
		 *
		 * @return the value from the {@link IEclipsePreferences} node or
		 *         <code>null</code>
		 */
		public String getStoredValue(IScopeContext[] lookupOrder, boolean ignoreTopScope, IWorkingCopyManager manager) {
			for (int i = ignoreTopScope ? 1 : 0; i < lookupOrder.length; i++) {
				String value = getStoredValue(lookupOrder[i], manager);
				if (value != null) {
					return value;
				}
			}
			return null;
		}

		/**
		 * Sets the value of this key
		 */
		public void setStoredValue(IScopeContext context, String value, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if (value != null) {
				node.put(key, value);
			} else {
				node.remove(key);
			}
		}

		@Override
		public String toString() {
			return qualifier + '/' + key;
		}
	}

	/**
	 * Returns a new {@link Key} for the {@link ApiUIPlugin} preference store
	 *
	 * @return the new {@link Key} for the {@link ApiUIPlugin} preference store
	 */
	protected final static Key getApiToolsKey(String key) {
		return new Key(ApiPlugin.PLUGIN_ID, key);
	}

	private static final Key KEY_MISSING_DEFAULT_API_PROFILE = getApiToolsKey(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE);

	private static final Key KEY_PLUGIN_MISSING_IN_BASELINE = getApiToolsKey(IApiProblemTypes.MISSING_PLUGIN_IN_API_BASELINE);
	/**
	 * An array of all of the keys for the page
	 */
	private static Key[] fgAllKeys = { KEY_MISSING_DEFAULT_API_PROFILE, KEY_PLUGIN_MISSING_IN_BASELINE };

	/**
	 * Constant representing the severity values presented in the combo boxes
	 * for each option
	 */
	private static final String[] SEVERITIES_LABELS = {
			PreferenceMessages.ApiErrorsWarningsConfigurationBlock_error,
			PreferenceMessages.ApiErrorsWarningsConfigurationBlock_warning,
			PreferenceMessages.ApiErrorsWarningsConfigurationBlock_ignore };

	/**
	 * Constant representing the severity values presented in the combo boxes
	 * for each option
	 */
	private static final String[] SEVERITIES = {
			ApiPlugin.VALUE_ERROR, ApiPlugin.VALUE_WARNING,
			ApiPlugin.VALUE_IGNORE, };

	/**
	 * Default selection listener for controls on the page
	 */
	private final SelectionListener selectionlistener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget instanceof Combo) {
				Combo combo = (Combo) e.widget;
				ControlData data = (ControlData) combo.getData();
				data.key.setStoredValue(fLookupOrder[0], combo.getText(), fManager);
				fDirty = true;
				ApiBaselinePreferencePage.rebuildcount = 0;
			}
		}
	};

	/**
	 * Listing of all of the {@link Combo}s added to the block
	 */

	private final ArrayList<Combo> fCombos = new ArrayList<>();
	/**
	 * Listing of the label in the block
	 */
	private final ArrayList<Label> fLabels = new ArrayList<>();

	/**
	 * The context of settings locations to search for values in
	 */
	IScopeContext[] fLookupOrder = null;

	/**
	 * the working copy manager to work with settings
	 */
	IWorkingCopyManager fManager = null;

	/**
	 * The main composite for the configuration block, used for
	 * enabling/disabling the block
	 */
	private Composite fMainComp = null;

	/**
	 * Flag used to know if the page needs saving or not
	 */
	boolean fDirty = false;

	/**
	 * The parent this block has been added to
	 */
	private Composite fParent = null;

	private boolean hasBaseline = true;
	/**
	 * Constructor
	 */
	public ApiBaselinesConfigurationBlock(IWorkbenchPreferenceContainer container) {
		fLookupOrder = new IScopeContext[] {
				InstanceScope.INSTANCE, DefaultScope.INSTANCE };
		if (container == null) {
			fManager = new WorkingCopyManager();
		} else {
			fManager = container.getWorkingCopyManager();
		}
	}

	/**
	 * Creates the control in the parent control
	 *
	 * @param parent the parent control
	 */
	public Control createControl(Composite parent, final ApiBaselinePreferencePage page) {
		fParent = parent;
		fMainComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);
		Group optionsProfileGroup = SWTFactory.createGroup(fMainComp, PreferenceMessages.ApiProfilesConfigurationBlock_options_group_title, 2, 1, GridData.FILL_BOTH);
		Combo combo = createComboControl(optionsProfileGroup,
				PreferenceMessages.ApiProfilesConfigurationBlock_missing_default_api_profile_message,
				KEY_MISSING_DEFAULT_API_PROFILE);
		fCombos.add(combo);
		combo = createComboControl(optionsProfileGroup,
				PreferenceMessages.ApiProfilesConfigurationBlock_plugin_missing_in_baseline_message,
				KEY_PLUGIN_MISSING_IN_BASELINE);
		fCombos.add(combo);
		Dialog.applyDialogFont(fMainComp);
		return fMainComp;
	}

	/**
	 * Saves all of the changes on the page
	 */
	public void performOK() {
		save();
	}

	/**
	 * Directly applies all of the changes on the page
	 */
	public void performApply() {
		save();
	}

	/**
	 * Performs the save operation on the working copy manager
	 */
	private void save() {
		if (fDirty) {
			try {
				ArrayList<Key> changes = new ArrayList<>();
				collectChanges(fLookupOrder[0], changes);
				filterOutChanges(changes);
				if (changes.size() == 1 && changes.get(0).equals(KEY_MISSING_DEFAULT_API_PROFILE)) {
					Key k = changes.get(0);

					String original = k.getStoredValue(fLookupOrder[0], null);
					if (original == null) {
						// this means default value - always error
						original = ApiPlugin.VALUE_ERROR;
					}
					String newval = k.getStoredValue(fLookupOrder[0], fManager);
					if (newval.equals(ApiPlugin.VALUE_IGNORE)) {
						// just check for any missing baseline and delete it
						deleteMissingBaselineMarker();

					} else if (newval.equals(ApiPlugin.VALUE_WARNING)) {
						// if error to warning - change attribute
						if (original.equals(ApiPlugin.VALUE_ERROR)) {
							updateMissingBaselineMarkerSeverity(IMarker.SEVERITY_WARNING);
						}
						// if ignore to warning - calculate
						if (original.equals(ApiPlugin.VALUE_IGNORE)) {
							if (hasBaseline == false) {
								createMissingBaselineMarker(IMarker.SEVERITY_WARNING);
							}
						}

					} else if (newval.equals(ApiPlugin.VALUE_ERROR)) {
						// if warning to error - change attribute
						if (original.equals(ApiPlugin.VALUE_WARNING)) {
							updateMissingBaselineMarkerSeverity(IMarker.SEVERITY_ERROR);
						}
						// if ignore to warning - calculate
						if (original.equals(ApiPlugin.VALUE_IGNORE)) {
							if (hasBaseline == false) {
								createMissingBaselineMarker(IMarker.SEVERITY_ERROR);
							}
						}
					}
					fDirty = false;
					return;
				}
				// code below probably redundant now
				if (changes.size() > 0) {
					if (ApiBaselinePreferencePage.rebuildcount < 1) {
						ApiBaselinePreferencePage.rebuildcount++;
						fManager.applyChanges();
						String message = PreferenceMessages.ApiErrorsWarningsConfigurationBlock_0;
						IProject[] apiProjects = Util.getApiProjects();
						if (apiProjects != null) {
							// do not even ask if there are no projects to build
							int userInput = MessageDialog.open(MessageDialog.QUESTION, fParent.getShell(),
									PreferenceMessages.ApiErrorsWarningsConfigurationBlock_2, message, SWT.NONE,
									PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_buildButtonLabel,
									PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_dontBuildButtonLabel);
							if (Window.OK == userInput) {
								Util.getBuildJob(apiProjects).schedule();
							}
						}
					}
				}
				fDirty = false;
			} catch (BackingStoreException bse) {
				ApiPlugin.log(bse);
			}
		}
	}

	// Filter out redundant change
	private void filterOutChanges(ArrayList<Key> changes) {
		if (changes.size() == 2) {
			Key k1 = changes.get(0);
			String original1 = k1.getStoredValue(fLookupOrder[0], null);
			String newval1 = k1.getStoredValue(fLookupOrder[0], fManager);
			if (original1 == null && newval1 == null) {
				changes.remove(0);
			}
			if (original1 != null && newval1 != null) {
				if (original1.equals(newval1)) {
					changes.remove(0);
				}
			}
			if (changes.size() == 2) {
				Key k2 = changes.get(1);
				String original2 = k2.getStoredValue(fLookupOrder[0], null);
				String newval2 = k2.getStoredValue(fLookupOrder[0], fManager);
				if (original2 == null && newval2 == null) {
					changes.remove(1);
				}
				if (original2 != null && newval2 != null) {
					if (original2.equals(newval2)) {
						changes.remove(1);
					}
				}
			}

		}
	}

	private void updateMissingBaselineMarkerSeverity(int severity) {
		ArrayList<IMarker> marker = findMissingBaselineMarker();

		for (IMarker iMarker : marker) {
			try {
				iMarker.setAttribute(IMarker.SEVERITY, severity);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	private void deleteMissingBaselineMarker() {
		ArrayList<IMarker> marker = findMissingBaselineMarker();
		for (IMarker iMarker : marker) {
			try {
				iMarker.delete();
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	public void createMissingBaselineMarker() {
		if (hasBaseline) {
			return;
		}
		String newval = fgAllKeys[0].getStoredValue(fLookupOrder[0], fManager);
		if (newval == null) {
			newval = ApiPlugin.VALUE_ERROR;
		}
		int valueWarning = -1;
		if (newval.equals(ApiPlugin.VALUE_WARNING)) {
			valueWarning = IMarker.SEVERITY_WARNING;
		}
		if (newval.equals(ApiPlugin.VALUE_ERROR)) {
			valueWarning = IMarker.SEVERITY_ERROR;
		}
		if(valueWarning < 0) {
			return;
		}
		IProject[] apiProjects = Util.getApiProjects();
		if (apiProjects == null) {
			return;
		}
		removeBaselineMismatchMarker();
		for (IProject iProject : apiProjects) {
			createMissingBaselineMarkerOnProject(iProject, valueWarning);
		}
	}

	private void removeBaselineMismatchMarker() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			IMarker[] findMarkers = root.findMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, false,
					IResource.DEPTH_ZERO);
			for (IMarker iMarker : findMarkers) {
				iMarker.delete();
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}

	}

	private void createMissingBaselineMarker(int valueWarning) {
		IProject[] apiProjects = Util.getApiProjects();
		if (apiProjects == null) {
			return;
		}
		for (IProject iProject : apiProjects) {
			createMissingBaselineMarkerOnProject(iProject, valueWarning);
		}
	}

	private void createMissingBaselineMarkerOnProject(IResource res, int valueWarning) {
		try {
			IMarker createMarker = res.createMarker(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER);
			createMarker.setAttribute(IMarker.MESSAGE, PreferenceMessages.ApiBaselinesConfigurationBlock_0);
			createMarker.setAttribute(IMarker.SEVERITY, valueWarning);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	private ArrayList<IMarker> findMissingBaselineMarker() {
		ArrayList<IMarker> markList = new ArrayList<>();
		int missing_plugin = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_API_BASELINE,
				IElementDescriptor.RESOURCE, IApiProblem.API_PLUGIN_NOT_PRESENT_IN_BASELINE, IApiProblem.NO_FLAGS);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			IMarker[] findMarkers = root.findMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, false,
					IResource.DEPTH_ZERO);
			for (IMarker iMarker : findMarkers) {
				markList.add(iMarker);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		IProject[] apiProjects = Util.getApiProjects();
		if (apiProjects == null) {
			return markList;
		}
		for (IProject iProject : apiProjects) {
			IMarker[] findMarkers;
			try {
				findMarkers = iProject.findMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, false,
						IResource.DEPTH_ZERO);
				for (IMarker iMarker : findMarkers) {
					int id = ApiProblemFactory.getProblemId(iMarker);
					if (id == missing_plugin) {
						continue;
					}
					markList.add(iMarker);
				}
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		return markList;
	}

	/**
	 * Cancels all of the changes on the page
	 */
	public void performCancel() {
	}

	/**
	 * Reverts all of the settings back to their defaults
	 */
	public void performDefaults() {
		String defval = null;
		for (Key key : fgAllKeys) {
			defval = key.getStoredValue(fLookupOrder, true, fManager);
			key.setStoredValue(fLookupOrder[0], defval, fManager);
		}
		updateCombos();
		fDirty = true;
	}

	/**
	 * Updates all of the registered {@link Combo}s on the page. Registration
	 * implies that the {@link Combo} control was added to the listing of
	 * fCombos
	 */
	private void updateCombos() {
		for (Combo combo : fCombos) {
			if (combo != null) {
				ControlData data = (ControlData) combo.getData();
				combo.select(data.getSelection(data.getKey().getStoredValue(fLookupOrder, false, fManager)));
			}

		}
	}

	/**
	 * Disposes the controls from this page
	 */
	public void dispose() {
		fMainComp.getParent().dispose();
	}

	/**
	 * Creates a {@link Label} | {@link Combo} control. The combo is initialised
	 * from the given {@link Key}
	 */
	protected Combo createComboControl(Composite parent, String label, Key key) {
		Label lbl = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
		lbl.setLayoutData(gd);
		lbl.setText(label);
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		gd = new GridData(GridData.END, GridData.CENTER, false, false);
		combo.setLayoutData(gd);
		ControlData data = new ControlData(key, SEVERITIES);
		combo.setData(data);
		combo.setItems(SEVERITIES_LABELS);
		combo.addSelectionListener(selectionlistener);
		combo.select(data.getSelection(key.getStoredValue(fLookupOrder, false, fManager)));
		addHighlight(parent, lbl, combo);
		fLabels.add(lbl);
		return combo;
	}

	/**
	 * Collects the keys that have changed on the page into the specified list
	 *
	 * @param changes the {@link List} to collect changed keys into
	 */
	private void collectChanges(IScopeContext context, List<Key> changes) {
		String origval = null, newval = null;
		boolean complete = true;
		for (Key key : fgAllKeys) {
			origval = key.getStoredValue(context, null);
			newval = key.getStoredValue(context, fManager);
			if (newval == null) {
				if (origval != null) {
					changes.add(key);
				} else if (complete) {
					key.setStoredValue(context, key.getStoredValue(fLookupOrder, true, fManager), fManager);
					changes.add(key);
				}
			} else if (!newval.equals(origval)) {
				changes.add(key);
			}
		}
	}

	public static Key[] getAllKeys() {
		return fgAllKeys;
	}

	public void selectOption(int index) {
		if (fCombos.get(index) != null && !fCombos.get(index).isDisposed()) {
			fCombos.get(index).setFocus();
			if (fLabels.get(index) != null && !(fLabels.get(index).isDisposed())) {
				if (org.eclipse.jface.util.Util.isMac()) {
					if (fLabels.get(index) != null) {
						highlight(fCombos.get(index).getParent(), fLabels.get(index), fCombos.get(index),
								ConfigurationBlock.HIGHLIGHT_FOCUS);
					}
				}
			}
		}
	}

	public void setHasBaseline(boolean b) {
		hasBaseline = b;

	}
}
