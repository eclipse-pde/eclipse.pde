/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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

import java.awt.Checkbox;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.internal.ui.preferences.ConfigurationBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

import com.ibm.icu.text.MessageFormat;

/**
 * This block is used to add the API Tools notification settings UI to a parent
 * control
 *
 * @since 1.0.0
 */
public class ApiErrorsWarningsConfigurationBlock extends ConfigurationBlock {

	public static final String P2_INSTALL_COMMAND_HANDLER = "org.eclipse.equinox.p2.ui.sdk.install"; //$NON-NLS-1$

	/**
	 * Provides data information for created controls
	 */
	protected static class ControlData {
		private Key key;
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
	public static class Key {

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

		public String getQualifier() {
			return qualifier;
		}

		public String getName() {
			return key;
		}

		/**
		 * Returns the {@link IEclipsePreferences} node for the given context
		 * and {@link IWorkingCopyManager}
		 *
		 * @param context
		 * @param manager
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
		 * @param context
		 * @param manager
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
		 * @param lookupOrder
		 * @param ignoreTopScope
		 * @param manager
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
		 *
		 * @param context
		 * @param value
		 * @param manager
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
	 * @param key
	 * @return the new {@link Key} for the {@link ApiUIPlugin} preference store
	 */
	protected final static Key getApiToolsKey(String key) {
		return new Key(ApiPlugin.PLUGIN_ID, key);
	}

	// Restriction modifier keys
	private static final Key KEY_NOIMPLEMENT = getApiToolsKey(IApiProblemTypes.ILLEGAL_IMPLEMENT);
	private static final Key KEY_NOEXTEND = getApiToolsKey(IApiProblemTypes.ILLEGAL_EXTEND);
	private static final Key KEY_NOINSTANTIATE = getApiToolsKey(IApiProblemTypes.ILLEGAL_INSTANTIATE);
	private static final Key KEY_NOREFERENCE = getApiToolsKey(IApiProblemTypes.ILLEGAL_REFERENCE);
	private static final Key KEY_NOOVERRIDE = getApiToolsKey(IApiProblemTypes.ILLEGAL_OVERRIDE);
	private static final Key KEY_LEAK_EXTEND = getApiToolsKey(IApiProblemTypes.LEAK_EXTEND);
	private static final Key KEY_LEAK_IMPLEMENT = getApiToolsKey(IApiProblemTypes.LEAK_IMPLEMENT);
	private static final Key KEY_LEAK_METHOD_RETURN_TYPE = getApiToolsKey(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE);
	private static final Key KEY_LEAK_FIELD_DECL = getApiToolsKey(IApiProblemTypes.LEAK_FIELD_DECL);
	private static final Key KEY_LEAK_METHOD_PARAM = getApiToolsKey(IApiProblemTypes.LEAK_METHOD_PARAM);
	private static final Key KEY_INVALID_JAVADOC_TAG = getApiToolsKey(IApiProblemTypes.INVALID_JAVADOC_TAG);
	private static final Key KEY_INVALID_ANNOTATION = getApiToolsKey(IApiProblemTypes.INVALID_ANNOTATION);
	private static final Key KEY_INVALID_REFERENCE_IN_SYSTEM_LIBRARIES = getApiToolsKey(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES);
	private static final Key KEY_UNUSED_PROBLEM_FILTERS = getApiToolsKey(IApiProblemTypes.UNUSED_PROBLEM_FILTERS);
	private static final Key KEY_MISSING_EE_DESCRIPTIONS = getApiToolsKey(IApiProblemTypes.MISSING_EE_DESCRIPTIONS);

	// compatibility keys
	private static final Key KEY_API_COMPONENT_REMOVED_API_TYPE = getApiToolsKey(IApiProblemTypes.API_COMPONENT_REMOVED_API_TYPE);
	private static final Key KEY_API_COMPONENT_REMOVED_TYPE = getApiToolsKey(IApiProblemTypes.API_COMPONENT_REMOVED_TYPE);
	private static final Key KEY_API_COMPONENT_REMOVED_REEXPORTED_API_TYPE = getApiToolsKey(IApiProblemTypes.API_COMPONENT_REMOVED_REEXPORTED_API_TYPE);
	private static final Key KEY_API_COMPONENT_REMOVED_REEXPORTED_TYPE = getApiToolsKey(IApiProblemTypes.API_COMPONENT_REMOVED_REEXPORTED_TYPE);

