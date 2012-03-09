/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - on going enhancements and maintenance
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import com.ibm.icu.text.MessageFormat;
import java.util.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A configuration block for setting PDE compiler preferences
 * 
 * @since 3.4
 */
public class PDECompilersConfigurationBlock extends ConfigurationBlock {

	/**
	 * Provides data information for created controls
	 */
	protected static class ControlData {
		private Key key;
		private String[] values;

		/**
		 * Constructor
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
			return values.length - 1; // assume the last option is the least severe
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
		 * @param qualifier
		 * @param key
		 */
		public Key(String qualifier, String key) {
			this.qualifier = qualifier;
			this.key = key;
		}

		/**
		 * Returns the {@link IEclipsePreferences} node for the given context and {@link IWorkingCopyManager}
		 * @param context
		 * @param manager
		 * @return the {@link IEclipsePreferences} node or <code>null</code>
		 */
		private IEclipsePreferences getNode(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = context.getNode(this.qualifier);
			if (manager != null) {
				return manager.getWorkingCopy(node);
			}
			return node;
		}

		/**
		 * Returns the value stored in the {@link IEclipsePreferences} node from the given context and working copy manager
		 * @param context
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or <code>null</code>
		 */
		public String getStoredValue(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if (node != null) {
				return node.get(this.key, null);
			}
			return null;
		}

		/**
		 * Returns the stored value of this {@link IEclipsePreferences} node using a given lookup order, and allowing the
		 * top scope to be ignored
		 * @param lookupOrder
		 * @param ignoreTopScope
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or <code>null</code>
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
		 * @param context
		 * @param value
		 * @param manager
		 */
		public void setStoredValue(IScopeContext context, String value, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if (value != null) {
				node.put(this.key, value);
			} else {
				node.remove(this.key);
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return qualifier + '/' + this.key;
		}
	}

	/**
	 * Returns a new {@link Key} for the {@link PDEPlugin} preference store
	 * @param key
	 * @return the new {@link Key} for the {@link PDEPlugin} preference store
	 */
	protected final static Key getPDEPrefKey(String key) {
		return new Key(PDE.PLUGIN_ID, key);
	}

	//unresolved
	private static final Key KEY_P_UNRESOLVED_IMPORTS = getPDEPrefKey(CompilerFlags.P_UNRESOLVED_IMPORTS);
	private static final Key KEY_P_UNRESOLVED_EX_POINTS = getPDEPrefKey(CompilerFlags.P_UNRESOLVED_EX_POINTS);
	private static final Key KEY_F_UNRESOLVED_PLUGINS = getPDEPrefKey(CompilerFlags.F_UNRESOLVED_PLUGINS);
	private static final Key KEY_F_UNRESOLVED_FEATURES = getPDEPrefKey(CompilerFlags.F_UNRESOLVED_FEATURES);

	//unknown elements
	private static final Key KEY_P_UNKNOWN_ELEMENT = getPDEPrefKey(CompilerFlags.P_UNKNOWN_ELEMENT);
	private static final Key KEY_P_UNKNOWN_ATTRIBUTE = getPDEPrefKey(CompilerFlags.P_UNKNOWN_ATTRIBUTE);
	private static final Key KEY_P_UNKNOWN_CLASS = getPDEPrefKey(CompilerFlags.P_UNKNOWN_CLASS);
	private static final Key KEY_P_UNKNOWN_RESOURCE = getPDEPrefKey(CompilerFlags.P_UNKNOWN_RESOURCE);
	private static final Key KEY_P_UNKNOWN_IDENTIFIER = getPDEPrefKey(CompilerFlags.P_UNKNOWN_IDENTIFIER);

	//general
	private static final Key KEY_P_DISCOURAGED_CLASS = getPDEPrefKey(CompilerFlags.P_DISCOURAGED_CLASS);
	private static final Key KEY_P_NO_REQUIRED_ATT = getPDEPrefKey(CompilerFlags.P_NO_REQUIRED_ATT);
	private static final Key KEY_P_NOT_EXTERNALIZED = getPDEPrefKey(CompilerFlags.P_NOT_EXTERNALIZED);
	private static final Key KEY_P_INCOMPATIBLE_ENV = getPDEPrefKey(CompilerFlags.P_INCOMPATIBLE_ENV);
	private static final Key KEY_P_MISSING_EXPORT_PKGS = getPDEPrefKey(CompilerFlags.P_MISSING_EXPORT_PKGS);
	private static final Key KEY_P_DEPRECATED = getPDEPrefKey(CompilerFlags.P_DEPRECATED);
	private static final Key KEY_P_INTERNAL = getPDEPrefKey(CompilerFlags.P_INTERNAL);

