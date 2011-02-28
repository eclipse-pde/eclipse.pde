/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
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
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This block is used to add the API Use Scan problem severity settings UI to a
 * parent control
 */
public class ApiUseScanConfigurationBlock {
	/**
	 * Provides data information for created controls
	 */
	protected static class ControlData {
		Key key;
		private String[] values;

		/**
		 * Constructor
		 * 
		 * @param key
		 * @param values
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

		private String qualifier;
		private String key;

		/**
		 * Constructor
		 * 
		 * @param qualifier
		 * @param key
		 */
		public Key(String qualifier, String key) {
			this.qualifier = qualifier;
			this.key = key;
		}

		/**
		 * Returns the {@link IEclipsePreferences} node for the given context
		 * and {@link IWorkingCopyManager}
		 * 
		 * @param context
		 * @param manager
		 * @return the {@link IEclipsePreferences} node or <code>null</code>
		 */
		private IEclipsePreferences getNode(IScopeContext context,
				IWorkingCopyManager manager) {
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
		 * @param context
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or
		 *         <code>null</code>
		 */
		public String getStoredValue(IScopeContext context,
				IWorkingCopyManager manager) {
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
		 * @param lookupOrder
		 * @param ignoreTopScope
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or
		 *         <code>null</code>
		 */
		public String getStoredValue(IScopeContext[] lookupOrder,
				boolean ignoreTopScope, IWorkingCopyManager manager) {
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
		 * 
		 * @param context
		 * @param value
		 * @param manager
		 */
		public void setStoredValue(IScopeContext context, String value,
				IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if (value != null) {
				node.put(key, value);
			} else {
				node.remove(key);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return qualifier + '/' + key;
		}
	}

	/**
	 * Returns a new {@link Key} for the {@link ApiUIPlugin} preference store
	 * 
	 * @param key
	 * @return the new {@link Key} for the {@link ApiUIPlugin} preference store
	 */
	protected final static Key getApiToolsKey(String key) {
		return new Key(ApiPlugin.PLUGIN_ID, key);
	}

	private static final Key KEY_API_USE_SCAN_TYPE_PROBLEM = getApiToolsKey(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY);
	private static final Key KEY_API_USE_SCAN_METHOD_PROBLEM = getApiToolsKey(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY);
	private static final Key KEY_API_USE_SCAN_FIELD_PROBLEM = getApiToolsKey(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY);

	/**
	 * An array of all of the keys for the page
	 */
	private static Key[] fgAllKeys = { KEY_API_USE_SCAN_TYPE_PROBLEM,
			KEY_API_USE_SCAN_METHOD_PROBLEM, KEY_API_USE_SCAN_FIELD_PROBLEM };

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
	private static final String[] SEVERITIES = { ApiPlugin.VALUE_ERROR,
			ApiPlugin.VALUE_WARNING, ApiPlugin.VALUE_IGNORE, };

	/**
	 * Default selection listener for controls on the page
	 */
	private SelectionListener selectionlistener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			if (e.widget instanceof Combo) {
				Combo combo = (Combo) e.widget;
				ControlData data = (ControlData) combo.getData();
				data.key.setStoredValue(fLookupOrder[0], combo.getText(),
						fManager);
				ApiBaselinePreferencePage.rebuildcount = 0;
			}
		}
	};

	/**
	 * Listing of all of the {@link Combo}s added to the block
	 */
	private Combo[] fCombo = null;

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
	 * Stored old fProject specific settings.
	 */
	// TODO will be used when we turn it into project specific settings
	private IdentityHashMap fOldProjectSettings = null;

	/**
	 * Constructor
	 * 
	 * @param project
	 */
	public ApiUseScanConfigurationBlock(IWorkingCopyManager manager) {
		fLookupOrder = new IScopeContext[] { InstanceScope.INSTANCE,
				DefaultScope.INSTANCE };
		fManager = manager;
		fOldProjectSettings = null;
	}

	/**
	 * Creates the control in the parent control
	 * 
	 * @param parent
	 *            the parent control
	 */
	public Control createControl(Composite parent) {
		fMainComp = SWTFactory.createComposite(parent, 1, 1,
				GridData.FILL_HORIZONTAL, 0, 0);
		Group optionsProfileGroup = SWTFactory.createGroup(fMainComp,
				PreferenceMessages.ApiUseScanConfigurationBlock_0, 2, 1,
				GridData.FILL_BOTH);
		fCombo = new Combo[3];
		fCombo[0] = createComboControl(optionsProfileGroup,
				PreferenceMessages.ApiUseScanConfigurationBlock_unresolvedTypeProblem,
				KEY_API_USE_SCAN_TYPE_PROBLEM);
		fCombo[1] = createComboControl(optionsProfileGroup,
				PreferenceMessages.ApiUseScanConfigurationBlock_unresolvedMethodProblem,
				KEY_API_USE_SCAN_METHOD_PROBLEM);
		fCombo[2] = createComboControl(optionsProfileGroup,
				PreferenceMessages.ApiUseScanConfigurationBlock_unresolvedFieldProblem,
				KEY_API_USE_SCAN_FIELD_PROBLEM);
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
		try {
			ArrayList changes = new ArrayList();
			collectChanges(fLookupOrder[0], changes);
			if (changes.size() > 0) {
				fManager.applyChanges();
			}

		} catch (BackingStoreException bse) {
			ApiPlugin.log(bse);
		}
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
		for (int i = 0; i < fgAllKeys.length; i++) {
			defval = fgAllKeys[i].getStoredValue(fLookupOrder, true, fManager);
			fgAllKeys[i].setStoredValue(fLookupOrder[0], defval, fManager);
		}
		updateCombos();
	}

	/**
	 * Updates all of the registered {@link Combo}s on the page. Registration
	 * implies that the {@link Combo} control was added to the listing of
	 * fCombos
	 */
	private void updateCombos() {
		if (fCombo != null) {
			for (int i = 0; i < fCombo.length; i++) {
				ControlData data = (ControlData) fCombo[i].getData();
				fCombo[i].select(data.getSelection(data.getKey()
						.getStoredValue(fLookupOrder, false, fManager)));
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
	 * Creates a {@link Label} | {@link Combo} control. The combo is initialized
	 * from the given {@link Key}
	 * 
	 * @param parent
	 * @param label
	 * @param key
	 */
	protected Combo createComboControl(Composite parent, String label, Key key) {
		Label lbl = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.BEGINNING, GridData.CENTER, true,
				false);
		lbl.setLayoutData(gd);
		lbl.setText(label);
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		gd = new GridData(GridData.END, GridData.CENTER, false, false);
		combo.setLayoutData(gd);
		ControlData data = new ControlData(key, SEVERITIES);
		combo.setData(data);
		combo.setItems(SEVERITIES_LABELS);
		combo.addSelectionListener(selectionlistener);
		combo.select(data.getSelection(key.getStoredValue(fLookupOrder, false,
				fManager)));
		return combo;
	}

	/**
	 * Collects the keys that have changed on the page into the specified list
	 * 
	 * @param changes
	 *            the {@link List} to collect changed keys into
	 */
	private void collectChanges(IScopeContext context, List changes) {
		Key key = null;
		String origval = null, newval = null;
		boolean complete = fOldProjectSettings == null;
		for (int i = 0; i < fgAllKeys.length; i++) {
			key = fgAllKeys[i];
			origval = key.getStoredValue(context, null);
			newval = key.getStoredValue(context, fManager);
			if (newval == null) {
				if (origval != null) {
					changes.add(key);
				} else if (complete) {
					key.setStoredValue(context,
							key.getStoredValue(fLookupOrder, true, fManager),
							fManager);
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

	public void setEnabled(boolean enabled) {
		fMainComp.setEnabled(enabled);
		for (int i = 0; i < fCombo.length; i++) {
			fCombo[i].setEnabled(enabled);
		}

	}
}