	private static final Key KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE = getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE);
	private static final Key KEY_ANNOTATION_ADDED_FIELD = getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_FIELD);
	private static final Key KEY_ANNOTATION_REMOVED_FIELD = getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_FIELD);
	private static final Key KEY_ANNOTATION_REMOVED_METHOD = getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_METHOD);
	private static final Key KEY_ANNOTATION_REMOVED_TYPE_MEMBER = getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_TYPE_MEMBER);
	private static final Key KEY_ANNOTATION_CHANGED_TYPE_CONVERSION = getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_TYPE_CONVERSION);

	// interface key constant
	private static final Key KEY_INTERFACE_ADDED_FIELD = getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_FIELD);
	private static final Key KEY_INTERFACE_ADDED_METHOD = getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_METHOD);
	private static final Key KEY_INTERFACE_ADDED_DEFAULT_METHOD = getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_DEFAULT_METHOD);
	private static final Key KEY_INTERFACE_ADDED_RESTRICTIONS = getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_RESTRICTIONS);
	private static final Key KEY_INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS = getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS);
	private static final Key KEY_INTERFACE_ADDED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_TYPE_PARAMETER);
	private static final Key KEY_INTERFACE_REMOVED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_TYPE_PARAMETER);
	private static final Key KEY_INTERFACE_REMOVED_FIELD = getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_FIELD);
	private static final Key KEY_INTERFACE_REMOVED_METHOD = getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_METHOD);
	private static final Key KEY_INTERFACE_REMOVED_TYPE_MEMBER = getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_TYPE_MEMBER);
	private static final Key KEY_INTERFACE_CHANGED_TYPE_CONVERSION = getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_TYPE_CONVERSION);
	private static final Key KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET = getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET);

	// enum key constant
	private static final Key KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET = getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
	private static final Key KEY_ENUM_CHANGED_TYPE_CONVERSION = getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_TYPE_CONVERSION);
	private static final Key KEY_ENUM_REMOVED_FIELD = getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_FIELD);
	private static final Key KEY_ENUM_REMOVED_ENUM_CONSTANT = getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_ENUM_CONSTANT);
	private static final Key KEY_ENUM_REMOVED_METHOD = getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_METHOD);
	private static final Key KEY_ENUM_REMOVED_TYPE_MEMBER = getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_TYPE_MEMBER);

	// class key constant
	private static final Key KEY_CLASS_ADDED_FIELD = getApiToolsKey(IApiProblemTypes.CLASS_ADDED_FIELD);
	private static final Key KEY_CLASS_ADDED_METHOD = getApiToolsKey(IApiProblemTypes.CLASS_ADDED_METHOD);
	private static final Key KEY_CLASS_ADDED_RESTRICTIONS = getApiToolsKey(IApiProblemTypes.CLASS_ADDED_RESTRICTIONS);
	private static final Key KEY_CLASS_ADDED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.CLASS_ADDED_TYPE_PARAMETER);
	private static final Key KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET = getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
	private static final Key KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT = getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT);
	private static final Key KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL = getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_CLASS_CHANGED_TYPE_CONVERSION = getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_TYPE_CONVERSION);
	private static final Key KEY_CLASS_CHANGED_DECREASE_ACCESS = getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_CLASS_REMOVED_FIELD = getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_FIELD);
	private static final Key KEY_CLASS_REMOVED_METHOD = getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_METHOD);
	private static final Key KEY_CLASS_REMOVED_CONSTRUCTOR = getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_CONSTRUCTOR);
	private static final Key KEY_CLASS_REMOVED_SUPERCLASS = getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_SUPERCLASS);
	private static final Key KEY_CLASS_REMOVED_TYPE_MEMBER = getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_TYPE_MEMBER);
	private static final Key KEY_CLASS_REMOVED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_TYPE_PARAMETER);

	// field key constant
	private static final Key KEY_FIELD_ADDED_VALUE = getApiToolsKey(IApiProblemTypes.FIELD_ADDED_VALUE);
	private static final Key KEY_FIELD_CHANGED_TYPE = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_TYPE);
	private static final Key KEY_FIELD_CHANGED_VALUE = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_VALUE);
	private static final Key KEY_FIELD_CHANGED_DECREASE_ACCESS = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT);
	private static final Key KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_STATIC_TO_NON_STATIC);
	private static final Key KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC = getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_NON_STATIC_TO_STATIC);
	private static final Key KEY_FIELD_REMOVED_VALUE = getApiToolsKey(IApiProblemTypes.FIELD_REMOVED_VALUE);
	private static final Key KEY_FIELD_REMOVED_TYPE_ARGUMENT = getApiToolsKey(IApiProblemTypes.FIELD_REMOVED_TYPE_ARGUMENT);

	// method key constant
	private static final Key KEY_METHOD_ADDED_RESTRICTIONS = getApiToolsKey(IApiProblemTypes.METHOD_ADDED_RESTRICTIONS);
	private static final Key KEY_METHOD_ADDED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.METHOD_ADDED_TYPE_PARAMETER);
	private static final Key KEY_METHOD_CHANGED_VARARGS_TO_ARRAY = getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_VARARGS_TO_ARRAY);
	private static final Key KEY_METHOD_CHANGED_DECREASE_ACCESS = getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT = getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT);
	private static final Key KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC = getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_NON_STATIC_TO_STATIC);
	private static final Key KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC = getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_STATIC_TO_NON_STATIC);
	private static final Key KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL = getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE = getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE);
	private static final Key KEY_METHOD_REMOVED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_TYPE_PARAMETER);

	// constructor key constant
	private static final Key KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_ADDED_TYPE_PARAMETER);
	private static final Key KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY = getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY);
	private static final Key KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS = getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER = getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_TYPE_PARAMETER);

	private static final Key KEY_TYPE_PARAMETER_ADDED_CLASS_BOUND = getApiToolsKey(IApiProblemTypes.TYPE_PARAMETER_ADDED_CLASS_BOUND);
	private static final Key KEY_TYPE_PARAMETER_CHANGED_CLASS_BOUND = getApiToolsKey(IApiProblemTypes.TYPE_PARAMETER_CHANGED_CLASS_BOUND);
	private static final Key KEY_TYPE_PARAMETER_REMOVED_CLASS_BOUND = getApiToolsKey(IApiProblemTypes.TYPE_PARAMETER_REMOVED_CLASS_BOUND);
	private static final Key KEY_TYPE_PARAMETER_ADDED_INTERFACE_BOUND = getApiToolsKey(IApiProblemTypes.TYPE_PARAMETER_ADDED_INTERFACE_BOUND);
	private static final Key KEY_TYPE_PARAMETER_CHANGED_INTERFACE_BOUND = getApiToolsKey(IApiProblemTypes.TYPE_PARAMETER_CHANGED_INTERFACE_BOUND);
	private static final Key KEY_TYPE_PARAMETER_REMOVED_INTERFACE_BOUND = getApiToolsKey(IApiProblemTypes.TYPE_PARAMETER_REMOVED_INTERFACE_BOUND);

	// version management keys
	private static final Key KEY_MISSING_SINCE_TAG = getApiToolsKey(IApiProblemTypes.MISSING_SINCE_TAG);
	private static final Key KEY_MALFORMED_SINCE_TAG = getApiToolsKey(IApiProblemTypes.MALFORMED_SINCE_TAG);
	private static final Key KEY_INVALID_SINCE_TAG_VERSION = getApiToolsKey(IApiProblemTypes.INVALID_SINCE_TAG_VERSION);
	private static final Key KEY_CHANGED_EXECUTION_ENV = getApiToolsKey(IApiProblemTypes.CHANGED_EXECUTION_ENV);
	private static final Key KEY_INCOMPATIBLE_API_COMPONENT_VERSION = getApiToolsKey(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION);
	private static final Key KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE = getApiToolsKey(
			IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE);
	private static final Key KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE = getApiToolsKey(
			IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE);
	private static final Key KEY_REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED = getApiToolsKey(IApiProblemTypes.REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED);

	private static final Key KEY_REPORT_RESOLUTION_ERRORS_API_COMPONENT = getApiToolsKey(IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT);
	// External Dependencies' Keys
	private static final Key KEY_API_USE_SCAN_TYPE_PROBLEM = getApiToolsKey(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY);
	private static final Key KEY_API_USE_SCAN_METHOD_PROBLEM = getApiToolsKey(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY);
	private static final Key KEY_API_USE_SCAN_FIELD_PROBLEM = getApiToolsKey(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY);
	/**
	 * @since 1.1
	 */
	private static final Key KEY_AUTOMATICALLY_REMOVE_PROBLEM_FILTERS = getApiToolsKey(IApiProblemTypes.AUTOMATICALLY_REMOVE_UNUSED_PROBLEM_FILTERS);

	public static final int API_SCANNING_USAGE_PAGE_ID = 0;
	public static final int COMPATIBILITY_PAGE_ID = 1;
	public static final int VERSION_MANAGEMENT_PAGE_ID = 2;
	public static final int API_COMPONENT_RESOLUTION_PAGE_ID = 3;
	public static final int API_USE_SCANS_PAGE_ID = 4;

	static Key[] fgAllApiComponentResolutionKeys = {
			KEY_REPORT_RESOLUTION_ERRORS_API_COMPONENT,
			KEY_UNUSED_PROBLEM_FILTERS,
			KEY_AUTOMATICALLY_REMOVE_PROBLEM_FILTERS };

	static Key[] fgAllCompatibilityKeys = {
			KEY_API_COMPONENT_REMOVED_API_TYPE, KEY_API_COMPONENT_REMOVED_TYPE,
			KEY_API_COMPONENT_REMOVED_REEXPORTED_API_TYPE,
			KEY_API_COMPONENT_REMOVED_REEXPORTED_TYPE,
			KEY_ANNOTATION_ADDED_FIELD,
			KEY_ANNOTATION_REMOVED_FIELD, KEY_ANNOTATION_REMOVED_METHOD,
			KEY_ANNOTATION_REMOVED_TYPE_MEMBER,
			KEY_ANNOTATION_CHANGED_TYPE_CONVERSION,
			KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
			KEY_INTERFACE_ADDED_FIELD, KEY_INTERFACE_ADDED_METHOD,
			KEY_INTERFACE_ADDED_DEFAULT_METHOD,
			KEY_INTERFACE_ADDED_RESTRICTIONS,
			KEY_INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS,
			KEY_INTERFACE_ADDED_TYPE_PARAMETER,
			KEY_INTERFACE_REMOVED_TYPE_PARAMETER, KEY_INTERFACE_REMOVED_FIELD,
			KEY_INTERFACE_REMOVED_METHOD, KEY_INTERFACE_REMOVED_TYPE_MEMBER,
			KEY_INTERFACE_CHANGED_TYPE_CONVERSION,
			KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
			KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
			KEY_ENUM_CHANGED_TYPE_CONVERSION, KEY_ENUM_REMOVED_FIELD,
			KEY_ENUM_REMOVED_ENUM_CONSTANT, KEY_ENUM_REMOVED_METHOD,
			KEY_ENUM_REMOVED_TYPE_MEMBER, KEY_CLASS_ADDED_FIELD, KEY_CLASS_ADDED_METHOD,
			KEY_CLASS_ADDED_RESTRICTIONS, KEY_CLASS_ADDED_TYPE_PARAMETER,
			KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
			KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
			KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL,
			KEY_CLASS_CHANGED_TYPE_CONVERSION,
			KEY_CLASS_CHANGED_DECREASE_ACCESS, KEY_CLASS_REMOVED_FIELD,
			KEY_CLASS_REMOVED_METHOD, KEY_CLASS_REMOVED_CONSTRUCTOR,
			KEY_CLASS_REMOVED_SUPERCLASS, KEY_CLASS_REMOVED_TYPE_MEMBER,
			KEY_CLASS_REMOVED_TYPE_PARAMETER, KEY_FIELD_ADDED_VALUE,
			KEY_FIELD_CHANGED_TYPE, KEY_FIELD_CHANGED_VALUE,
			KEY_FIELD_CHANGED_DECREASE_ACCESS,
			KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
			KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL,
			KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC,
			KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC, KEY_FIELD_REMOVED_VALUE,
			KEY_FIELD_REMOVED_TYPE_ARGUMENT, KEY_METHOD_ADDED_RESTRICTIONS,
			KEY_METHOD_ADDED_TYPE_PARAMETER,
			KEY_METHOD_CHANGED_VARARGS_TO_ARRAY,
			KEY_METHOD_CHANGED_DECREASE_ACCESS,
			KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
			KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC,
			KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC,
			KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL,
			KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
			KEY_METHOD_REMOVED_TYPE_PARAMETER,
			KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER,
			KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
			KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
			KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
			KEY_TYPE_PARAMETER_ADDED_CLASS_BOUND,
			KEY_TYPE_PARAMETER_ADDED_INTERFACE_BOUND,
			KEY_TYPE_PARAMETER_CHANGED_CLASS_BOUND,
			KEY_TYPE_PARAMETER_CHANGED_INTERFACE_BOUND,
			KEY_TYPE_PARAMETER_REMOVED_CLASS_BOUND,
			KEY_TYPE_PARAMETER_REMOVED_INTERFACE_BOUND, };

	static Key[] fgAllApiScanningKeys = {
			KEY_NOIMPLEMENT, KEY_NOEXTEND, KEY_NOREFERENCE, KEY_NOINSTANTIATE,
			KEY_NOOVERRIDE, KEY_LEAK_EXTEND, KEY_LEAK_FIELD_DECL,
			KEY_LEAK_IMPLEMENT, KEY_LEAK_METHOD_PARAM,
			KEY_LEAK_METHOD_RETURN_TYPE, KEY_INVALID_JAVADOC_TAG,
			KEY_INVALID_ANNOTATION, KEY_INVALID_REFERENCE_IN_SYSTEM_LIBRARIES,
			KEY_MISSING_EE_DESCRIPTIONS };

	static Key[] fgAllVersionManagementKeys = {
			KEY_MISSING_SINCE_TAG, KEY_MALFORMED_SINCE_TAG,
			KEY_INVALID_SINCE_TAG_VERSION,
			KEY_INCOMPATIBLE_API_COMPONENT_VERSION,
			KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
			KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE, KEY_CHANGED_EXECUTION_ENV };

	static Key[] fgAllExternalDependenciesKeys = {
			KEY_API_USE_SCAN_TYPE_PROBLEM, KEY_API_USE_SCAN_METHOD_PROBLEM,
			KEY_API_USE_SCAN_FIELD_PROBLEM };

	/**
	 * An array of all of the keys for the page
	 */
	private static Key[] fgAllKeys = {
			KEY_NOIMPLEMENT,
			KEY_NOEXTEND,
			KEY_NOINSTANTIATE,
			KEY_NOREFERENCE,
			KEY_NOOVERRIDE,
			KEY_LEAK_EXTEND,
			KEY_LEAK_FIELD_DECL,
			KEY_LEAK_IMPLEMENT,
			KEY_LEAK_METHOD_PARAM,
			KEY_LEAK_METHOD_RETURN_TYPE,
			KEY_INVALID_JAVADOC_TAG,
			KEY_INVALID_ANNOTATION,
			KEY_INVALID_REFERENCE_IN_SYSTEM_LIBRARIES,
			KEY_UNUSED_PROBLEM_FILTERS,
			KEY_MISSING_EE_DESCRIPTIONS,
			KEY_API_COMPONENT_REMOVED_API_TYPE,
			KEY_API_COMPONENT_REMOVED_TYPE,
			KEY_API_COMPONENT_REMOVED_REEXPORTED_API_TYPE,
			KEY_API_COMPONENT_REMOVED_REEXPORTED_TYPE,
			KEY_ANNOTATION_ADDED_FIELD,
			KEY_ANNOTATION_REMOVED_FIELD,
			KEY_ANNOTATION_REMOVED_METHOD,
			KEY_ANNOTATION_REMOVED_TYPE_MEMBER,
			KEY_ANNOTATION_CHANGED_TYPE_CONVERSION,
			KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
			KEY_INTERFACE_ADDED_FIELD,
			KEY_INTERFACE_ADDED_METHOD,
			KEY_INTERFACE_ADDED_DEFAULT_METHOD,
			KEY_INTERFACE_ADDED_RESTRICTIONS,
			KEY_INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS,
			KEY_INTERFACE_ADDED_TYPE_PARAMETER,
			KEY_INTERFACE_REMOVED_TYPE_PARAMETER,
			KEY_INTERFACE_REMOVED_FIELD,
			KEY_INTERFACE_REMOVED_METHOD,
			KEY_INTERFACE_REMOVED_TYPE_MEMBER,
			KEY_INTERFACE_CHANGED_TYPE_CONVERSION,
			KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
			KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
			KEY_ENUM_CHANGED_TYPE_CONVERSION,
			KEY_ENUM_REMOVED_FIELD,
			KEY_ENUM_REMOVED_ENUM_CONSTANT,
			KEY_ENUM_REMOVED_METHOD,
			KEY_ENUM_REMOVED_TYPE_MEMBER,
			KEY_CLASS_ADDED_FIELD,
			KEY_CLASS_ADDED_METHOD,
			KEY_CLASS_ADDED_RESTRICTIONS,
			KEY_CLASS_ADDED_TYPE_PARAMETER,
			KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
			KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
			KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL,
			KEY_CLASS_CHANGED_TYPE_CONVERSION,
			KEY_CLASS_CHANGED_DECREASE_ACCESS,
			KEY_CLASS_REMOVED_FIELD,
			KEY_CLASS_REMOVED_METHOD,
			KEY_CLASS_REMOVED_CONSTRUCTOR,
			KEY_CLASS_REMOVED_SUPERCLASS,
			KEY_CLASS_REMOVED_TYPE_MEMBER,
			KEY_CLASS_REMOVED_TYPE_PARAMETER,
			KEY_FIELD_ADDED_VALUE,
			KEY_FIELD_CHANGED_TYPE,
			KEY_FIELD_CHANGED_VALUE,
			KEY_FIELD_CHANGED_DECREASE_ACCESS,
			KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
			KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL,
			KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC,
			KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC,
			KEY_FIELD_REMOVED_VALUE,
			KEY_FIELD_REMOVED_TYPE_ARGUMENT,
			KEY_METHOD_ADDED_RESTRICTIONS,
			KEY_METHOD_ADDED_TYPE_PARAMETER,
			KEY_METHOD_CHANGED_VARARGS_TO_ARRAY,
			KEY_METHOD_CHANGED_DECREASE_ACCESS,
			KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
			KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC,
			KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC,
			KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL,
			KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
			KEY_METHOD_REMOVED_TYPE_PARAMETER,
			KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER,
			KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
			KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
			KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
			KEY_TYPE_PARAMETER_ADDED_CLASS_BOUND,
			KEY_TYPE_PARAMETER_ADDED_INTERFACE_BOUND,
			KEY_TYPE_PARAMETER_CHANGED_CLASS_BOUND,
			KEY_TYPE_PARAMETER_CHANGED_INTERFACE_BOUND,
			KEY_TYPE_PARAMETER_REMOVED_CLASS_BOUND,
			KEY_TYPE_PARAMETER_REMOVED_INTERFACE_BOUND,
			KEY_MISSING_SINCE_TAG,
			KEY_MALFORMED_SINCE_TAG,
			KEY_INVALID_SINCE_TAG_VERSION,
			KEY_CHANGED_EXECUTION_ENV,
			KEY_INCOMPATIBLE_API_COMPONENT_VERSION,
			KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
			KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
			KEY_REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED,
			KEY_REPORT_RESOLUTION_ERRORS_API_COMPONENT,
			KEY_AUTOMATICALLY_REMOVE_PROBLEM_FILTERS,
			KEY_API_USE_SCAN_TYPE_PROBLEM, KEY_API_USE_SCAN_METHOD_PROBLEM,
			KEY_API_USE_SCAN_FIELD_PROBLEM };

	/**
	 * Constant representing the {@link IDialogSettings} section for this block
	 */
	private static final String SETTINGS = "api_errorswarnings_block"; //$NON-NLS-1$

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
	 * Default value set for check box controls
	 *
	 * @since 1.1
	 */
	private static final String[] CHECKBOX_VALUES = {
			ApiPlugin.VALUE_ENABLED, ApiPlugin.VALUE_DISABLED };

	/**
	 * Default selection listener for controls on the page
	 */
	private SelectionListener selectionlistener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Widget widget = e.widget;
			ControlData data = (ControlData) widget.getData();
			Key key = data.getKey();
			String newValue = null;
			if (widget instanceof Button) {
				newValue = data.getValue(((Button) widget).getSelection());
			} else if (widget instanceof Combo) {
				newValue = data.getValue(((Combo) widget).getSelectionIndex());
			} else {
				return;
			}
			setValue(key, newValue);
			validateSettings(key);
		}
	};

	public void validateSettings(Key changedKey) {
		// we need to disable the two checkboxes if the key is for the bundle
		// version
		if (changedKey != null) {
			if (KEY_INCOMPATIBLE_API_COMPONENT_VERSION.equals(changedKey) || KEY_INVALID_REFERENCE_IN_SYSTEM_LIBRARIES.equals(changedKey)) {
				updateEnableStates();
			}
		} else {
			updateEnableStates();
		}
	}

	protected boolean checkValue(Key key, String value) {
		return value.equals(getValue(key));
	}

	protected String getValue(Key key) {
		if (fOldProjectSettings != null) {
			return fOldProjectSettings.get(key);
		}
		return key.getStoredValue(fLookupOrder, false, fManager);
	}

	protected String setValue(Key key, String value) {
		fDirty = true;
		fRebuildcount = 0;
		if (fOldProjectSettings != null) {
			return fOldProjectSettings.put(key, value);
		}
		String oldValue = getValue(key);
		key.setStoredValue(fLookupOrder[0], value, fManager);
		return oldValue;
	}

	private void updateEnableStates() {
		boolean enableSystemLibraryCheck = !checkValue(KEY_INVALID_REFERENCE_IN_SYSTEM_LIBRARIES,
				ApiPlugin.VALUE_IGNORE);
		if (this.fSystemLibraryControls != null) {
			for (Control control : fSystemLibraryControls) {
				control.setEnabled(enableSystemLibraryCheck);
			}
		}
	}

	class SetAllSelectionAdapter extends SelectionAdapter {
		String newValue;
		int kind;

		public SetAllSelectionAdapter(int kind, String newValue) {
			this.kind = kind;
			this.newValue = newValue;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			switch (this.kind) {
				case API_SCANNING_USAGE_PAGE_ID:
					setAllTo(this.newValue, fgAllApiScanningKeys);
					break;
				case VERSION_MANAGEMENT_PAGE_ID:
					setAllTo(this.newValue, fgAllVersionManagementKeys);
					break;
				case COMPATIBILITY_PAGE_ID:
					setAllTo(this.newValue, fgAllCompatibilityKeys);
					break;
				case API_COMPONENT_RESOLUTION_PAGE_ID:
					setAllTo(this.newValue, fgAllApiComponentResolutionKeys);
					break;
				case API_USE_SCANS_PAGE_ID:
					setAllTo(this.newValue, fgAllExternalDependenciesKeys);
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Listing of all of the {@link ExpandableComposite}s in the block
	 */
	private ArrayList<ExpandableComposite> fExpComps = new ArrayList<>();

	/**
	 * Listing of all of the {@link Combo}s added to the block
	 */
	private ArrayList<Combo> fCombos = new ArrayList<>();

	/**
	 * Map of combo and label
	 */
	private HashMap<Combo, Label> fComboLabelMap = new HashMap<>();

	/**
	 * Listing of all of the {@link Checkbox}es added to the block
	 */
	private ArrayList<Button> fCheckBoxes = new ArrayList<>();

	/**
	 * Control used inside the system library ee group
	 */
	private List<Control> fSystemLibraryControls = null;

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
	 * The main composite for the configuration block, used for
	 * enabling/disabling the block
	 */
	private Composite fMainComp = null;

	/**
	 * Stored old fProject specific settings.
	 */
	private IdentityHashMap<Key, String> fOldProjectSettings = null;

	/**
	 * Flag used to know if the page needs saving or not
	 */
	private boolean fDirty = false;

	/**
	 * counter to know how many times we have prompted users' to rebuild
	 */
	private int fRebuildcount = 0;

	/**
	 * The parent this block has been added to
	 */
	private Composite fParent = null;

	/**
	 * The tab folder for the various tabs
	 */
	private TabFolder fTabFolder = null;

	/**
	 * Constructor
	 *
	 * @param project
	 */
	public ApiErrorsWarningsConfigurationBlock(IProject project, IWorkbenchPreferenceContainer container) {
		fProject = project;
		if (fProject != null) {
			fLookupOrder = new IScopeContext[] {
					new ProjectScope(fProject), InstanceScope.INSTANCE,
					DefaultScope.INSTANCE };
		} else {
			fLookupOrder = new IScopeContext[] {
					InstanceScope.INSTANCE, DefaultScope.INSTANCE };
		}
		if (container == null) {
			fManager = new WorkingCopyManager();
		} else {
			fManager = container.getWorkingCopyManager();
		}
		if (fProject == null || hasProjectSpecificSettings(fProject)) {
			fOldProjectSettings = null;
		} else {
			fOldProjectSettings = new IdentityHashMap<>();
			for (Key key : fgAllKeys) {
				fOldProjectSettings.put(key, key.getStoredValue(fLookupOrder, false, fManager));
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
		SWTFactory.createVerticalSpacer(parent, 1);
		fMainComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		fTabFolder = new TabFolder(fMainComp, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 400;
		fTabFolder.setLayoutData(gd);

		// API scanning usage options
		createPage(API_SCANNING_USAGE_PAGE_ID, fTabFolder, PreferenceMessages.ApiToolingNotificationsBlock_0, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_3);
		// API compatibility options
		createPage(COMPATIBILITY_PAGE_ID, fTabFolder, PreferenceMessages.ApiToolingNotificationsBlock_1, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_8);
		createPage(VERSION_MANAGEMENT_PAGE_ID, fTabFolder, PreferenceMessages.ApiToolingNotificationsBlock_2, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_9);
		createPage(API_COMPONENT_RESOLUTION_PAGE_ID, fTabFolder, PreferenceMessages.ApiToolingNotificationsBlock_3, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_10);
		createPage(API_USE_SCANS_PAGE_ID, fTabFolder, PreferenceMessages.ApiToolingNotificationsBlock_4, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_11);
		restoreExpansionState();
		validateSettings(null);
		Dialog.applyDialogFont(fMainComp);
		return fMainComp;
	}

	private void initializeComboControls(Composite composite, String[] labels, Key[] keys) {
		for (int i = 0, max = labels.length; i < max; i++) {
			createComboControl(composite, labels[i], keys[i]);
		}
	}

	public int convertWidthInCharsToPixels(Font font, int chars) {
		GC gc = new GC(font.getDevice());
		gc.setFont(font);
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return Dialog.convertWidthInCharsToPixels(fontMetrics, chars);
	}

	protected Button addCheckBox(Composite parent, String label, Key key, String[] values, int indent) {
		ControlData data = new ControlData(key, values);
		Font dialogFont = JFaceResources.getDialogFont();

		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		gd.horizontalIndent = convertWidthInCharsToPixels(dialogFont, indent);

		Button checkBox = new Button(parent, SWT.CHECK);
		checkBox.setFont(dialogFont);
		checkBox.setText(label);
		checkBox.setData(data);
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(this.selectionlistener);

		String currValue = null;
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=280619
		// only look up the project and default scopes if the project
		// already contains project-specific settings
		if (hasProjectSpecificSettings(fProject)) {
			currValue = key.getStoredValue(new IScopeContext[] {
					new ProjectScope(fProject), DefaultScope.INSTANCE }, false, fManager);
		} else {
			currValue = key.getStoredValue(fLookupOrder, false, fManager);
		}
		checkBox.setSelection(data.getSelection(currValue) == 0);
		fCheckBoxes.add(checkBox);
		return checkBox;
	}

	/**
	 * Creates a tab page parented in the folder
	 *
	 * @param tabID
	 * @param folder
	 * @param name
	 * @param description
	 * @return
	 */
	private Composite createPage(int tabID, TabFolder folder, String name, String description) {
		Composite page = SWTFactory.createComposite(folder, 1, 1, GridData.FILL_BOTH);
		TabItem tab = new TabItem(folder, SWT.NONE, tabID);
		tab.setText(name);
		tab.setControl(page);

		SWTFactory.createVerticalSpacer(page, 1);

		// Add 'Achieving API Binary Compatibility' link at the top for compatibility tab
		if (tabID == COMPATIBILITY_PAGE_ID) {
			Link link = new Link(page, SWT.CENTER);
			link.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 7));
			String linkHttp = "<a href=\"https://wiki.eclipse.org/Evolving_Java-based_APIs_2\">" //$NON-NLS-1$
					+ PreferenceMessages.ApiErrorsWarningsConfigurationBlock_4 + "</a>"; //$NON-NLS-1$
			link.setText(NLS.bind(PreferenceMessages.ApiErrorsWarningsConfigurationBlock_5, linkHttp));
			link.setSize(400, 100);
			link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
				} catch (Exception ex) {
				}
			}));
		}

		SWTFactory.createWrapLabel(page, description, 1);
		SWTFactory.createVerticalSpacer(page, 1);

		switch (tabID) {
			case API_SCANNING_USAGE_PAGE_ID: {
				createApiScanningPage(page);
				break;
			}
			case VERSION_MANAGEMENT_PAGE_ID: {
				createVersionManagementPage(page);
				break;
			}
			case API_COMPONENT_RESOLUTION_PAGE_ID: {
				createApiComponentResolutionPage(page);
				break;
			}
			case COMPATIBILITY_PAGE_ID: {
				createCompatibilityPage(page);
				break;
			}
			case API_USE_SCANS_PAGE_ID: {
				createAPIUseScanPage(page);
				break;
			}
			default:
				break;
		}
		SWTFactory.createVerticalSpacer(page, 1);

		Group bcomp = SWTFactory.createGroup(page, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_setAllto, 3, 2, GridData.FILL_HORIZONTAL);
		Button button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_error_button, null, SWT.RIGHT);
		button.addSelectionListener(new SetAllSelectionAdapter(tabID, ApiPlugin.VALUE_ERROR));
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_warning_button, null, SWT.RIGHT);
		button.addSelectionListener(new SetAllSelectionAdapter(tabID, ApiPlugin.VALUE_WARNING));
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_ignore_button, null, SWT.RIGHT);
		button.addSelectionListener(new SetAllSelectionAdapter(tabID, ApiPlugin.VALUE_IGNORE));
		return page;
	}

	private void createAPIUseScanPage(Composite page) {
		Composite vcomp = SWTFactory.createComposite(page, 2, 1, GridData.FILL_BOTH);
		initializeComboControls(vcomp, new String[] {
				PreferenceMessages.ApiUseScanConfigurationBlock_unresolvedTypeProblem,
				PreferenceMessages.ApiUseScanConfigurationBlock_unresolvedMethodProblem,
				PreferenceMessages.ApiUseScanConfigurationBlock_unresolvedFieldProblem }, new Key[] {
				KEY_API_USE_SCAN_TYPE_PROBLEM, KEY_API_USE_SCAN_METHOD_PROBLEM,
				KEY_API_USE_SCAN_FIELD_PROBLEM });
	}

	private void createCompatibilityPage(Composite page) {
		// compatibility
		Composite vcomp = SWTFactory.createComposite(page, 2, 1, GridData.FILL_HORIZONTAL);
		addCheckBox(vcomp, PreferenceMessages.CompatibilityReportApiBreakageWhenMajorVersionIncremented, KEY_REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED, CHECKBOX_VALUES, 2);
		ScrolledComposite scomp = new ScrolledComposite(page, SWT.H_SCROLL | SWT.V_SCROLL);
		scomp.setExpandHorizontal(true);
		scomp.setExpandVertical(true);
		scomp.setLayout(new GridLayout(1, false));
		scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scomp.addListener(SWT.Resize, event -> handleExpand(getScrollingParent(event.widget)));
		Composite sbody = SWTFactory.createComposite(scomp, 1, 1, GridData.FILL_BOTH);
		scomp.setContent(sbody);

		Composite client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityAPIComponentElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.API_COMPONENT_REMOVED_API_TYPE,
				PreferenceMessages.API_COMPONENT_REMOVED_TYPE,
				PreferenceMessages.API_COMPONENT_REMOVED_REEXPORTED_API_TYPE,
				PreferenceMessages.API_COMPONENT_REMOVED_REEXPORTED_TYPE, }, new Key[] {
				KEY_API_COMPONENT_REMOVED_API_TYPE,
				KEY_API_COMPONENT_REMOVED_TYPE,
				KEY_API_COMPONENT_REMOVED_REEXPORTED_API_TYPE,
				KEY_API_COMPONENT_REMOVED_REEXPORTED_TYPE, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityClassElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.CLASS_ADDED_FIELD,
				PreferenceMessages.CLASS_ADDED_METHOD,
				PreferenceMessages.CLASS_ADDED_RESTRICTIONS,
				PreferenceMessages.CLASS_ADDED_TYPE_PARAMETER,
				PreferenceMessages.CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
				PreferenceMessages.CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
				PreferenceMessages.CLASS_CHANGED_NON_FINAL_TO_FINAL,
				PreferenceMessages.CLASS_CHANGED_TYPE_CONVERSION,
				PreferenceMessages.CLASS_CHANGED_DECREASE_ACCESS,
				PreferenceMessages.CLASS_REMOVED_FIELD,
				PreferenceMessages.CLASS_REMOVED_METHOD,
				PreferenceMessages.CLASS_REMOVED_CONSTRUCTOR,
				PreferenceMessages.CLASS_REMOVED_SUPERCLASS,
				PreferenceMessages.CLASS_REMOVED_TYPE_MEMBER,
				PreferenceMessages.CLASS_REMOVED_TYPE_PARAMETER, },
				new Key[] { KEY_CLASS_ADDED_FIELD,
				KEY_CLASS_ADDED_METHOD, KEY_CLASS_ADDED_RESTRICTIONS,
				KEY_CLASS_ADDED_TYPE_PARAMETER,
				KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
				KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
				KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL,
				KEY_CLASS_CHANGED_TYPE_CONVERSION,
				KEY_CLASS_CHANGED_DECREASE_ACCESS, KEY_CLASS_REMOVED_FIELD,
				KEY_CLASS_REMOVED_METHOD, KEY_CLASS_REMOVED_CONSTRUCTOR,
				KEY_CLASS_REMOVED_SUPERCLASS, KEY_CLASS_REMOVED_TYPE_MEMBER,
				KEY_CLASS_REMOVED_TYPE_PARAMETER, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityInterfaceElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.INTERFACE_ADDED_FIELD,
				PreferenceMessages.INTERFACE_ADDED_METHOD,
				PreferenceMessages.INTERFACE_ADDED_DEFAULT_METHOD,
				PreferenceMessages.INTERFACE_ADDED_RESTRICTIONS,
				PreferenceMessages.INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS,
				PreferenceMessages.INTERFACE_ADDED_TYPE_PARAMETER,
				PreferenceMessages.INTERFACE_CHANGED_TYPE_CONVERSION,
				PreferenceMessages.INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
				PreferenceMessages.INTERFACE_REMOVED_TYPE_PARAMETER,
				PreferenceMessages.INTERFACE_REMOVED_FIELD,
				PreferenceMessages.INTERFACE_REMOVED_METHOD,
				PreferenceMessages.INTERFACE_REMOVED_TYPE_MEMBER, }, new Key[] {
				KEY_INTERFACE_ADDED_FIELD, KEY_INTERFACE_ADDED_METHOD,
				KEY_INTERFACE_ADDED_DEFAULT_METHOD,
				KEY_INTERFACE_ADDED_RESTRICTIONS,
				KEY_INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS,
				KEY_INTERFACE_ADDED_TYPE_PARAMETER,
				KEY_INTERFACE_CHANGED_TYPE_CONVERSION,
				KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
				KEY_INTERFACE_REMOVED_TYPE_PARAMETER,
				KEY_INTERFACE_REMOVED_FIELD, KEY_INTERFACE_REMOVED_METHOD,
				KEY_INTERFACE_REMOVED_TYPE_MEMBER, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityEnumElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
				PreferenceMessages.ENUM_CHANGED_TYPE_CONVERSION,
				PreferenceMessages.ENUM_REMOVED_FIELD,
				PreferenceMessages.ENUM_REMOVED_ENUM_CONSTANT,
				PreferenceMessages.ENUM_REMOVED_METHOD,
				PreferenceMessages.ENUM_REMOVED_TYPE_MEMBER, }, new Key[] {
				KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
				KEY_ENUM_CHANGED_TYPE_CONVERSION, KEY_ENUM_REMOVED_FIELD,
				KEY_ENUM_REMOVED_ENUM_CONSTANT, KEY_ENUM_REMOVED_METHOD,
				KEY_ENUM_REMOVED_TYPE_MEMBER, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityAnnotationElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
				PreferenceMessages.ANNOTATION_CHANGED_TYPE_CONVERSION,
				PreferenceMessages.ANNOTATION_ADDED_FIELD,
				PreferenceMessages.ANNOTATION_REMOVED_FIELD,
				PreferenceMessages.ANNOTATION_REMOVED_METHOD,
				PreferenceMessages.ANNOTATION_REMOVED_TYPE_MEMBER, }, new Key[] {
				KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
				KEY_ANNOTATION_CHANGED_TYPE_CONVERSION,
				KEY_ANNOTATION_ADDED_FIELD,
				KEY_ANNOTATION_REMOVED_FIELD, KEY_ANNOTATION_REMOVED_METHOD,
				KEY_ANNOTATION_REMOVED_TYPE_MEMBER, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityFieldElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.FIELD_ADDED_VALUE,
				PreferenceMessages.FIELD_CHANGED_TYPE,
				PreferenceMessages.FIELD_CHANGED_VALUE,
				PreferenceMessages.FIELD_CHANGED_DECREASE_ACCESS,
				PreferenceMessages.FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
				PreferenceMessages.FIELD_CHANGED_NON_FINAL_TO_FINAL,
				PreferenceMessages.FIELD_CHANGED_STATIC_TO_NON_STATIC,
				PreferenceMessages.FIELD_CHANGED_NON_STATIC_TO_STATIC,
				PreferenceMessages.FIELD_REMOVED_VALUE,
				PreferenceMessages.FIELD_REMOVED_TYPE_ARGUMENT, }, new Key[] {
				KEY_FIELD_ADDED_VALUE, KEY_FIELD_CHANGED_TYPE,
				KEY_FIELD_CHANGED_VALUE, KEY_FIELD_CHANGED_DECREASE_ACCESS,
				KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
				KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL,
				KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC,
				KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC,
				KEY_FIELD_REMOVED_VALUE, KEY_FIELD_REMOVED_TYPE_ARGUMENT, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityMethodElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.METHOD_ADDED_RESTRICTIONS,
				PreferenceMessages.METHOD_ADDED_TYPE_PARAMETER,
				PreferenceMessages.METHOD_CHANGED_VARARGS_TO_ARRAY,
				PreferenceMessages.METHOD_CHANGED_DECREASE_ACCESS,
				PreferenceMessages.METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
				PreferenceMessages.METHOD_CHANGED_NON_STATIC_TO_STATIC,
				PreferenceMessages.METHOD_CHANGED_STATIC_TO_NON_STATIC,
				PreferenceMessages.METHOD_CHANGED_NON_FINAL_TO_FINAL,
				PreferenceMessages.METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
				PreferenceMessages.METHOD_REMOVED_TYPE_PARAMETER, }, new Key[] {
				KEY_METHOD_ADDED_RESTRICTIONS, KEY_METHOD_ADDED_TYPE_PARAMETER,
				KEY_METHOD_CHANGED_VARARGS_TO_ARRAY,
				KEY_METHOD_CHANGED_DECREASE_ACCESS,
				KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
				KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC,
				KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC,
				KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL,
				KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
				KEY_METHOD_REMOVED_TYPE_PARAMETER, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityConstructorElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.CONSTRUCTOR_ADDED_TYPE_PARAMETER,
				PreferenceMessages.CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
				PreferenceMessages.CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
				PreferenceMessages.CONSTRUCTOR_REMOVED_TYPE_PARAMETER, }, new Key[] {
				KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER,
				KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
				KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
				KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER, });
		client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityTypeParameterElement);
		initializeComboControls(client, new String[] {
				PreferenceMessages.TYPE_PARAMETER_ADDED_CLASS_BOUND,
				PreferenceMessages.TYPE_PARAMETER_ADDED_INTERFACE_BOUND,
				PreferenceMessages.TYPE_PARAMETER_CHANGED_CLASS_BOUND,
				PreferenceMessages.TYPE_PARAMETER_CHANGED_INTERFACE_BOUND,
				PreferenceMessages.TYPE_PARAMETER_REMOVED_CLASS_BOUND,
				PreferenceMessages.TYPE_PARAMETER_REMOVED_INTERFACE_BOUND, }, new Key[] {
				KEY_TYPE_PARAMETER_ADDED_CLASS_BOUND,
				KEY_TYPE_PARAMETER_ADDED_INTERFACE_BOUND,
				KEY_TYPE_PARAMETER_CHANGED_CLASS_BOUND,
				KEY_TYPE_PARAMETER_CHANGED_INTERFACE_BOUND,
				KEY_TYPE_PARAMETER_REMOVED_CLASS_BOUND,
				KEY_TYPE_PARAMETER_REMOVED_INTERFACE_BOUND, });
	}

	private void createApiComponentResolutionPage(Composite page) {
		Composite vcomp = SWTFactory.createComposite(page, 2, 1, GridData.FILL_BOTH);
		initializeComboControls(vcomp, new String[] {
				PreferenceMessages.ReportApiComponentResolutionFailure,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_unused_problem_filters, }, new Key[] {
				KEY_REPORT_RESOLUTION_ERRORS_API_COMPONENT,
				KEY_UNUSED_PROBLEM_FILTERS });
		addCheckBox(vcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_automatically_remove_problem_filters, KEY_AUTOMATICALLY_REMOVE_PROBLEM_FILTERS, CHECKBOX_VALUES, 0);
	}

	private void createVersionManagementPage(Composite page) {
		Composite vcomp = SWTFactory.createComposite(page, 2, 1, GridData.FILL_BOTH);
		initializeComboControls(vcomp, new String[] {
				PreferenceMessages.VersionManagementReportMissingSinceTag,
				PreferenceMessages.VersionManagementReportMalformedSinceTags,
				PreferenceMessages.VersionManagementReportInvalidSinceTagVersion,
				PreferenceMessages.VersionManagementReportInvalidApiComponentVersion,
				PreferenceMessages.VersionManagementReportInvalidApiComponentVersionReportMinorWithoutApiChange,
				PreferenceMessages.VersionManagementReportInvalidApiComponentVersionReportMajorWithoutBreakingChange,
				PreferenceMessages.VersionManagementReportChangedExecutionEnv,
		},
				new Key[] {
						KEY_MISSING_SINCE_TAG, KEY_MALFORMED_SINCE_TAG, KEY_INVALID_SINCE_TAG_VERSION,
						KEY_INCOMPATIBLE_API_COMPONENT_VERSION,
						KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
						KEY_INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
						KEY_CHANGED_EXECUTION_ENV, });
	}

	private void createApiScanningPage(Composite page) {
		ScrolledComposite scomp = new ScrolledComposite(page, SWT.H_SCROLL | SWT.V_SCROLL);
		scomp.setExpandHorizontal(true);
		scomp.setExpandVertical(true);
		scomp.setLayout(new GridLayout(2, false));
		scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scomp.addListener(SWT.Resize, event -> handleExpand(getScrollingParent(event.widget)));
		Composite sbody = SWTFactory.createComposite(scomp, 1, 1, GridData.FILL_BOTH);
		scomp.setContent(sbody);

		Composite client = createExpansibleComposite(sbody, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_general);
		initializeComboControls(client, new String[] {
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_invalid_tag_use,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_invalid_annotation_use,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_invalid_reference_to_system_libraries, }, new Key[] {
				KEY_INVALID_JAVADOC_TAG, KEY_INVALID_ANNOTATION,
				KEY_INVALID_REFERENCE_IN_SYSTEM_LIBRARIES });
		initializeInstalledMetatadata(client);
		initializeComboControls(client, new String[] { PreferenceMessages.ApiErrorsWarningsConfigurationBlock_3 }, new Key[] { KEY_MISSING_EE_DESCRIPTIONS });
		client = createExpansibleComposite(sbody, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_restrictions);
		initializeComboControls(client, new String[] {
				PreferenceMessages.ApiProblemSeveritiesNoImplement,
				PreferenceMessages.ApiProblemSeveritiesNoExtend,
				PreferenceMessages.ApiProblemSeveritiesNoReference,
				PreferenceMessages.ApiProblemSeveritiesNoInstanciate,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_override_tagged_method, }, new Key[] {
				KEY_NOIMPLEMENT, KEY_NOEXTEND, KEY_NOREFERENCE,
				KEY_NOINSTANTIATE, KEY_NOOVERRIDE, });
		client = createExpansibleComposite(sbody, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_leaks);
		initializeComboControls(client, new String[] {
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_extend_non_api_class,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_implement_non_api,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_field_decl_non_api,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_return_type_non_api,
				PreferenceMessages.ApiErrorsWarningsConfigurationBlock_parameter_non_api }, new Key[] {
				KEY_LEAK_EXTEND, KEY_LEAK_IMPLEMENT, KEY_LEAK_FIELD_DECL,
				KEY_LEAK_METHOD_RETURN_TYPE, KEY_LEAK_METHOD_PARAM });
	}

	/**
	 * Initializes the group of EE meta-data that is installed
	 *
	 * @param parent
	 */
	private void initializeInstalledMetatadata(final Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);
		GridData gd = (GridData) comp.getLayoutData();
		gd.horizontalIndent = 15;
		Group group = SWTFactory.createGroup(comp, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_checkable_ees, 3, 3, GridData.FILL_BOTH);
		String[] stubs = StubApiComponent.getInstalledMetadata();
		this.fSystemLibraryControls = new ArrayList<>(stubs.length + 1);
		this.fSystemLibraryControls.add(group);
		boolean installMore = (stubs.length < ProfileModifiers.getAllIds().length);
		if (stubs.length == 0) {
			SWTFactory.createVerticalSpacer(group, 1);
			this.fSystemLibraryControls.add(SWTFactory.createLabel(group, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_no_ees_installed, JFaceResources.getDialogFont(), 1));
		} else {
			for (String stub : stubs) {
				this.fSystemLibraryControls.add(SWTFactory.createLabel(group, stub, JFaceResources.getDialogFont(), 1));
			}
		}
		if (installMore) {
			ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			final Command command = commandService.getCommand(P2_INSTALL_COMMAND_HANDLER);
			if (command.isHandled()) {
				String linkedName = PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_checkable_ees_link_label;
				if (stubs.length == 0) {
					linkedName = PreferenceMessages.install_ee_descriptions;
				}
				SWTFactory.createVerticalSpacer(group, 1);
				Link link = SWTFactory.createLink(group, linkedName, JFaceResources.getDialogFont(), 3);
				link.setToolTipText(PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_checkable_ees_tooltip);
				link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
					IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
					try {
						handlerService.executeCommand(P2_INSTALL_COMMAND_HANDLER, null);
					} catch (ExecutionException ex) {
						handleCommandException();
					} catch (NotDefinedException ex) {
						handleCommandException();
					} catch (NotEnabledException ex) {
						handleCommandException();
					} catch (NotHandledException ex) {
						handleCommandException();
					}
				}));
				this.fSystemLibraryControls.add(link);
			}
		}
	}

	public static void handleCommandException() {
		MessageDialog.openError(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_checkable_ees_error_dialog_title, PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_checkable_ees_error_dialog_description);

	}

	/**
	 * Sets all values of the given set of keys to the given new value
	 *
	 * @param newValue
	 * @param keys
	 */
	void setAllTo(String newValue, Key[] keys) {
		for (Key key : keys) {
			key.setStoredValue(fLookupOrder[0], newValue, fManager);
		}
		updateControls();
		validateSettings(null);
		fDirty = true;
		fRebuildcount = 0;
	}

	/**
	 * Creates an {@link ExpandableComposite} with a client composite and a
	 * default grid layout
	 *
	 * @param parent
	 * @param title
	 * @return
	 */
	private Composite createExpansibleComposite(Composite parent, String title) {
		ExpandableComposite ecomp = SWTFactory.createExpandibleComposite(parent, title, 1, GridData.FILL_HORIZONTAL);
		ecomp.addExpansionListener(new ExpansionAdapter() {
			@Override
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
	 *
	 * @param obj
	 * @return
	 */
	ScrolledComposite getScrollingParent(Object obj) {
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
	void handleExpand(ScrolledComposite composite) {
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
				ArrayList<Key> changes = new ArrayList<>();
				collectChanges(fLookupOrder[0], changes);
				if (changes.size() > 0) {
					if (fRebuildcount < 1) {
						fRebuildcount++;
						fManager.applyChanges();
						IProject[] projects = Util.getApiProjects();
						String message = PreferenceMessages.ApiErrorsWarningsConfigurationBlock_0;
						if (fProject != null) {
							projects = new IProject[] { fProject };
							message = MessageFormat.format(PreferenceMessages.ApiErrorsWarningsConfigurationBlock_1, fProject.getName());
						}
						if (projects != null) {
							int userInput = MessageDialog.open(MessageDialog.QUESTION, fParent.getShell(),
									PreferenceMessages.ApiErrorsWarningsConfigurationBlock_2, message, SWT.NONE,
									PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_buildButtonLabel,
									PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_dontBuildButtonLabel);
							if (Window.OK == userInput) {
								Util.getBuildJob(projects).schedule();
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
		for (Key key : fgAllKeys) {
			defval = key.getStoredValue(fLookupOrder, true, fManager);
			key.setStoredValue(fLookupOrder[0], defval, fManager);
		}
		updateControls();
		validateSettings(null);
		fDirty = true;
		fRebuildcount = 0;
	}

	/**
	 * Updates all of the registered {@link Control}s on the page. Registration
	 * implies that the {@link Control} control was added to the listing of
	 * fCombos or fCheckBoxes
	 */
	private void updateControls() {
		for (int i = 0; i < fCombos.size(); i++) {
			Combo combo = fCombos.get(i);
			ControlData data = (ControlData) combo.getData();
			combo.select(data.getSelection(getValue(data.getKey())));
		}
		for (int i = fCheckBoxes.size() - 1; i >= 0; i--) {
			Button button = fCheckBoxes.get(i);
			ControlData data = (ControlData) button.getData();
			String currValue = getValue(data.getKey());
			button.setSelection(data.getSelection(currValue) == 0);
		}
	}

	/**
	 * recursive method to enable/disable all of the controls on the main page
	 *
	 * @param ctrl
	 * @param enabled
	 */
	private void enableControl(Control ctrl, boolean enabled) {
		ctrl.setEnabled(enabled);
		if (ctrl instanceof Composite) {
			Composite comp = (Composite) ctrl;
			Control[] children = comp.getChildren();
			for (Control element : children) {
				enableControl(element, enabled);
			}
		}

	}

	/**
	 * Disposes the controls from this page
	 */
	public void dispose() {
		fMainComp.getParent().dispose();
		fExpComps.clear();
		fCombos.clear();
		fCheckBoxes.clear();
	}

	/**
	 * Creates a {@link Label} | {@link Combo} control. The combo is initialised
	 * from the given {@link Key}
	 *
	 * @param parent
	 * @param label
	 * @param key
	 */
	protected void createComboControl(Composite parent, String label, Key key) {
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
		fCombos.add(combo);
		addHighlight(parent, lbl, combo);
		if (org.eclipse.jface.util.Util.isMac()) {
			fComboLabelMap.put(combo, lbl);
		}

	}

	/**
	 * Restores the expansion state of the composites in this block. If there
	 * are no settings, than the first composite is expanded by default
	 */
	private void restoreExpansionState() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS);
		if (settings != null) {
			for (int i = 0; i < fExpComps.size(); i++) {
				fExpComps.get(i).setExpanded(settings.getBoolean(Integer.toString(i)));
			}
		} else {
			fExpComps.get(0).setExpanded(true);
		}
	}

	/**
	 * Stores the expansion state of the composites
	 */
	private void persistExpansionState() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS);
		for (int i = 0; i < fExpComps.size(); i++) {
			settings.put(Integer.toString(i), fExpComps.get(i).isExpanded());
		}
	}

	/**
	 * Sets using project specific settings
	 *
	 * @param enable
	 */
	public void useProjectSpecificSettings(boolean enable) {
		boolean disabled = fOldProjectSettings == null;
		if (enable != disabled && fProject != null) {
			if (enable) {
				for (Key key : fgAllKeys) {
					key.setStoredValue(fLookupOrder[0], fOldProjectSettings.get(key), fManager);
				}
				fOldProjectSettings = null;
				updateControls();
			} else {
				fOldProjectSettings = new IdentityHashMap<>();
				String old = null;
				for (Key key : fgAllKeys) {
					old = key.getStoredValue(fLookupOrder, false, fManager);
					fOldProjectSettings.put(key, old);
					key.setStoredValue(fLookupOrder[0], null, fManager);
				}
			}
		}
		fDirty = true;
		enableControl(fMainComp, enable);
		if (enable) {
			validateSettings(null);
		}
	}

	/**
	 * returns if this block has project specific settings
	 *
	 * @param project
	 * @return true if there are project specific settings, false otherwise
	 */
	public boolean hasProjectSpecificSettings(IProject project) {
		if (project != null) {
			IScopeContext projectContext = new ProjectScope(project);
			for (Key fgAllKey : fgAllKeys) {
				if (fgAllKey.getStoredValue(projectContext, fManager) != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Collects the keys that have changed on the page into the specified list
	 *
	 * @param changes the {@link List} to collect changed keys into
	 */
	private void collectChanges(IScopeContext context, List<Key> changes) {
		boolean complete = fOldProjectSettings == null && fProject != null;
		for (Key key : fgAllKeys) {
			String origval = key.getStoredValue(context, null);
			String newval = key.getStoredValue(context, fManager);
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

	protected Button getCheckBox(Key key) {
		for (int i = fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr = fCheckBoxes.get(i);
			ControlData data = (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;
	}

	/**
	 * If a valid tab ID is provided, select that tab in the tab folder.
	 *
	 * @param tabID integer tab id, one of
	 *            {@link ApiErrorsWarningsConfigurationBlock#API_USE_SCANS_PAGE_ID}
	 *            ,
	 *            {@link ApiErrorsWarningsConfigurationBlock#COMPATIBILITY_PAGE_ID}
	 *            ,
	 *            {@link ApiErrorsWarningsConfigurationBlock#VERSION_MANAGEMENT_PAGE_ID}
	 *            ,
	 *            {@link ApiErrorsWarningsConfigurationBlock#API_COMPONENT_RESOLUTION_PAGE_ID}
	 *            and
	 *            {@link ApiErrorsWarningsConfigurationBlock#API_USE_SCANS_PAGE_ID}
	 */
	public void selectTab(int tabID) {
		if (tabID >= 0 && tabID < fTabFolder.getItemCount()) {
			fTabFolder.setSelection(tabID);
		}
	}

	public void selectOption(String key, String qualifier) {
		Key[] allKeys = getAllKeys();
		for (int i = 0; i < allKeys.length; i++) {
			Key curr = allKeys[i];
			if (curr.getName().equals(key) && curr.getQualifier().equals(qualifier)) {
				selectOption(curr);
			}
		}

	}

	protected ExpandableComposite getParentExpandableComposite(Control control) {
		Control parent = control.getParent();
		while (!(parent instanceof ExpandableComposite) && parent != null) {
			parent = parent.getParent();
		}
		if (parent instanceof ExpandableComposite) {
			return (ExpandableComposite) parent;
		}
		return null;
	}
	public void selectOption(Key key) {
		Control control = findControl(key);
		if (control != null) {
			if (!fExpComps.isEmpty()) {
				ExpandableComposite expandable = getParentExpandableComposite(control);
				if (expandable != null) {
					for (int i = 0; i < fExpComps.size(); i++) {
						ExpandableComposite curr = fExpComps.get(i);
						curr.setExpanded(curr == expandable);
					}
				}
			}
			control.setFocus();
			if (org.eclipse.jface.util.Util.isMac()) {
				Label labelControl = fComboLabelMap.get(control);
				if (labelControl != null && control instanceof Combo) {
					highlight(control.getParent(), labelControl, (Combo) control, ConfigurationBlock.HIGHLIGHT_FOCUS);
				}
			}
		}
	}

	protected Combo getComboBox(Key key) {
		for (int i = fCombos.size() - 1; i >= 0; i--) {
			Combo curr = fCombos.get(i);
			ControlData data = (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;
	}
	protected Control findControl(Key key) {
		Combo comboBox= getComboBox(key);
		if (comboBox != null) {
			return comboBox;
		}
		return null;
	}

}