	// build
	private static final Key KEY_P_BUILD_SOURCE_LIBRARY = getPDEPrefKey(CompilerFlags.P_BUILD_SOURCE_LIBRARY);
	private static final Key KEY_P_BUILD_OUTPUT_LIBRARY = getPDEPrefKey(CompilerFlags.P_BUILD_OUTPUT_LIBRARY);
	private static final Key KEY_P_BUILD_MISSING_OUTPUT = getPDEPrefKey(CompilerFlags.P_BUILD_MISSING_OUTPUT);
	private static final Key KEY_P_BUILD_SRC_INCLUDES = getPDEPrefKey(CompilerFlags.P_BUILD_SRC_INCLUDES);
	private static final Key KEY_P_BUILD_BIN_INCLUDES = getPDEPrefKey(CompilerFlags.P_BUILD_BIN_INCLUDES);
	private static final Key KEY_P_BUILD_JAVA_COMPLIANCE = getPDEPrefKey(CompilerFlags.P_BUILD_JAVA_COMPLIANCE);
	private static final Key KEY_P_BUILD_JAVA_COMPILER = getPDEPrefKey(CompilerFlags.P_BUILD_JAVA_COMPILER);
	private static final Key KEY_P_BUILD_ENCODINGS = getPDEPrefKey(CompilerFlags.P_BUILD_ENCODINGS);
	private static final Key KEY_P_BUILD = getPDEPrefKey(CompilerFlags.P_BUILD);

	// versioning
	private static final Key KEY_P_VERSION_EXP_PKG = getPDEPrefKey(CompilerFlags.P_MISSING_VERSION_EXP_PKG);
	private static final Key KEY_P_VERSION_IMP_PKG = getPDEPrefKey(CompilerFlags.P_MISSING_VERSION_IMP_PKG);
	private static final Key KEY_P_VERSION_REQ_BUNDLE = getPDEPrefKey(CompilerFlags.P_MISSING_VERSION_REQ_BUNDLE);

	private static final Key KEY_S_CREATE_DOCS = getPDEPrefKey(CompilerFlags.S_CREATE_DOCS);
	private static final Key KEY_S_DOC_FOLDER = getPDEPrefKey(CompilerFlags.S_DOC_FOLDER);
	private static final Key KEY_S_OPEN_TAGS = getPDEPrefKey(CompilerFlags.S_OPEN_TAGS);

	private static String[] SEVERITIES = {PDEUIMessages.PDECompilersConfigurationBlock_error, PDEUIMessages.PDECompilersConfigurationBlock_warning, PDEUIMessages.PDECompilersConfigurationBlock_ignore};

	private static Key[] fgAllKeys = {KEY_F_UNRESOLVED_FEATURES, KEY_F_UNRESOLVED_PLUGINS, KEY_P_BUILD, KEY_P_BUILD_MISSING_OUTPUT, KEY_P_BUILD_SOURCE_LIBRARY, KEY_P_BUILD_OUTPUT_LIBRARY, KEY_P_BUILD_SRC_INCLUDES, KEY_P_BUILD_BIN_INCLUDES, KEY_P_BUILD_JAVA_COMPLIANCE, KEY_P_BUILD_JAVA_COMPILER, KEY_P_BUILD_ENCODINGS, KEY_P_INTERNAL, KEY_P_DEPRECATED, KEY_P_DISCOURAGED_CLASS, KEY_P_INCOMPATIBLE_ENV, KEY_P_MISSING_EXPORT_PKGS, KEY_P_NO_REQUIRED_ATT, KEY_P_NOT_EXTERNALIZED, KEY_P_UNKNOWN_ATTRIBUTE, KEY_P_UNKNOWN_CLASS, KEY_P_UNKNOWN_ELEMENT, KEY_P_UNKNOWN_IDENTIFIER, KEY_P_UNKNOWN_RESOURCE, KEY_P_UNRESOLVED_EX_POINTS, KEY_P_UNRESOLVED_IMPORTS, KEY_P_VERSION_EXP_PKG, KEY_P_VERSION_IMP_PKG, KEY_P_VERSION_REQ_BUNDLE, KEY_S_CREATE_DOCS, KEY_S_DOC_FOLDER, KEY_S_OPEN_TAGS};

	/**
	 * Constant representing the {@link IDialogSettings} section for this block
	 */
	private static final String SETTINGS = "pde_compiler_errorwarnings_block"; //$NON-NLS-1$

	/**
	 * The context of settings locations to search for values in
	 */
	private IScopeContext[] fLookupOrder = null;

	/**
	 * The project this block is working on settings for. Only applies in the 
	 * case of project specific settings
	 */
	private IProject fProject = null;

	/**
	 * the working copy manager to work with settings
	 */
	private IWorkingCopyManager fManager = null;

	/**
	 * The main composite for the configuration block, used for enabling/disabling the block 
	 */
	private Composite fMainComp = null;

	/**
	 * Stored old fProject specific settings. 
	 */
	private IdentityHashMap fOldProjectSettings = null;

	/**
	 * Map of controls to the tab they appear on. Allows for optimizing which builders
	 * are used if changes are made
	 */
	private HashMap fControlMap = new HashMap(3);

	/**
	 * Listing of all of the {@link ExpandableComposite}s in the block
	 */
	private ArrayList fExpComps = new ArrayList();

	/**
	 * Flag used to know if the page needs saving or not
	 */
	private boolean fDirty = false;

	/**
	 * counter to know how many times we have prompted users' to rebuild
	 */
	private int fRebuildcount = 0;

	/**
	 * Set of builders to use when building if there are changes
	 */
	private HashSet fBuilders = new HashSet(4);

	/**
	 * The parent this block has been added to 
	 */
	private Composite fParent = null;

	/**
	 * Default selection listener for combo and check controls 
	 */
	private SelectionListener selectionlistener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			if (e.widget instanceof Combo) {
				Combo combo = (Combo) e.widget;
				ControlData data = (ControlData) combo.getData();
				data.key.setStoredValue(fLookupOrder[0], Integer.toString(combo.getSelectionIndex()), fManager);
				fDirty = true;
				fRebuildcount = 0;
			} else if (e.widget instanceof Button) {
				Button button = (Button) e.widget;
				ControlData data = (ControlData) button.getData();
				data.key.setStoredValue(fLookupOrder[0], Boolean.toString(button.getSelection()), fManager);
				fDirty = true;
				fRebuildcount = 0;
			}
			addBuilder((Control) e.widget);
		}
	};

	/**
	 * Default modify listener for text controls
	 */
	private ModifyListener modifylistener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (e.widget instanceof Text) {
				Text text = (Text) e.widget;
				ControlData data = (ControlData) text.getData();
				data.key.setStoredValue(fLookupOrder[0], text.getText().trim(), fManager);
				fDirty = true;
				fRebuildcount = 0;
			}
		}
	};

	/**
	 * Constructor
	 * @param project
	 * @param container
	 */
	public PDECompilersConfigurationBlock(IProject project, IWorkbenchPreferenceContainer container) {
		fProject = project;
		if (fProject != null) {
			fLookupOrder = new IScopeContext[] {new ProjectScope(fProject), InstanceScope.INSTANCE, DefaultScope.INSTANCE};
		} else {
			fLookupOrder = new IScopeContext[] {InstanceScope.INSTANCE, DefaultScope.INSTANCE};
		}
		if (container == null) {
			fManager = new WorkingCopyManager();
		} else {
			fManager = container.getWorkingCopyManager();
		}
		if (fProject == null || hasProjectSpecificSettings(fProject)) {
			fOldProjectSettings = null;
		} else {
			fOldProjectSettings = new IdentityHashMap();
			for (int i = 0; i < fgAllKeys.length; i++) {
				fOldProjectSettings.put(fgAllKeys[i], fgAllKeys[i].getStoredValue(fLookupOrder, false, fManager));
			}
		}
		//make it load so we have access to the pde preferences initialized via pde core preferences 
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=273017 
		PDECore.getDefault().getPreferencesManager();
	}

	/**
	 * returns if this block has fProject specific settings
	 * @param fProject
	 * @return true if there are fProject specific settings, false otherwise
	 */
	public boolean hasProjectSpecificSettings(IProject project) {
		if (project != null) {
			IScopeContext projectContext = new ProjectScope(project);
			for (int i = 0; i < fgAllKeys.length; i++) {
				if (fgAllKeys[i].getStoredValue(projectContext, fManager) != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Sets using project specific settings
	 * @param enable
	 */
	public void useProjectSpecificSettings(boolean enable) {
		boolean disabled = fOldProjectSettings == null;
		if (enable != disabled && fProject != null) {
			if (enable) {
				for (int i = 0; i < fgAllKeys.length; i++) {
					fgAllKeys[i].setStoredValue(fLookupOrder[0], (String) fOldProjectSettings.get(fgAllKeys[i]), fManager);
				}
				fOldProjectSettings = null;
				updateControls();
			} else {
				fOldProjectSettings = new IdentityHashMap();
				String old = null;
				for (int i = 0; i < fgAllKeys.length; i++) {
					old = fgAllKeys[i].getStoredValue(fLookupOrder, false, fManager);
					fOldProjectSettings.put(fgAllKeys[i], old);
					fgAllKeys[i].setStoredValue(fLookupOrder[0], null, fManager);
				}
			}
		}
		fDirty = true;
		enableControl(fMainComp, enable);
	}

	/**
	 * Updates all of the registered {@link Control}s on the page.
	 * Registration implies that the {@link Control} was added to the map
	 * fControlMap
	 */
	private void updateControls() {
		HashSet controls = null;
		Control control = null;
		for (Iterator iter = fControlMap.keySet().iterator(); iter.hasNext();) {
			controls = (HashSet) fControlMap.get(iter.next());
			if (controls == null) {
				continue;
			}
			for (Iterator iter2 = controls.iterator(); iter2.hasNext();) {
				control = (Control) iter2.next();
				if (control instanceof Combo) {
					Combo combo = (Combo) control;
					ControlData data = (ControlData) combo.getData();
					int index = 0;
					try {
						index = Integer.parseInt(data.key.getStoredValue(fLookupOrder, false, fManager));
					} catch (Exception e) {
						//set the default if something goes wrong
						index = Integer.parseInt(data.key.getStoredValue(fLookupOrder, true, fManager));
					}
					combo.select(data.getSelection(SEVERITIES[index]));
				} else if (control instanceof Button) {
					Button button = (Button) control;
					ControlData data = (ControlData) button.getData();
					button.setSelection(Boolean.valueOf(data.key.getStoredValue(fLookupOrder, false, fManager)).booleanValue());
				} else if (control instanceof Text) {
					Text text = (Text) control;
					ControlData data = (ControlData) text.getData();
					text.setText(data.key.getStoredValue(fLookupOrder, false, fManager));
				}
			}
		}
	}

	/**
	 * recursive method to enable/disable all of the controls on the main page
	 * @param ctrl
	 * @param enabled
	 */
	private void enableControl(Control ctrl, boolean enabled) {
		ctrl.setEnabled(enabled);
		if (ctrl instanceof Composite) {
			Composite comp = (Composite) ctrl;
			Control[] children = comp.getChildren();
			for (int i = 0; i < children.length; i++) {
				enableControl(children[i], enabled);
			}
		}
	}

	/**
	 * Creates the control in the parent control
	 * 
	 * @param parent the parent control
	 */
	public Control createControl(Composite parent) {
		fParent = parent;
		fMainComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		TabFolder folder = null;
		if (fProject == null) {
			SWTFactory.createVerticalSpacer(parent, 1);
			folder = new TabFolder(fMainComp, SWT.NONE);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 375;
			folder.setLayoutData(gd);
		}

		Composite main = (folder == null ? fMainComp : folder);
		//plugins page
		createPage(CompilerFlags.PLUGIN_FLAGS, main, PDEUIMessages.CompilersConfigurationBlock_plugins, PDEUIMessages.CompilersConfigurationBlock_label);

		if (fProject == null) {
			//the sharing property page does not use these two tabs
			//schema page
			createPage(CompilerFlags.SCHEMA_FLAGS, main, PDEUIMessages.CompilersConfigurationBlock_schemas, PDEUIMessages.CompilersConfigurationBlock_altlabel);

			//features / sites page
			createPage(CompilerFlags.FEATURE_FLAGS, main, PDEUIMessages.CompilersConfigurationBlock_features, PDEUIMessages.CompilersConfigurationBlock_label);
		}
		restoreExpansionState();
		org.eclipse.jface.dialogs.Dialog.applyDialogFont(fParent);
		return fMainComp;
	}

	/**
	 * Creates a tab page parented in the folder
	 * @param kind
	 * @param folder
	 * @param name
	 * @param description
	 * @return a new composite to act as the page for a tab
	 */
	private Composite createPage(int kind, Composite folder, String name, String description) {
		Composite page = SWTFactory.createComposite(folder, 1, 1, GridData.FILL_BOTH);
		Composite parent = page;
		if (fProject == null) {
			TabItem tab = new TabItem((TabFolder) folder, SWT.NONE);
			tab.setText(name);
			tab.setControl(page);
			parent = SWTFactory.createComposite(page, 2, 1, GridData.FILL_BOTH);
		}
		SWTFactory.createWrapLabel(parent, description, 2);
		SWTFactory.createVerticalSpacer(parent, 1);
		switch (kind) {
			case CompilerFlags.PLUGIN_FLAGS : {
				ScrolledComposite scomp = createScrolledComposite(parent, 1);
				Composite sbody = SWTFactory.createComposite(scomp, 1, 1, GridData.FILL_BOTH);
				scomp.setContent(sbody);
				// General
				Composite client = createExpansibleComposite(sbody, PDEUIMessages.PDECompilersConfigurationBlock_general);
				initializeComboControls(client, new String[] {PDEUIMessages.compilers_p_no_required_att, PDEUIMessages.CompilersConfigurationTab_incompatEnv, PDEUIMessages.compilers_p_exported_pkgs}, new Key[] {KEY_P_NO_REQUIRED_ATT, KEY_P_INCOMPATIBLE_ENV, KEY_P_MISSING_EXPORT_PKGS}, CompilerFlags.PLUGIN_FLAGS);
				//build.properties
				client = createExpansibleComposite(sbody, PDEUIMessages.PDECompilersConfigurationBlock_build);
				initializeComboControls(client, new String[] {PDEUIMessages.PDECompilersConfigurationBlock_1, PDEUIMessages.PDECompilersConfigurationBlock_2, PDEUIMessages.PDECompilersConfigurationBlock_0, PDEUIMessages.PDECompilersConfigurationBlock_3, PDEUIMessages.PDECompilersConfigurationBlock_4, PDEUIMessages.PDECompilersConfigurationBlock_5, PDEUIMessages.PDECompilersConfigurationBlock_6, PDEUIMessages.PDECompilersConfigurationBlock_7, PDEUIMessages.CompilersConfigurationTab_buildPropertiesErrors}, new Key[] {KEY_P_BUILD_SOURCE_LIBRARY, KEY_P_BUILD_OUTPUT_LIBRARY, KEY_P_BUILD_MISSING_OUTPUT, KEY_P_BUILD_BIN_INCLUDES, KEY_P_BUILD_SRC_INCLUDES, KEY_P_BUILD_JAVA_COMPLIANCE, KEY_P_BUILD_JAVA_COMPILER, KEY_P_BUILD_ENCODINGS, KEY_P_BUILD}, CompilerFlags.PLUGIN_FLAGS);
				// Versioning
				client = createExpansibleComposite(sbody, PDEUIMessages.PDECompilersConfigurationBlock_versioning);
				initializeComboControls(client, new String[] {PDEUIMessages.compilers_p_missing_require_bundle, PDEUIMessages.compilers_p_missing_exp_pkg, PDEUIMessages.compilers_p_missing_imp_pkg}, new Key[] {KEY_P_VERSION_REQ_BUNDLE, KEY_P_VERSION_EXP_PKG, KEY_P_VERSION_IMP_PKG}, CompilerFlags.PLUGIN_FLAGS);
				// Usage
				client = createExpansibleComposite(sbody, PDEUIMessages.PDECompilersConfigurationBlock_usage);
				initializeComboControls(client, new String[] {PDEUIMessages.compilers_p_unresolved_import, PDEUIMessages.compilers_p_unresolved_ex_points, PDEUIMessages.compilers_p_internal, PDEUIMessages.compilers_p_deprecated, PDEUIMessages.compilers_p_not_externalized_att}, new Key[] {KEY_P_UNRESOLVED_IMPORTS, KEY_P_UNRESOLVED_EX_POINTS, KEY_P_INTERNAL, KEY_P_DEPRECATED, KEY_P_NOT_EXTERNALIZED}, CompilerFlags.PLUGIN_FLAGS);
				// References
				client = createExpansibleComposite(sbody, PDEUIMessages.PDECompilersConfigurationBlock_references);
				initializeComboControls(client, new String[] {PDEUIMessages.compilers_p_unknown_element, PDEUIMessages.compilers_p_unknown_attribute, PDEUIMessages.compilers_p_unknown_class, PDEUIMessages.compilers_p_discouraged_class, PDEUIMessages.compilers_p_unknown_resource, PDEUIMessages.compilers_p_unknown_identifier}, new Key[] {KEY_P_UNKNOWN_ELEMENT, KEY_P_UNKNOWN_ATTRIBUTE, KEY_P_UNKNOWN_CLASS, KEY_P_DISCOURAGED_CLASS, KEY_P_UNKNOWN_RESOURCE, KEY_P_UNKNOWN_IDENTIFIER,}, CompilerFlags.PLUGIN_FLAGS);

				break;
			}
			case CompilerFlags.SCHEMA_FLAGS : {
				createCheckControl(parent, PDEUIMessages.compilers_s_create_docs, KEY_S_CREATE_DOCS, CompilerFlags.SCHEMA_FLAGS);
				Composite comp = SWTFactory.createComposite(parent, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);
				createTextControl(comp, PDEUIMessages.compilers_s_doc_folder, KEY_S_DOC_FOLDER, CompilerFlags.SCHEMA_FLAGS);
				SWTFactory.createVerticalSpacer(parent, 1);
				initializeComboControls(parent, new String[] {PDEUIMessages.compilers_s_open_tags}, new Key[] {KEY_S_OPEN_TAGS}, CompilerFlags.SCHEMA_FLAGS);
				break;
			}
			case CompilerFlags.FEATURE_FLAGS : {
				initializeComboControls(parent, new String[] {PDEUIMessages.compilers_f_unresolved_features, PDEUIMessages.compilers_f_unresolved_plugins}, new Key[] {KEY_F_UNRESOLVED_FEATURES, KEY_F_UNRESOLVED_PLUGINS}, CompilerFlags.FEATURE_FLAGS);
				break;
			}
		}
		return page;
	}

	/**
	 * Creates a set of combo boxes for the given string/ key pairs
	 * @param composite
	 * @param labels
	 * @param keys
	 * @param tabkind
	 */
	private void initializeComboControls(Composite composite, String[] labels, Key[] keys, int tabkind) {
		for (int i = 0, max = labels.length; i < max; i++) {
			createComboControl(composite, labels[i], keys[i], tabkind);
		}
	}

	/**
	 * Creates a checkbox button control in the parent
	 * @param parent
	 * @param label
	 * @param key
	 * @param tabkind
	 */
	private void createCheckControl(Composite parent, String label, Key key, int tabkind) {
		Button button = SWTFactory.createCheckButton(parent, label, null, false, 2);
		ControlData data = new ControlData(key, new String[] {Boolean.toString(false)});
		button.setData(data);
		button.setSelection(Boolean.valueOf(data.key.getStoredValue(fLookupOrder, false, fManager)).booleanValue());
		button.addSelectionListener(selectionlistener);
		Integer mapkey = new Integer(tabkind);
		HashSet controls = (HashSet) fControlMap.get(mapkey);
		if (controls == null) {
			controls = new HashSet(8);
			fControlMap.put(mapkey, controls);
		}
		controls.add(button);
	}

	/**
	 * Disposes the controls from this page
	 */
	public void dispose() {
		fMainComp.getParent().dispose();
		fExpComps.clear();
		fControlMap.clear();
	}

	/**
	 * Creates a {@link Label} | {@link Combo} control. The combo is initialized from the given {@link Key}
	 * @param parent
	 * @param label
	 * @param key
	 * @param tabkind
	 */
	protected void createComboControl(Composite parent, String label, Key key, int tabkind) {
		Label lbl = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
		lbl.setLayoutData(gd);
		lbl.setText(label);
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		gd = new GridData(GridData.END, GridData.CENTER, false, false);
		ControlData data = new ControlData(key, SEVERITIES);
		combo.setData(data);
		combo.setItems(SEVERITIES);
		combo.addSelectionListener(selectionlistener);
		int index = 0;
		String value = key.getStoredValue(fLookupOrder, false, fManager);
		if (value == null)
			value = key.getStoredValue(fLookupOrder, true, fManager);

		if (value != null)
			index = Integer.parseInt(value);
		combo.select(data.getSelection(SEVERITIES[index]));
		Integer mapkey = new Integer(tabkind);
		HashSet controls = (HashSet) fControlMap.get(mapkey);
		if (controls == null) {
			controls = new HashSet(8);
			fControlMap.put(mapkey, controls);
		}
		controls.add(combo);
		addHighlight(parent, lbl, combo);
	}

	/**
	 * Creates a new text control on the parent for the given pref key
	 * @param parent
	 * @param label
	 * @param key
	 * @param tabkind
	 */
	private void createTextControl(Composite parent, String label, Key key, int tabkind) {
		SWTFactory.createLabel(parent, label, 1);
		Text text = SWTFactory.createSingleText(parent, 1);
		ControlData data = new ControlData(key, new String[0]);
		text.setData(data);
		String value = data.key.getStoredValue(fLookupOrder, false, fManager);
		if (value != null)
			text.setText(value);
		text.addModifyListener(modifylistener);
		Integer mapkey = new Integer(tabkind);
		HashSet controls = (HashSet) fControlMap.get(mapkey);
		if (controls == null) {
			controls = new HashSet(8);
			fControlMap.put(mapkey, controls);
		}
		controls.add(text);
	}

	/**
	 * Creates a scrolled composite
	 * @param parent
	 * @param columns
	 * @return a new scrolled composite
	 */
	protected ScrolledComposite createScrolledComposite(Composite parent, int columns) {
		ScrolledComposite scomp = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scomp.setExpandHorizontal(true);
		scomp.setExpandVertical(true);
		scomp.setLayout(new GridLayout(1, false));
		scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scomp.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				handleExpand(getScrollingParent(event.widget));
			}
		});
		return scomp;
	}

	/**
	 * Creates an {@link ExpandableComposite} with a client composite and a default grid layout
	 * @param parent
	 * @param title
	 * @return a new expandable composite
	 */
	private Composite createExpansibleComposite(Composite parent, String title) {
		ExpandableComposite ecomp = SWTFactory.createExpandibleComposite(parent, title, 1, GridData.FILL_HORIZONTAL);
		ecomp.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				Object obj = e.getSource();
				handleExpand(getScrollingParent(obj));
			}
		});
		ecomp.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		fExpComps.add(ecomp);
		Composite client = SWTFactory.createComposite(ecomp, 2, 1, GridData.FILL_BOTH);
		ecomp.setClient(client);
		return client;
	}

	/**
	 * Returns the scrolling parent for the given ExpandibleComposite object
	 * @param obj
	 * @return the scrolling parent of the given object or <code>null</code> if there isn't one
	 */
	private ScrolledComposite getScrollingParent(Object obj) {
		if (obj instanceof ExpandableComposite) {
			ExpandableComposite ecomp = (ExpandableComposite) obj;
			Composite parent = ecomp.getParent();
			while (parent != null && !(parent instanceof ScrolledComposite)) {
				parent = parent.getParent();
			}
			if (parent != null) {
				return (ScrolledComposite) parent;
			}
		}
		if (obj instanceof ScrolledComposite) {
			return (ScrolledComposite) obj;
		}
		return null;
	}

	/**
	 * Handles one of the expandable composites being expanded 
	 */
	private void handleExpand(ScrolledComposite composite) {
		if (composite == null) {
			return;
		}
		try {
			composite.setRedraw(false);
			Composite c = (Composite) composite.getContent();
			if (c == null) {
				return;
			}
			Point newSize = c.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			composite.setMinSize(newSize);
			c.layout(true);
		} finally {
			composite.setRedraw(true);
		}
	}

	/**
	 * Saves all of the changes on the page
	 */
	public void performOK() {
		save();
		persistExpansionState();
	}

	/**
	 * Directly applies all of the changes on the page
	 */
	public void performApply() {
		save();
	}

	/**
	 * Performs the save operation on the working cop manager
	 */
	private void save() {
		if (fDirty) {
			try {
				ArrayList changes = new ArrayList();
				collectChanges(fLookupOrder[0], changes);
				if (changes.size() > 0) {
					if (fRebuildcount < 1) {
						fRebuildcount++;
						fManager.applyChanges();
						String message = PDEUIMessages.PDECompilersConfigurationBlock_settings_changed_all;
						if (fProject != null) {
							message = MessageFormat.format(PDEUIMessages.PDECompilersConfigurationBlock_setting_changed_project, new String[] {fProject.getName()});
						}
						if (MessageDialog.openQuestion(fParent.getShell(), PDEUIMessages.PDECompilersConfigurationBlock_settings_changed, message)) {
							doFullBuild();
						}
					}
				}
				fDirty = false;
			} catch (BackingStoreException bse) {
				PDEPlugin.log(bse);
			}
		}
	}

	/**
	 * Collects the keys that have changed on the page into the specified list
	 * @param changes the {@link List} to collect changed keys into
	 */
	private void collectChanges(IScopeContext context, List changes) {
		Key key = null;
		String origval = null, newval = null;
		boolean complete = fOldProjectSettings == null && fProject != null;
		for (int i = 0; i < fgAllKeys.length; i++) {
			key = fgAllKeys[i];
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

	/**
	 * Cancels all of the changes on the page
	 */
	public void performCancel() {
		persistExpansionState();
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
		updateControls();
		fDirty = true;
		fRebuildcount = 0;
	}

	/**
	 * Stores the expansion state of the composites
	 */
	private void persistExpansionState() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS);
		for (int i = 0; i < fExpComps.size(); i++) {
			settings.put(Integer.toString(i), ((ExpandableComposite) fExpComps.get(i)).isExpanded());
		}
	}

	/**
	 * Restores the expansion state of the composites in this block.
	 * If there are no settings, than the first composite is expanded by default
	 */
	private void restoreExpansionState() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS);
		if (settings != null) {
			for (int i = 0; i < fExpComps.size(); i++) {
				((ExpandableComposite) fExpComps.get(i)).setExpanded(settings.getBoolean(Integer.toString(i)));
			}
		} else {
			((ExpandableComposite) fExpComps.get(0)).setExpanded(true);
		}
	}

	/**
	 * Adds the associated builder for the tab this control lives on. Lookups are done using 
	 * hash codes for constant time containment checks
	 * @param control
	 */
	private void addBuilder(Control control) {
		HashSet controls = null;
		Integer key = null;
		for (Iterator iter = fControlMap.keySet().iterator(); iter.hasNext();) {
			key = (Integer) iter.next();
			controls = (HashSet) fControlMap.get(key);
			if (controls == null) {
				continue;
			}
			if (controls.contains(control)) {
				switch (key.intValue()) {
					case CompilerFlags.PLUGIN_FLAGS : {
						fBuilders.add(PDE.MANIFEST_BUILDER_ID);
						break;
					}
					case CompilerFlags.SCHEMA_FLAGS : {
						fBuilders.add(PDE.SCHEMA_BUILDER_ID);
						break;
					}
					case CompilerFlags.FEATURE_FLAGS :
					case CompilerFlags.SITE_FLAGS : {
						fBuilders.add(PDE.FEATURE_BUILDER_ID);
						break;
					}
				}
				return;
			}
		}
	}

	/**
	 * Performs a full build of the workspace
	 */
	private void doFullBuild() {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) {
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}

			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject[] projects = null;
					if (fProject == null) {
						projects = PDEPlugin.getWorkspace().getRoot().getProjects();
					} else {
						projects = new IProject[] {fProject};
					}
					monitor.beginTask("", projects.length * 2); //$NON-NLS-1$
					for (int i = 0; i < projects.length; i++) {
						IProject projectToBuild = projects[i];
						if (!projectToBuild.isOpen())
							continue;
						if (projectToBuild.hasNature(PDE.PLUGIN_NATURE)) {
							if (fBuilders.contains(PDE.MANIFEST_BUILDER_ID))
								projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, PDE.MANIFEST_BUILDER_ID, null, new SubProgressMonitor(monitor, 1));
							else
								monitor.worked(1);
							if (fBuilders.contains(PDE.SCHEMA_BUILDER_ID))
								projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, PDE.SCHEMA_BUILDER_ID, null, new SubProgressMonitor(monitor, 1));
							else
								monitor.worked(1);
						} else if (projectToBuild.hasNature(PDE.FEATURE_NATURE)) {
							if (fBuilders.contains(PDE.FEATURE_BUILDER_ID))
								projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, PDE.FEATURE_BUILDER_ID, null, new SubProgressMonitor(monitor, 2));
						} else {
							monitor.worked(2);
						}
					}
				} catch (CoreException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true);
		buildJob.schedule();
	}
}
