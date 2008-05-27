/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

import com.ibm.icu.text.MessageFormat;


/**
 * This block is used to add the Api tooling notification settings UI
 * to a parent control
 * 
 * @since 1.0.0
 */
public class ApiErrorsWarningsConfigurationBlock {
	
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
			int index= selection ? 0 : 1;
			return values[index];
		}
		
		public String getValue(int index) {
			return values[index];
		}		
		
		public int getSelection(String value) {
			if (value != null) {
				for (int i= 0; i < values.length; i++) {
					if (value.equals(values[i])) {
						return i;
					}
				}
			}
			return values.length -1; // assume the last option is the least severe
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
			this.qualifier= qualifier;
			this.key= key;
		}
		
		/**
		 * Returns the {@link IEclipsePreferences} node for the given context and {@link IWorkingCopyManager}
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
		 * Returns the value stored in the {@link IEclipsePreferences} node from the given context and working copy manager
		 * @param context
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or <code>null</code>
		 */
		public String getStoredValue(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if(node != null) {
				return node.get(key, null);
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
				node.put(key, value);
			} else {
				node.remove(key);
			}
		}
			
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return qualifier + '/' + key;
		}
	}

	/**
	 * Returns a new {@link Key} for the {@link ApiUIPlugin} preference store
	 * @param key
	 * @return the new {@link Key} for the {@link ApiUIPlugin} preference store
	 */
	protected final static Key getApiToolsKey(String key) {
		return new Key(ApiPlugin.getPluginIdentifier(), key);
	}
	//Restriction modifier keys
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
	
	//compatibility keys
	private static final Key KEY_API_COMPONENT_REMOVED_API_TYPE =
		getApiToolsKey(IApiProblemTypes.API_COMPONENT_REMOVED_API_TYPE);
	private static final Key KEY_API_COMPONENT_REMOVED_TYPE =
		getApiToolsKey(IApiProblemTypes.API_COMPONENT_REMOVED_TYPE);
	private static final Key KEY_ANNOTATION_ADDED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_CLASS_BOUND);
	private static final Key KEY_ANNOTATION_ADDED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_INTERFACE_BOUND);
	private static final Key KEY_ANNOTATION_ADDED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_INTERFACE_BOUNDS);
	private static final Key KEY_ANNOTATION_ADDED_FIELD =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_FIELD);
	private static final Key KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE);
	private static final Key KEY_ANNOTATION_ADDED_METHOD =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_METHOD);
	private static final Key KEY_ANNOTATION_ADDED_TYPE_MEMBER =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_TYPE_MEMBER);
	private static final Key KEY_ANNOTATION_ADDED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_ADDED_TYPE_PARAMETER);
	private static final Key KEY_ANNOTATION_REMOVED_FIELD =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_FIELD);
	private static final Key KEY_ANNOTATION_REMOVED_METHOD_DEFAULT_VALUE =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_METHOD_DEFAULT_VALUE);
	private static final Key KEY_ANNOTATION_REMOVED_METHOD_NO_DEFAULT_VALUE =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_METHOD_NO_DEFAULT_VALUE);
	private static final Key KEY_ANNOTATION_REMOVED_TYPE_MEMBER =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_TYPE_MEMBER);
	private static final Key KEY_ANNOTATION_REMOVED_TYPE_PARAMETERS =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_TYPE_PARAMETERS);
	private static final Key KEY_ANNOTATION_REMOVED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_TYPE_PARAMETER);
	private static final Key KEY_ANNOTATION_REMOVED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_CLASS_BOUND);
	private static final Key KEY_ANNOTATION_REMOVED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_INTERFACE_BOUND);
	private static final Key KEY_ANNOTATION_REMOVED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_REMOVED_FIELD);
	private static final Key KEY_ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
	private static final Key KEY_ANNOTATION_CHANGED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_INTERFACE_BOUNDS);
	private static final Key KEY_ANNOTATION_CHANGED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_CLASS_BOUND);
	private static final Key KEY_ANNOTATION_CHANGED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_INTERFACE_BOUND);
	private static final Key KEY_ANNOTATION_CHANGED_TO_CLASS =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_TO_CLASS);
	private static final Key KEY_ANNOTATION_CHANGED_TO_ENUM =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_TO_ENUM);
	private static final Key KEY_ANNOTATION_CHANGED_TO_INTERFACE =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_TO_INTERFACE);
	private static final Key KEY_ANNOTATION_CHANGED_RESTRICTIONS =
		getApiToolsKey(IApiProblemTypes.ANNOTATION_CHANGED_RESTRICTIONS);

	// interface key constant
	private static final Key KEY_INTERFACE_ADDED_FIELD =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_FIELD);
	private static final Key KEY_INTERFACE_ADDED_METHOD =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_METHOD);
	private static final Key KEY_INTERFACE_ADDED_TYPE_MEMBER =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_TYPE_MEMBER);
	private static final Key KEY_INTERFACE_ADDED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_CLASS_BOUND);
	private static final Key KEY_INTERFACE_ADDED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_INTERFACE_BOUND);
	private static final Key KEY_INTERFACE_ADDED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_INTERFACE_BOUNDS);
	private static final Key KEY_INTERFACE_ADDED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.INTERFACE_ADDED_TYPE_PARAMETER);
	private static final Key KEY_INTERFACE_REMOVED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_TYPE_PARAMETER);
	private static final Key KEY_INTERFACE_REMOVED_TYPE_PARAMETERS =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_TYPE_PARAMETERS);
	private static final Key KEY_INTERFACE_REMOVED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_CLASS_BOUND);
	private static final Key KEY_INTERFACE_REMOVED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_INTERFACE_BOUND);
	private static final Key KEY_INTERFACE_REMOVED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_INTERFACE_BOUNDS);
	private static final Key KEY_INTERFACE_REMOVED_FIELD =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_FIELD);
	private static final Key KEY_INTERFACE_REMOVED_METHOD =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_METHOD);
	private static final Key KEY_INTERFACE_REMOVED_TYPE_MEMBER =
		getApiToolsKey(IApiProblemTypes.INTERFACE_REMOVED_TYPE_MEMBER);
	private static final Key KEY_INTERFACE_CHANGED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_CLASS_BOUND);
	private static final Key KEY_INTERFACE_CHANGED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_INTERFACE_BOUND);
	private static final Key KEY_INTERFACE_CHANGED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_INTERFACE_BOUNDS);
	private static final Key KEY_INTERFACE_CHANGED_TO_CLASS =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_TO_CLASS);
	private static final Key KEY_INTERFACE_CHANGED_TO_ENUM =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_TO_ENUM);
	private static final Key KEY_INTERFACE_CHANGED_TO_ANNOTATION =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_TO_ANNOTATION);
	private static final Key KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
	private static final Key KEY_INTERFACE_CHANGED_RESTRICTIONS =
		getApiToolsKey(IApiProblemTypes.INTERFACE_CHANGED_RESTRICTIONS);

	// enum key constant
	private static final Key KEY_ENUM_ADDED_FIELD =
		getApiToolsKey(IApiProblemTypes.ENUM_ADDED_FIELD);
	private static final Key KEY_ENUM_ADDED_METHOD =
		getApiToolsKey(IApiProblemTypes.ENUM_ADDED_METHOD);
	private static final Key KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET =
		getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
	private static final Key KEY_ENUM_CHANGED_TO_ANNOTATION =
		getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_TO_ANNOTATION);
	private static final Key KEY_ENUM_CHANGED_TO_CLASS =
		getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_TO_CLASS);
	private static final Key KEY_ENUM_CHANGED_TO_INTERFACE =
		getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_TO_INTERFACE);
	private static final Key KEY_ENUM_REMOVED_FIELD =
		getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_FIELD);
	private static final Key KEY_ENUM_REMOVED_ENUM_CONSTANT =
		getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_ENUM_CONSTANT);
	private static final Key KEY_ENUM_REMOVED_METHOD =
		getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_METHOD);
	private static final Key KEY_ENUM_REMOVED_CONSTRUCTOR =
		getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_CONSTRUCTOR);
	private static final Key KEY_ENUM_REMOVED_TYPE_MEMBER =
		getApiToolsKey(IApiProblemTypes.ENUM_REMOVED_TYPE_MEMBER);
	private static final Key KEY_ENUM_CHANGED_RESTRICTIONS =
		getApiToolsKey(IApiProblemTypes.ENUM_CHANGED_RESTRICTIONS);

	// class key constant
	private static final Key KEY_CLASS_ADDED_FIELD =
		getApiToolsKey(IApiProblemTypes.CLASS_ADDED_FIELD);
	private static final Key KEY_CLASS_ADDED_METHOD =
		getApiToolsKey(IApiProblemTypes.CLASS_ADDED_METHOD);
	private static final Key KEY_CLASS_ADDED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.CLASS_ADDED_TYPE_PARAMETER);
	private static final Key KEY_CLASS_ADDED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.CLASS_ADDED_CLASS_BOUND);
	private static final Key KEY_CLASS_ADDED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.CLASS_ADDED_INTERFACE_BOUND);
	private static final Key KEY_CLASS_ADDED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.CLASS_ADDED_INTERFACE_BOUNDS);
	private static final Key KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
	private static final Key KEY_CLASS_CHANGED_CONTRACTED_SUPERCLASS_SET =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_CONTRACTED_SUPERCLASS_SET);
	private static final Key KEY_CLASS_CHANGED_SUPERCLASS =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_SUPERCLASS);
	private static final Key KEY_CLASS_CHANGED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_CLASS_BOUND);
	private static final Key KEY_CLASS_CHANGED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_INTERFACE_BOUND);
	private static final Key KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT);
	private static final Key KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_CLASS_CHANGED_TO_ANNOTATION =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_TO_ANNOTATION);
	private static final Key KEY_CLASS_CHANGED_TO_ENUM =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_TO_ENUM);
	private static final Key KEY_CLASS_CHANGED_TO_INTERFACE =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_TO_INTERFACE);
	private static final Key KEY_CLASS_CHANGED_DECREASE_ACCESS =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_CLASS_REMOVED_FIELD =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_FIELD);
	private static final Key KEY_CLASS_REMOVED_METHOD =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_METHOD);
	private static final Key KEY_CLASS_REMOVED_CONSTRUCTOR =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_CONSTRUCTOR);
	private static final Key KEY_CLASS_REMOVED_TYPE_MEMBER =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_TYPE_MEMBER);
	private static final Key KEY_CLASS_REMOVED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_TYPE_PARAMETER);
	private static final Key KEY_CLASS_REMOVED_TYPE_PARAMETERS =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_TYPE_PARAMETERS);
	private static final Key KEY_CLASS_REMOVED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_CLASS_BOUND);
	private static final Key KEY_CLASS_REMOVED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_INTERFACE_BOUND);
	private static final Key KEY_CLASS_REMOVED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.CLASS_REMOVED_INTERFACE_BOUNDS);
	private static final Key KEY_CLASS_CHANGED_RESTRICTIONS =
		getApiToolsKey(IApiProblemTypes.CLASS_CHANGED_RESTRICTIONS);

	// field key constant
	private static final Key KEY_FIELD_ADDED_VALUE =
		getApiToolsKey(IApiProblemTypes.FIELD_ADDED_VALUE);
	private static final Key KEY_FIELD_CHANGED_TYPE =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_TYPE);
	private static final Key KEY_FIELD_CHANGED_VALUE =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_VALUE);
	private static final Key KEY_FIELD_CHANGED_DECREASE_ACCESS =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT);
	private static final Key KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_STATIC_TO_NON_STATIC);
	private static final Key KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC =
		getApiToolsKey(IApiProblemTypes.FIELD_CHANGED_NON_STATIC_TO_STATIC);
	private static final Key KEY_FIELD_REMOVED_VALUE =
		getApiToolsKey(IApiProblemTypes.FIELD_REMOVED_VALUE);
	private static final Key KEY_FIELD_REMOVED_TYPE_ARGUMENTS =
		getApiToolsKey(IApiProblemTypes.FIELD_REMOVED_TYPE_ARGUMENTS);

	// method key constant
	private static final Key KEY_METHOD_ADDED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.METHOD_ADDED_CLASS_BOUND);
	private static final Key KEY_METHOD_ADDED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.METHOD_ADDED_INTERFACE_BOUND);
	private static final Key KEY_METHOD_ADDED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.METHOD_ADDED_INTERFACE_BOUNDS);
	private static final Key KEY_METHOD_ADDED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.METHOD_ADDED_TYPE_PARAMETER);
	private static final Key KEY_METHOD_CHANGED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_CLASS_BOUND);
	private static final Key KEY_METHOD_CHANGED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_INTERFACE_BOUND);
	private static final Key KEY_METHOD_CHANGED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_TYPE_PARAMETER);
	private static final Key KEY_METHOD_CHANGED_VARARGS_TO_ARRAY =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_VARARGS_TO_ARRAY);
	private static final Key KEY_METHOD_CHANGED_DECREASE_ACCESS =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT);
	private static final Key KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_NON_STATIC_TO_STATIC);
	private static final Key KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_STATIC_TO_NON_STATIC);
	private static final Key KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL =
		getApiToolsKey(IApiProblemTypes.METHOD_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE =
		getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE);
	private static final Key KEY_METHOD_REMOVED_TYPE_PARAMETERS =
		getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_TYPE_PARAMETERS);
	private static final Key KEY_METHOD_REMOVED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_TYPE_PARAMETER);
	private static final Key KEY_METHOD_REMOVED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_CLASS_BOUND);
	private static final Key KEY_METHOD_REMOVED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_INTERFACE_BOUND);
	private static final Key KEY_METHOD_REMOVED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.METHOD_REMOVED_INTERFACE_BOUNDS);

	// constructor key constant
	private static final Key KEY_CONSTRUCTOR_ADDED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_ADDED_CLASS_BOUND);
	private static final Key KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_ADDED_INTERFACE_BOUND);
	private static final Key KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_ADDED_INTERFACE_BOUNDS);
	private static final Key KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_ADDED_TYPE_PARAMETER);
	private static final Key KEY_CONSTRUCTOR_CHANGED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_CLASS_BOUND);
	private static final Key KEY_CONSTRUCTOR_CHANGED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_INTERFACE_BOUND);
	private static final Key KEY_CONSTRUCTOR_CHANGED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_TYPE_PARAMETER);
	private static final Key KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY);
	private static final Key KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_DECREASE_ACCESS);
	private static final Key KEY_CONSTRUCTOR_CHANGED_NON_ABSTRACT_TO_ABSTRACT =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_NON_ABSTRACT_TO_ABSTRACT);
	private static final Key KEY_CONSTRUCTOR_CHANGED_NON_STATIC_TO_STATIC =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_NON_STATIC_TO_STATIC);
	private static final Key KEY_CONSTRUCTOR_CHANGED_STATIC_TO_NON_STATIC =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_STATIC_TO_NON_STATIC);
	private static final Key KEY_CONSTRUCTOR_CHANGED_NON_FINAL_TO_FINAL=
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_CHANGED_NON_FINAL_TO_FINAL);
	private static final Key KEY_CONSTRUCTOR_REMOVED_ANNOTATION_DEFAULT_VALUE =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_ANNOTATION_DEFAULT_VALUE);
	private static final Key KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETERS =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_TYPE_PARAMETERS);
	private static final Key KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_TYPE_PARAMETER);
	private static final Key KEY_CONSTRUCTOR_REMOVED_CLASS_BOUND =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_CLASS_BOUND);
	private static final Key KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUND =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_INTERFACE_BOUND);
	private static final Key KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUNDS =
		getApiToolsKey(IApiProblemTypes.CONSTRUCTOR_REMOVED_INTERFACE_BOUNDS);

	// version management keys
	private static final Key KEY_MISSING_SINCE_TAG =
		getApiToolsKey(IApiProblemTypes.MISSING_SINCE_TAG);
	private static final Key KEY_MALFORMED_SINCE_TAG =
		getApiToolsKey(IApiProblemTypes.MALFORMED_SINCE_TAG);
	private static final Key KEY_INVALID_SINCE_TAG_VERSION =
		getApiToolsKey(IApiProblemTypes.INVALID_SINCE_TAG_VERSION);
	private static final Key KEY_INCOMPATIBLE_API_COMPONENT_VERSION =
		getApiToolsKey(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION);
	private static final Key KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE =
		getApiToolsKey(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE);
	private static final Key KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE =
		getApiToolsKey(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE);

	private final int API_SCANNING_USAGE_PAGE_ID = 0;
	private final int COMPATIBILITY_PAGE_ID = 1;
	private final int VERSION_MANAGEMENT_PAGE_ID = 2;
	
	private static Key[] fgAllCompatibilityKeys = {
		KEY_API_COMPONENT_REMOVED_API_TYPE,
		KEY_API_COMPONENT_REMOVED_TYPE,
		KEY_ANNOTATION_ADDED_FIELD,
		KEY_ANNOTATION_ADDED_METHOD,
		KEY_ANNOTATION_ADDED_TYPE_MEMBER,
		KEY_ANNOTATION_REMOVED_FIELD,
		KEY_ANNOTATION_REMOVED_METHOD_DEFAULT_VALUE,
		KEY_ANNOTATION_REMOVED_METHOD_NO_DEFAULT_VALUE,
		KEY_ANNOTATION_REMOVED_TYPE_MEMBER,
		KEY_ANNOTATION_REMOVED_TYPE_PARAMETERS,
		KEY_ANNOTATION_REMOVED_TYPE_PARAMETER,
		KEY_ANNOTATION_REMOVED_CLASS_BOUND,
		KEY_ANNOTATION_REMOVED_INTERFACE_BOUND,
		KEY_ANNOTATION_REMOVED_INTERFACE_BOUNDS,
		KEY_ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_ANNOTATION_CHANGED_INTERFACE_BOUNDS,
		KEY_ANNOTATION_CHANGED_CLASS_BOUND,
		KEY_ANNOTATION_CHANGED_INTERFACE_BOUND,
		KEY_ANNOTATION_CHANGED_TO_CLASS,
		KEY_ANNOTATION_CHANGED_TO_ENUM,
		KEY_ANNOTATION_CHANGED_TO_INTERFACE,
		KEY_ANNOTATION_CHANGED_RESTRICTIONS,
		KEY_ANNOTATION_ADDED_TYPE_PARAMETER,
		KEY_ANNOTATION_ADDED_CLASS_BOUND,
		KEY_ANNOTATION_ADDED_INTERFACE_BOUND,
		KEY_ANNOTATION_ADDED_INTERFACE_BOUNDS,
		KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
		KEY_INTERFACE_ADDED_FIELD,
		KEY_INTERFACE_ADDED_METHOD,
		KEY_INTERFACE_ADDED_TYPE_MEMBER,
		KEY_INTERFACE_ADDED_CLASS_BOUND,
		KEY_INTERFACE_ADDED_INTERFACE_BOUND,
		KEY_INTERFACE_ADDED_INTERFACE_BOUNDS,
		KEY_INTERFACE_ADDED_TYPE_PARAMETER,
		KEY_INTERFACE_REMOVED_TYPE_PARAMETER,
		KEY_INTERFACE_REMOVED_TYPE_PARAMETERS,
		KEY_INTERFACE_REMOVED_CLASS_BOUND,
		KEY_INTERFACE_REMOVED_INTERFACE_BOUND,
		KEY_INTERFACE_REMOVED_INTERFACE_BOUNDS,
		KEY_INTERFACE_REMOVED_FIELD,
		KEY_INTERFACE_REMOVED_METHOD,
		KEY_INTERFACE_REMOVED_TYPE_MEMBER,
		KEY_INTERFACE_CHANGED_CLASS_BOUND,
		KEY_INTERFACE_CHANGED_INTERFACE_BOUND,
		KEY_INTERFACE_CHANGED_INTERFACE_BOUNDS,
		KEY_INTERFACE_CHANGED_TO_CLASS,
		KEY_INTERFACE_CHANGED_TO_ENUM,
		KEY_INTERFACE_CHANGED_TO_ANNOTATION,
		KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_INTERFACE_CHANGED_RESTRICTIONS,
		KEY_ENUM_ADDED_FIELD,
		KEY_ENUM_ADDED_METHOD,
		KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_ENUM_CHANGED_TO_ANNOTATION,
		KEY_ENUM_CHANGED_TO_CLASS,
		KEY_ENUM_CHANGED_TO_INTERFACE,
		KEY_ENUM_CHANGED_RESTRICTIONS,
		KEY_ENUM_REMOVED_FIELD,
		KEY_ENUM_REMOVED_ENUM_CONSTANT,
		KEY_ENUM_REMOVED_METHOD,
		KEY_ENUM_REMOVED_CONSTRUCTOR,
		KEY_ENUM_REMOVED_TYPE_MEMBER,
		KEY_CLASS_ADDED_FIELD,
		KEY_CLASS_ADDED_METHOD,
		KEY_CLASS_ADDED_TYPE_PARAMETER,
		KEY_CLASS_ADDED_CLASS_BOUND,
		KEY_CLASS_ADDED_INTERFACE_BOUND,
		KEY_CLASS_ADDED_INTERFACE_BOUNDS,
		KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_CLASS_CHANGED_CONTRACTED_SUPERCLASS_SET,
		KEY_CLASS_CHANGED_SUPERCLASS,
		KEY_CLASS_CHANGED_CLASS_BOUND,
		KEY_CLASS_CHANGED_INTERFACE_BOUND,
		KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL,
		KEY_CLASS_CHANGED_TO_ANNOTATION,
		KEY_CLASS_CHANGED_TO_ENUM,
		KEY_CLASS_CHANGED_TO_INTERFACE,
		KEY_CLASS_CHANGED_DECREASE_ACCESS,
		KEY_CLASS_CHANGED_RESTRICTIONS,
		KEY_CLASS_REMOVED_FIELD,
		KEY_CLASS_REMOVED_METHOD,
		KEY_CLASS_REMOVED_CONSTRUCTOR,
		KEY_CLASS_REMOVED_TYPE_MEMBER,
		KEY_CLASS_REMOVED_TYPE_PARAMETER,
		KEY_CLASS_REMOVED_TYPE_PARAMETERS,
		KEY_CLASS_REMOVED_CLASS_BOUND,
		KEY_CLASS_REMOVED_INTERFACE_BOUND,
		KEY_CLASS_REMOVED_INTERFACE_BOUNDS,
		KEY_FIELD_ADDED_VALUE,
		KEY_FIELD_CHANGED_TYPE,
		KEY_FIELD_CHANGED_VALUE,
		KEY_FIELD_CHANGED_DECREASE_ACCESS,
		KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
		KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL,
		KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC,
		KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC,
		KEY_FIELD_REMOVED_VALUE,
		KEY_FIELD_REMOVED_TYPE_ARGUMENTS,
		KEY_METHOD_ADDED_CLASS_BOUND,
		KEY_METHOD_ADDED_INTERFACE_BOUND,
		KEY_METHOD_ADDED_INTERFACE_BOUNDS,
		KEY_METHOD_ADDED_TYPE_PARAMETER,
		KEY_METHOD_CHANGED_CLASS_BOUND,
		KEY_METHOD_CHANGED_INTERFACE_BOUND,
		KEY_METHOD_CHANGED_TYPE_PARAMETER,
		KEY_METHOD_CHANGED_VARARGS_TO_ARRAY,
		KEY_METHOD_CHANGED_DECREASE_ACCESS,
		KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC,
		KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC,
		KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL,
		KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
		KEY_METHOD_REMOVED_TYPE_PARAMETERS,
		KEY_METHOD_REMOVED_TYPE_PARAMETER,
		KEY_METHOD_REMOVED_CLASS_BOUND,
		KEY_METHOD_REMOVED_INTERFACE_BOUND,
		KEY_METHOD_REMOVED_INTERFACE_BOUNDS,
		KEY_CONSTRUCTOR_ADDED_CLASS_BOUND,
		KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUND,
		KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUNDS,
		KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER,
		KEY_CONSTRUCTOR_CHANGED_CLASS_BOUND,
		KEY_CONSTRUCTOR_CHANGED_INTERFACE_BOUND,
		KEY_CONSTRUCTOR_CHANGED_TYPE_PARAMETER,
		KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
		KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
		KEY_CONSTRUCTOR_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		KEY_CONSTRUCTOR_CHANGED_NON_STATIC_TO_STATIC,
		KEY_CONSTRUCTOR_CHANGED_STATIC_TO_NON_STATIC,
		KEY_CONSTRUCTOR_CHANGED_NON_FINAL_TO_FINAL,
		KEY_CONSTRUCTOR_REMOVED_ANNOTATION_DEFAULT_VALUE,
		KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETERS,
		KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
		KEY_CONSTRUCTOR_REMOVED_CLASS_BOUND,
		KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUND,
		KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUNDS,
	};

	private static Key[] fgAllApiScanningKeys = {
		KEY_NOIMPLEMENT,
		KEY_NOEXTEND,
		KEY_NOREFERENCE,
		KEY_NOINSTANTIATE,
		KEY_NOOVERRIDE,
		KEY_LEAK_EXTEND,
		KEY_LEAK_FIELD_DECL,
		KEY_LEAK_IMPLEMENT,
		KEY_LEAK_METHOD_PARAM,
		KEY_LEAK_METHOD_RETURN_TYPE,
		KEY_INVALID_JAVADOC_TAG
	};

	private static Key[] fgAllVersionManagementKeys = {
		KEY_MISSING_SINCE_TAG,
		KEY_MALFORMED_SINCE_TAG,
		KEY_INVALID_SINCE_TAG_VERSION,
		KEY_INCOMPATIBLE_API_COMPONENT_VERSION
	};

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
		KEY_API_COMPONENT_REMOVED_API_TYPE,
		KEY_API_COMPONENT_REMOVED_TYPE,
		KEY_ANNOTATION_ADDED_FIELD,
		KEY_ANNOTATION_ADDED_METHOD,
		KEY_ANNOTATION_ADDED_TYPE_MEMBER,
		KEY_ANNOTATION_REMOVED_FIELD,
		KEY_ANNOTATION_REMOVED_METHOD_DEFAULT_VALUE,
		KEY_ANNOTATION_REMOVED_METHOD_NO_DEFAULT_VALUE,
		KEY_ANNOTATION_REMOVED_TYPE_MEMBER,
		KEY_ANNOTATION_REMOVED_TYPE_PARAMETERS,
		KEY_ANNOTATION_REMOVED_TYPE_PARAMETER,
		KEY_ANNOTATION_REMOVED_CLASS_BOUND,
		KEY_ANNOTATION_REMOVED_INTERFACE_BOUND,
		KEY_ANNOTATION_REMOVED_INTERFACE_BOUNDS,
		KEY_ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_ANNOTATION_CHANGED_INTERFACE_BOUNDS,
		KEY_ANNOTATION_CHANGED_CLASS_BOUND,
		KEY_ANNOTATION_CHANGED_INTERFACE_BOUND,
		KEY_ANNOTATION_CHANGED_TO_CLASS,
		KEY_ANNOTATION_CHANGED_TO_ENUM,
		KEY_ANNOTATION_CHANGED_TO_INTERFACE,
		KEY_ANNOTATION_CHANGED_RESTRICTIONS,
		KEY_ANNOTATION_ADDED_TYPE_PARAMETER,
		KEY_ANNOTATION_ADDED_CLASS_BOUND,
		KEY_ANNOTATION_ADDED_INTERFACE_BOUND,
		KEY_ANNOTATION_ADDED_INTERFACE_BOUNDS,
		KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
		KEY_INTERFACE_ADDED_FIELD,
		KEY_INTERFACE_ADDED_METHOD,
		KEY_INTERFACE_ADDED_TYPE_MEMBER,
		KEY_INTERFACE_ADDED_CLASS_BOUND,
		KEY_INTERFACE_ADDED_INTERFACE_BOUND,
		KEY_INTERFACE_ADDED_INTERFACE_BOUNDS,
		KEY_INTERFACE_ADDED_TYPE_PARAMETER,
		KEY_INTERFACE_REMOVED_TYPE_PARAMETER,
		KEY_INTERFACE_REMOVED_TYPE_PARAMETERS,
		KEY_INTERFACE_REMOVED_CLASS_BOUND,
		KEY_INTERFACE_REMOVED_INTERFACE_BOUND,
		KEY_INTERFACE_REMOVED_INTERFACE_BOUNDS,
		KEY_INTERFACE_REMOVED_FIELD,
		KEY_INTERFACE_REMOVED_METHOD,
		KEY_INTERFACE_REMOVED_TYPE_MEMBER,
		KEY_INTERFACE_CHANGED_CLASS_BOUND,
		KEY_INTERFACE_CHANGED_INTERFACE_BOUND,
		KEY_INTERFACE_CHANGED_INTERFACE_BOUNDS,
		KEY_INTERFACE_CHANGED_TO_CLASS,
		KEY_INTERFACE_CHANGED_TO_ENUM,
		KEY_INTERFACE_CHANGED_TO_ANNOTATION,
		KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_INTERFACE_CHANGED_RESTRICTIONS,
		KEY_ENUM_ADDED_FIELD,
		KEY_ENUM_ADDED_METHOD,
		KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_ENUM_CHANGED_TO_ANNOTATION,
		KEY_ENUM_CHANGED_TO_CLASS,
		KEY_ENUM_CHANGED_TO_INTERFACE,
		KEY_ENUM_CHANGED_RESTRICTIONS,
		KEY_ENUM_REMOVED_FIELD,
		KEY_ENUM_REMOVED_ENUM_CONSTANT,
		KEY_ENUM_REMOVED_METHOD,
		KEY_ENUM_REMOVED_CONSTRUCTOR,
		KEY_ENUM_REMOVED_TYPE_MEMBER,
		KEY_CLASS_ADDED_FIELD,
		KEY_CLASS_ADDED_METHOD,
		KEY_CLASS_ADDED_TYPE_PARAMETER,
		KEY_CLASS_ADDED_CLASS_BOUND,
		KEY_CLASS_ADDED_INTERFACE_BOUND,
		KEY_CLASS_ADDED_INTERFACE_BOUNDS,
		KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		KEY_CLASS_CHANGED_CONTRACTED_SUPERCLASS_SET,
		KEY_CLASS_CHANGED_SUPERCLASS,
		KEY_CLASS_CHANGED_CLASS_BOUND,
		KEY_CLASS_CHANGED_INTERFACE_BOUND,
		KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL,
		KEY_CLASS_CHANGED_TO_ANNOTATION,
		KEY_CLASS_CHANGED_TO_ENUM,
		KEY_CLASS_CHANGED_TO_INTERFACE,
		KEY_CLASS_CHANGED_DECREASE_ACCESS,
		KEY_CLASS_CHANGED_RESTRICTIONS,
		KEY_CLASS_REMOVED_FIELD,
		KEY_CLASS_REMOVED_METHOD,
		KEY_CLASS_REMOVED_CONSTRUCTOR,
		KEY_CLASS_REMOVED_TYPE_MEMBER,
		KEY_CLASS_REMOVED_TYPE_PARAMETER,
		KEY_CLASS_REMOVED_TYPE_PARAMETERS,
		KEY_CLASS_REMOVED_CLASS_BOUND,
		KEY_CLASS_REMOVED_INTERFACE_BOUND,
		KEY_CLASS_REMOVED_INTERFACE_BOUNDS,
		KEY_FIELD_ADDED_VALUE,
		KEY_FIELD_CHANGED_TYPE,
		KEY_FIELD_CHANGED_VALUE,
		KEY_FIELD_CHANGED_DECREASE_ACCESS,
		KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
		KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL,
		KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC,
		KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC,
		KEY_FIELD_REMOVED_VALUE,
		KEY_FIELD_REMOVED_TYPE_ARGUMENTS,
		KEY_METHOD_ADDED_CLASS_BOUND,
		KEY_METHOD_ADDED_INTERFACE_BOUND,
		KEY_METHOD_ADDED_INTERFACE_BOUNDS,
		KEY_METHOD_ADDED_TYPE_PARAMETER,
		KEY_METHOD_CHANGED_CLASS_BOUND,
		KEY_METHOD_CHANGED_INTERFACE_BOUND,
		KEY_METHOD_CHANGED_TYPE_PARAMETER,
		KEY_METHOD_CHANGED_VARARGS_TO_ARRAY,
		KEY_METHOD_CHANGED_DECREASE_ACCESS,
		KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC,
		KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC,
		KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL,
		KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
		KEY_METHOD_REMOVED_TYPE_PARAMETERS,
		KEY_METHOD_REMOVED_TYPE_PARAMETER,
		KEY_METHOD_REMOVED_CLASS_BOUND,
		KEY_METHOD_REMOVED_INTERFACE_BOUND,
		KEY_METHOD_REMOVED_INTERFACE_BOUNDS,
		KEY_CONSTRUCTOR_ADDED_CLASS_BOUND,
		KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUND,
		KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUNDS,
		KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER,
		KEY_CONSTRUCTOR_CHANGED_CLASS_BOUND,
		KEY_CONSTRUCTOR_CHANGED_INTERFACE_BOUND,
		KEY_CONSTRUCTOR_CHANGED_TYPE_PARAMETER,
		KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
		KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
		KEY_CONSTRUCTOR_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		KEY_CONSTRUCTOR_CHANGED_NON_STATIC_TO_STATIC,
		KEY_CONSTRUCTOR_CHANGED_STATIC_TO_NON_STATIC,
		KEY_CONSTRUCTOR_CHANGED_NON_FINAL_TO_FINAL,
		KEY_CONSTRUCTOR_REMOVED_ANNOTATION_DEFAULT_VALUE,
		KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETERS,
		KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
		KEY_CONSTRUCTOR_REMOVED_CLASS_BOUND,
		KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUND,
		KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUNDS,
		KEY_MISSING_SINCE_TAG,
		KEY_MALFORMED_SINCE_TAG,
		KEY_INVALID_SINCE_TAG_VERSION,
		KEY_INCOMPATIBLE_API_COMPONENT_VERSION,
		KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE,
		KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE,
	};

	/**
	 * Constant representing the {@link IDialogSettings} section for this block
	 */
	private static final String SETTINGS = "api_errorswarnings_block"; //$NON-NLS-1$
	
	/**
	 * Constant representing the severity values presented in the combo boxes for each option
	 */
	private static final String[] SEVERITIES_LABELS = {
		PreferenceMessages.ApiErrorsWarningsConfigurationBlock_error,
		PreferenceMessages.ApiErrorsWarningsConfigurationBlock_warning,
		PreferenceMessages.ApiErrorsWarningsConfigurationBlock_ignore
	};
	
	/**
	 * Constant representing the severity values presented in the combo boxes for each option
	 */
	private static final String[] SEVERITIES = {
		ApiPlugin.VALUE_ERROR,
		ApiPlugin.VALUE_WARNING,
		ApiPlugin.VALUE_IGNORE,
	};
	
	/**
	 * Default selection listener for controls on the page
	 */
	private SelectionListener selectionlistener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			Widget widget = e.widget;
			ControlData data = (ControlData) widget.getData();
			Key key = data.getKey();
			String newValue= null;
			if (widget instanceof Button) {
				newValue= data.getValue(((Button)widget).getSelection());
			} else if (widget instanceof Combo) {
				newValue= data.getValue(((Combo)widget).getSelectionIndex());
			} else {
				return;
			}
			String oldValue= setValue(key, newValue);
			validateSettings(key, oldValue, newValue);
		}
	};
	
	public void validateSettings(Key changedKey, String oldValue, String newValue) {
		// we need to disable the two checkboxes if the key is for the bundle version
		if (changedKey != null) {
			if (KEY_INCOMPATIBLE_API_COMPONENT_VERSION.equals(changedKey)) {
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
			return (String) fOldProjectSettings.get(key);
		}
		return key.getStoredValue(fLookupOrder, false, fManager);
	}
	protected String setValue(Key key, String value) {
		fDirty = true;
		fRebuildcount = 0;
		if (fOldProjectSettings != null) {
			return (String) fOldProjectSettings.put(key, value);
		}
		String oldValue= getValue(key);
		key.setStoredValue(fLookupOrder[0], value, fManager);
		return oldValue;
	}
	private void updateEnableStates() {
		boolean enableUnusedParams= !checkValue(KEY_INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_IGNORE);
		getCheckBox(KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE).setEnabled(enableUnusedParams);
		getCheckBox(KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE).setEnabled(enableUnusedParams);
	}

	class SetAllSelectionAdapter extends SelectionAdapter {
		String newValue;
		int kind;

		public SetAllSelectionAdapter(int kind, String newValue) {
			this.kind = kind;
			this.newValue = newValue;
		}
		
		public void widgetSelected(SelectionEvent e) {
			switch(this.kind) {
				case API_SCANNING_USAGE_PAGE_ID :
					setAllTo(this.newValue, fgAllApiScanningKeys);
					break;
				case VERSION_MANAGEMENT_PAGE_ID :
					setAllTo(this.newValue, fgAllVersionManagementKeys);
					break;
				case COMPATIBILITY_PAGE_ID :
					setAllTo(this.newValue, fgAllCompatibilityKeys);
					break;
			}
		}
	}
	
	/**
	 * Listing of all of the {@link ExpandableComposite}s in the block
	 */
	private ArrayList fExpComps = new ArrayList();

	/**
	 * Listing of all of the {@link Combo}s added to the block
	 */
	private ArrayList fCombos = new ArrayList();
	
	/**
	 * Listing of all of the {@link Checkbox}es added to the block
	 */
	private ArrayList fCheckBoxes = new ArrayList();
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
	 * Constructor
	 * @param project
	 */
	public ApiErrorsWarningsConfigurationBlock(IProject project, IWorkbenchPreferenceContainer container) {
		fProject = project;
		if(fProject != null) {
			fLookupOrder = new IScopeContext[] {
				new ProjectScope(fProject),
				new InstanceScope(),
				new DefaultScope()
			};
		}
		else {
			fLookupOrder = new IScopeContext[] {
				new InstanceScope(),
				new DefaultScope()
			};
		}
		if(container == null) {
			fManager = new WorkingCopyManager();
		}
		else {
			fManager = container.getWorkingCopyManager();
		}
		if (fProject == null || hasProjectSpecificSettings(fProject)) {
			fOldProjectSettings = null;
		} else {
			fOldProjectSettings = new IdentityHashMap();
			for (int i= 0; i < fgAllKeys.length; i++) {
				fOldProjectSettings.put(fgAllKeys[i], fgAllKeys[i].getStoredValue(fLookupOrder, false, fManager));
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
		TabFolder folder = new TabFolder(fMainComp, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 400;
		folder.setLayoutData(gd);

		// API scanning usage options
		createPage(
				API_SCANNING_USAGE_PAGE_ID,
				folder,
				PreferenceMessages.ApiToolingNotificationsBlock_0,
				PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_3);
		// API compatibility options
		createPage(
				COMPATIBILITY_PAGE_ID,
				folder,
				PreferenceMessages.ApiToolingNotificationsBlock_1,
				PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_8); 
		createPage(
				VERSION_MANAGEMENT_PAGE_ID,
				folder,
				PreferenceMessages.ApiToolingNotificationsBlock_2,
				PreferenceMessages.ApiProblemSeveritiesConfigurationBlock_9); 
		restoreExpansionState();
		validateSettings(null, null, null);
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
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();
		return Dialog.convertWidthInCharsToPixels(fontMetrics, chars);
	}	

	protected Button addCheckBox(Composite parent, String label, Key key, String[] values, int indent) {
		ControlData data= new ControlData(key, values);
		Font dialogFont = JFaceResources.getDialogFont();
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 3;
		gd.horizontalIndent= convertWidthInCharsToPixels(dialogFont, indent);
		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setFont(dialogFont);
		checkBox.setText(label);
		checkBox.setData(data);
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(this.selectionlistener);
		
		String currValue= key.getStoredValue(fLookupOrder, false, fManager);
		checkBox.setSelection(data.getSelection(currValue) == 0);
		fCheckBoxes.add(checkBox);
		return checkBox;
	}

	/**
	 * Creates a tab page parented in the folder
	 * @param kind
	 * @param folder
	 * @param name
	 * @param description
	 * @return
	 */
	private Composite createPage(int kind, TabFolder folder, String name, String description) {
		Composite page = SWTFactory.createComposite(folder, 1, 1, GridData.FILL_BOTH);
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(name);
		tab.setControl(page);
		
		SWTFactory.createVerticalSpacer(page, 1);
		SWTFactory.createWrapLabel(page, description, 1);
		SWTFactory.createVerticalSpacer(page, 1);
		
		switch(kind) {
			case API_SCANNING_USAGE_PAGE_ID : {
				ScrolledComposite scomp = new ScrolledComposite(page, SWT.H_SCROLL | SWT.V_SCROLL);
				scomp.setExpandHorizontal(true);
				scomp.setExpandVertical(true);
				scomp.setLayout(new GridLayout(1, false));
				scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				scomp.addListener(SWT.Resize, new Listener() {
					public void handleEvent(Event event) {
						handleExpand(getScrollingParent(event.widget));
					}
				});
				Composite sbody = SWTFactory.createComposite(scomp, 1, 1, GridData.FILL_BOTH);
				scomp.setContent(sbody);
				
				Composite client = createExpansibleComposite(sbody, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_general);
				initializeComboControls(client, 
						new String[] {PreferenceMessages.ApiErrorsWarningsConfigurationBlock_invalid_tag_use},
						new Key[] {KEY_INVALID_JAVADOC_TAG}
						);
				
				client = createExpansibleComposite(sbody, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_restrictions);
				initializeComboControls(client,
					new String[] {
						PreferenceMessages.ApiProblemSeveritiesNoImplement,
						PreferenceMessages.ApiProblemSeveritiesNoExtend,
						PreferenceMessages.ApiProblemSeveritiesNoReference,
						PreferenceMessages.ApiProblemSeveritiesNoInstanciate,
						PreferenceMessages.ApiErrorsWarningsConfigurationBlock_override_tagged_method,
					},
					new Key[] {
						KEY_NOIMPLEMENT,
						KEY_NOEXTEND,
						KEY_NOREFERENCE,
						KEY_NOINSTANTIATE,
						KEY_NOOVERRIDE,
					}
				);
				client = createExpansibleComposite(sbody, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_leaks);
				initializeComboControls(client,
						new String[] {
							PreferenceMessages.ApiErrorsWarningsConfigurationBlock_extend_non_api_class,
							PreferenceMessages.ApiErrorsWarningsConfigurationBlock_implement_non_api,
							PreferenceMessages.ApiErrorsWarningsConfigurationBlock_field_decl_non_api,
							PreferenceMessages.ApiErrorsWarningsConfigurationBlock_return_type_non_api,
							PreferenceMessages.ApiErrorsWarningsConfigurationBlock_parameter_non_api
						},
						new Key[] {
							KEY_LEAK_EXTEND,
							KEY_LEAK_IMPLEMENT,
							KEY_LEAK_FIELD_DECL,
							KEY_LEAK_METHOD_RETURN_TYPE,
							KEY_LEAK_METHOD_PARAM
						}
					);
				break;
			}
			case VERSION_MANAGEMENT_PAGE_ID :
				Composite vcomp = SWTFactory.createComposite(page, 2, 1, GridData.FILL_BOTH);
				initializeComboControls(vcomp,
					new String[] {
						PreferenceMessages.VersionManagementReportMissingSinceTag,
						PreferenceMessages.VersionManagementReportMalformedSinceTags,
						PreferenceMessages.VersionManagementReportInvalidSinceTagVersion,
						PreferenceMessages.VersionManagementReportInvalidApiComponentVersion,
					},
					new Key[] {
						KEY_MISSING_SINCE_TAG,
						KEY_MALFORMED_SINCE_TAG,
						KEY_INVALID_SINCE_TAG_VERSION,
						KEY_INCOMPATIBLE_API_COMPONENT_VERSION
					}
				);
				addCheckBox(
					vcomp,
					PreferenceMessages.VersionManagementReportInvalidApiComponentVersionIncludeMinorWithoutApiChange,
					KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE,
					new String[] { ApiPlugin.VALUE_DISABLED, ApiPlugin.VALUE_ENABLED },
					2);
				addCheckBox(
					vcomp,
					PreferenceMessages.VersionManagementReportInvalidApiComponentVersionIncludeMajorWithoutBreakingChange,
					KEY_INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE,
					new String[] { ApiPlugin.VALUE_DISABLED, ApiPlugin.VALUE_ENABLED },
					2);
				break;
			case COMPATIBILITY_PAGE_ID :
				// compatibility
				ScrolledComposite scomp = new ScrolledComposite(page, SWT.H_SCROLL | SWT.V_SCROLL);
				scomp.setExpandHorizontal(true);
				scomp.setExpandVertical(true);
				scomp.setLayout(new GridLayout(1, false));
				scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				scomp.addListener(SWT.Resize, new Listener() {
					public void handleEvent(Event event) {
						handleExpand(getScrollingParent(event.widget));
					}
				});
				Composite sbody = SWTFactory.createComposite(scomp, 1, 1, GridData.FILL_BOTH);
				scomp.setContent(sbody);
				
				Composite client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityAPIComponentElement);
				initializeComboControls(
						client,
						new String[] {
								PreferenceMessages.API_COMPONENT_REMOVED_API_TYPE,
								PreferenceMessages.API_COMPONENT_REMOVED_TYPE,
						},
						new Key[] {
								KEY_API_COMPONENT_REMOVED_API_TYPE,
								KEY_API_COMPONENT_REMOVED_TYPE,
						});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityAnnotationElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.ANNOTATION_ADDED_FIELD,
						PreferenceMessages.ANNOTATION_ADDED_METHOD,
						PreferenceMessages.ANNOTATION_ADDED_TYPE_MEMBER,
						PreferenceMessages.ANNOTATION_ADDED_CLASS_BOUND,
						PreferenceMessages.ANNOTATION_ADDED_INTERFACE_BOUND,
						PreferenceMessages.ANNOTATION_ADDED_INTERFACE_BOUNDS,
						PreferenceMessages.ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
						PreferenceMessages.ANNOTATION_ADDED_TYPE_PARAMETER,
						PreferenceMessages.ANNOTATION_CHANGED_CLASS_BOUND,
						PreferenceMessages.ANNOTATION_CHANGED_INTERFACE_BOUND,
						PreferenceMessages.ANNOTATION_CHANGED_INTERFACE_BOUNDS,
						PreferenceMessages.ANNOTATION_CHANGED_TO_CLASS,
						PreferenceMessages.ANNOTATION_CHANGED_TO_ENUM,
						PreferenceMessages.ANNOTATION_CHANGED_TO_INTERFACE,
						PreferenceMessages.ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						PreferenceMessages.ANNOTATION_CHANGED_RESTRICTIONS,
						PreferenceMessages.ANNOTATION_REMOVED_TYPE_PARAMETER,
						PreferenceMessages.ANNOTATION_REMOVED_TYPE_PARAMETERS,
						PreferenceMessages.ANNOTATION_REMOVED_CLASS_BOUND,
						PreferenceMessages.ANNOTATION_REMOVED_INTERFACE_BOUND,
						PreferenceMessages.ANNOTATION_REMOVED_INTERFACE_BOUNDS,
						PreferenceMessages.ANNOTATION_REMOVED_FIELD,
						PreferenceMessages.ANNOTATION_REMOVED_METHOD_DEFAULT_VALUE,
						PreferenceMessages.ANNOTATION_REMOVED_METHOD_NO_DEFAULT_VALUE,
						PreferenceMessages.ANNOTATION_REMOVED_TYPE_MEMBER,
					},
					new Key[] {
						KEY_ANNOTATION_ADDED_FIELD,
						KEY_ANNOTATION_ADDED_METHOD,
						KEY_ANNOTATION_ADDED_TYPE_MEMBER,
						KEY_ANNOTATION_ADDED_CLASS_BOUND,
						KEY_ANNOTATION_ADDED_INTERFACE_BOUND,
						KEY_ANNOTATION_ADDED_INTERFACE_BOUNDS,
						KEY_ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
						KEY_ANNOTATION_ADDED_TYPE_PARAMETER,
						KEY_ANNOTATION_CHANGED_CLASS_BOUND,
						KEY_ANNOTATION_CHANGED_INTERFACE_BOUND,
						KEY_ANNOTATION_CHANGED_INTERFACE_BOUNDS,
						KEY_ANNOTATION_CHANGED_TO_CLASS,
						KEY_ANNOTATION_CHANGED_TO_ENUM,
						KEY_ANNOTATION_CHANGED_TO_INTERFACE,
						KEY_ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						KEY_ANNOTATION_CHANGED_RESTRICTIONS,
						KEY_ANNOTATION_REMOVED_TYPE_PARAMETER,
						KEY_ANNOTATION_REMOVED_TYPE_PARAMETERS,
						KEY_ANNOTATION_REMOVED_CLASS_BOUND,
						KEY_ANNOTATION_REMOVED_INTERFACE_BOUND,
						KEY_ANNOTATION_REMOVED_INTERFACE_BOUNDS,
						KEY_ANNOTATION_REMOVED_FIELD,
						KEY_ANNOTATION_REMOVED_METHOD_DEFAULT_VALUE,
						KEY_ANNOTATION_REMOVED_METHOD_NO_DEFAULT_VALUE,
						KEY_ANNOTATION_REMOVED_TYPE_MEMBER,
					});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityInterfaceElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.INTERFACE_ADDED_FIELD,
						PreferenceMessages.INTERFACE_ADDED_METHOD,
						PreferenceMessages.INTERFACE_ADDED_TYPE_MEMBER,
						PreferenceMessages.INTERFACE_ADDED_CLASS_BOUND,
						PreferenceMessages.INTERFACE_ADDED_INTERFACE_BOUND,
						PreferenceMessages.INTERFACE_ADDED_INTERFACE_BOUNDS,
						PreferenceMessages.INTERFACE_ADDED_TYPE_PARAMETER,
						PreferenceMessages.INTERFACE_CHANGED_CLASS_BOUND,
						PreferenceMessages.INTERFACE_CHANGED_INTERFACE_BOUND,
						PreferenceMessages.INTERFACE_CHANGED_INTERFACE_BOUNDS,
						PreferenceMessages.INTERFACE_CHANGED_TO_CLASS,
						PreferenceMessages.INTERFACE_CHANGED_TO_ENUM,
						PreferenceMessages.INTERFACE_CHANGED_TO_ANNOTATION,
						PreferenceMessages.INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						PreferenceMessages.INTERFACE_CHANGED_RESTRICTIONS,
						PreferenceMessages.INTERFACE_REMOVED_TYPE_PARAMETER,
						PreferenceMessages.INTERFACE_REMOVED_TYPE_PARAMETERS,
						PreferenceMessages.INTERFACE_REMOVED_CLASS_BOUND,
						PreferenceMessages.INTERFACE_REMOVED_INTERFACE_BOUND,
						PreferenceMessages.INTERFACE_REMOVED_INTERFACE_BOUNDS,
						PreferenceMessages.INTERFACE_REMOVED_FIELD,
						PreferenceMessages.INTERFACE_REMOVED_METHOD,
						PreferenceMessages.INTERFACE_REMOVED_TYPE_MEMBER,
					},
					new Key[] {
						KEY_INTERFACE_ADDED_FIELD,
						KEY_INTERFACE_ADDED_METHOD,
						KEY_INTERFACE_ADDED_TYPE_MEMBER,
						KEY_INTERFACE_ADDED_CLASS_BOUND,
						KEY_INTERFACE_ADDED_INTERFACE_BOUND,
						KEY_INTERFACE_ADDED_INTERFACE_BOUNDS,
						KEY_INTERFACE_ADDED_TYPE_PARAMETER,
						KEY_INTERFACE_CHANGED_CLASS_BOUND,
						KEY_INTERFACE_CHANGED_INTERFACE_BOUND,
						KEY_INTERFACE_CHANGED_INTERFACE_BOUNDS,
						KEY_INTERFACE_CHANGED_TO_CLASS,
						KEY_INTERFACE_CHANGED_TO_ENUM,
						KEY_INTERFACE_CHANGED_TO_ANNOTATION,
						KEY_INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						KEY_INTERFACE_CHANGED_RESTRICTIONS,
						KEY_INTERFACE_REMOVED_TYPE_PARAMETER,
						KEY_INTERFACE_REMOVED_TYPE_PARAMETERS,
						KEY_INTERFACE_REMOVED_CLASS_BOUND,
						KEY_INTERFACE_REMOVED_INTERFACE_BOUND,
						KEY_INTERFACE_REMOVED_INTERFACE_BOUNDS,
						KEY_INTERFACE_REMOVED_FIELD,
						KEY_INTERFACE_REMOVED_METHOD,
						KEY_INTERFACE_REMOVED_TYPE_MEMBER,
					});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityEnumElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.ENUM_ADDED_FIELD,
						PreferenceMessages.ENUM_ADDED_METHOD,
						PreferenceMessages.ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						PreferenceMessages.ENUM_CHANGED_TO_ANNOTATION,
						PreferenceMessages.ENUM_CHANGED_TO_CLASS,
						PreferenceMessages.ENUM_CHANGED_TO_INTERFACE,
						PreferenceMessages.ENUM_CHANGED_RESTRICTIONS,
						PreferenceMessages.ENUM_REMOVED_FIELD,
						PreferenceMessages.ENUM_REMOVED_ENUM_CONSTANT,
						PreferenceMessages.ENUM_REMOVED_METHOD,
						PreferenceMessages.ENUM_REMOVED_CONSTRUCTOR,
						PreferenceMessages.ENUM_REMOVED_TYPE_MEMBER,
					},
					new Key[] {
						KEY_ENUM_ADDED_FIELD,
						KEY_ENUM_ADDED_METHOD,
						KEY_ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						KEY_ENUM_CHANGED_TO_ANNOTATION,
						KEY_ENUM_CHANGED_TO_CLASS,
						KEY_ENUM_CHANGED_TO_INTERFACE,
						KEY_ENUM_CHANGED_RESTRICTIONS,
						KEY_ENUM_REMOVED_FIELD,
						KEY_ENUM_REMOVED_ENUM_CONSTANT,
						KEY_ENUM_REMOVED_METHOD,
						KEY_ENUM_REMOVED_CONSTRUCTOR,
						KEY_ENUM_REMOVED_TYPE_MEMBER,
					});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityClassElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.CLASS_ADDED_FIELD,
						PreferenceMessages.CLASS_ADDED_METHOD,
						PreferenceMessages.CLASS_ADDED_TYPE_PARAMETER,
						PreferenceMessages.CLASS_ADDED_CLASS_BOUND,
						PreferenceMessages.CLASS_ADDED_INTERFACE_BOUND,
						PreferenceMessages.CLASS_ADDED_INTERFACE_BOUNDS,
						PreferenceMessages.CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						PreferenceMessages.CLASS_CHANGED_CONTRACTED_SUPERCLASS_SET,
						PreferenceMessages.CLASS_CHANGED_SUPERCLASS,
						PreferenceMessages.CLASS_CHANGED_CLASS_BOUND,
						PreferenceMessages.CLASS_CHANGED_INTERFACE_BOUND,
						PreferenceMessages.CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
						PreferenceMessages.CLASS_CHANGED_NON_FINAL_TO_FINAL,
						PreferenceMessages.CLASS_CHANGED_TO_ANNOTATION,
						PreferenceMessages.CLASS_CHANGED_TO_ENUM,
						PreferenceMessages.CLASS_CHANGED_TO_INTERFACE,
						PreferenceMessages.CLASS_CHANGED_DECREASE_ACCESS,
						PreferenceMessages.CLASS_CHANGED_RESTRICTIONS,
						PreferenceMessages.CLASS_REMOVED_FIELD,
						PreferenceMessages.CLASS_REMOVED_METHOD,
						PreferenceMessages.CLASS_REMOVED_CONSTRUCTOR,
						PreferenceMessages.CLASS_REMOVED_TYPE_MEMBER,
						PreferenceMessages.CLASS_REMOVED_TYPE_PARAMETER,
						PreferenceMessages.CLASS_REMOVED_TYPE_PARAMETERS,
						PreferenceMessages.CLASS_REMOVED_CLASS_BOUND,
						PreferenceMessages.CLASS_REMOVED_INTERFACE_BOUND,
						PreferenceMessages.CLASS_REMOVED_INTERFACE_BOUNDS,
					},
					new Key[] {
						KEY_CLASS_ADDED_FIELD,
						KEY_CLASS_ADDED_METHOD,
						KEY_CLASS_ADDED_TYPE_PARAMETER,
						KEY_CLASS_ADDED_CLASS_BOUND,
						KEY_CLASS_ADDED_INTERFACE_BOUND,
						KEY_CLASS_ADDED_INTERFACE_BOUNDS,
						KEY_CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
						KEY_CLASS_CHANGED_CONTRACTED_SUPERCLASS_SET,
						KEY_CLASS_CHANGED_SUPERCLASS,
						KEY_CLASS_CHANGED_CLASS_BOUND,
						KEY_CLASS_CHANGED_INTERFACE_BOUND,
						KEY_CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
						KEY_CLASS_CHANGED_NON_FINAL_TO_FINAL,
						KEY_CLASS_CHANGED_TO_ANNOTATION,
						KEY_CLASS_CHANGED_TO_ENUM,
						KEY_CLASS_CHANGED_TO_INTERFACE,
						KEY_CLASS_CHANGED_DECREASE_ACCESS,
						KEY_CLASS_CHANGED_RESTRICTIONS,
						KEY_CLASS_REMOVED_FIELD,
						KEY_CLASS_REMOVED_METHOD,
						KEY_CLASS_REMOVED_CONSTRUCTOR,
						KEY_CLASS_REMOVED_TYPE_MEMBER,
						KEY_CLASS_REMOVED_TYPE_PARAMETER,
						KEY_CLASS_REMOVED_TYPE_PARAMETERS,
						KEY_CLASS_REMOVED_CLASS_BOUND,
						KEY_CLASS_REMOVED_INTERFACE_BOUND,
						KEY_CLASS_REMOVED_INTERFACE_BOUNDS,
					});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityFieldElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.FIELD_ADDED_VALUE,
						PreferenceMessages.FIELD_CHANGED_TYPE,
						PreferenceMessages.FIELD_CHANGED_VALUE,
						PreferenceMessages.FIELD_CHANGED_DECREASE_ACCESS,
						PreferenceMessages.FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
						PreferenceMessages.FIELD_CHANGED_NON_FINAL_TO_FINAL,
						PreferenceMessages.FIELD_CHANGED_STATIC_TO_NON_STATIC,
						PreferenceMessages.FIELD_CHANGED_NON_STATIC_TO_STATIC,
						PreferenceMessages.FIELD_REMOVED_VALUE,
						PreferenceMessages.FIELD_REMOVED_TYPE_ARGUMENTS,
					},
					new Key[] {
						KEY_FIELD_ADDED_VALUE,
						KEY_FIELD_CHANGED_TYPE,
						KEY_FIELD_CHANGED_VALUE,
						KEY_FIELD_CHANGED_DECREASE_ACCESS,
						KEY_FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
						KEY_FIELD_CHANGED_NON_FINAL_TO_FINAL,
						KEY_FIELD_CHANGED_STATIC_TO_NON_STATIC,
						KEY_FIELD_CHANGED_NON_STATIC_TO_STATIC,
						KEY_FIELD_REMOVED_VALUE,
						KEY_FIELD_REMOVED_TYPE_ARGUMENTS,
					});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityMethodElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.METHOD_ADDED_CLASS_BOUND,
						PreferenceMessages.METHOD_ADDED_INTERFACE_BOUND,
						PreferenceMessages.METHOD_ADDED_INTERFACE_BOUNDS,
						PreferenceMessages.METHOD_ADDED_TYPE_PARAMETER,
						PreferenceMessages.METHOD_CHANGED_CLASS_BOUND,
						PreferenceMessages.METHOD_CHANGED_INTERFACE_BOUND,
						PreferenceMessages.METHOD_CHANGED_TYPE_PARAMETER,
						PreferenceMessages.METHOD_CHANGED_VARARGS_TO_ARRAY,
						PreferenceMessages.METHOD_CHANGED_DECREASE_ACCESS,
						PreferenceMessages.METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
						PreferenceMessages.METHOD_CHANGED_NON_STATIC_TO_STATIC,
						PreferenceMessages.METHOD_CHANGED_STATIC_TO_NON_STATIC,
						PreferenceMessages.METHOD_CHANGED_NON_FINAL_TO_FINAL,
						PreferenceMessages.METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
						PreferenceMessages.METHOD_REMOVED_TYPE_PARAMETERS,
						PreferenceMessages.METHOD_REMOVED_TYPE_PARAMETER,
						PreferenceMessages.METHOD_REMOVED_CLASS_BOUND,
						PreferenceMessages.METHOD_REMOVED_INTERFACE_BOUND,
						PreferenceMessages.METHOD_REMOVED_INTERFACE_BOUNDS,
					},
					new Key[] {
						KEY_METHOD_ADDED_CLASS_BOUND,
						KEY_METHOD_ADDED_INTERFACE_BOUND,
						KEY_METHOD_ADDED_INTERFACE_BOUNDS,
						KEY_METHOD_ADDED_TYPE_PARAMETER,
						KEY_METHOD_CHANGED_CLASS_BOUND,
						KEY_METHOD_CHANGED_INTERFACE_BOUND,
						KEY_METHOD_CHANGED_TYPE_PARAMETER,
						KEY_METHOD_CHANGED_VARARGS_TO_ARRAY,
						KEY_METHOD_CHANGED_DECREASE_ACCESS,
						KEY_METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
						KEY_METHOD_CHANGED_NON_STATIC_TO_STATIC,
						KEY_METHOD_CHANGED_STATIC_TO_NON_STATIC,
						KEY_METHOD_CHANGED_NON_FINAL_TO_FINAL,
						KEY_METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
						KEY_METHOD_REMOVED_TYPE_PARAMETERS,
						KEY_METHOD_REMOVED_TYPE_PARAMETER,
						KEY_METHOD_REMOVED_CLASS_BOUND,
						KEY_METHOD_REMOVED_INTERFACE_BOUND,
						KEY_METHOD_REMOVED_INTERFACE_BOUNDS,
					});
				client = createExpansibleComposite(sbody, PreferenceMessages.CompatibilityConstructorElement);
				initializeComboControls(
					client,
					new String[] {
						PreferenceMessages.CONSTRUCTOR_ADDED_CLASS_BOUND,
						PreferenceMessages.CONSTRUCTOR_ADDED_INTERFACE_BOUND,
						PreferenceMessages.CONSTRUCTOR_ADDED_INTERFACE_BOUNDS,
						PreferenceMessages.CONSTRUCTOR_ADDED_TYPE_PARAMETER,
						PreferenceMessages.CONSTRUCTOR_CHANGED_CLASS_BOUND,
						PreferenceMessages.CONSTRUCTOR_CHANGED_INTERFACE_BOUND,
						PreferenceMessages.CONSTRUCTOR_CHANGED_TYPE_PARAMETER,
						PreferenceMessages.CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
						PreferenceMessages.CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
						PreferenceMessages.CONSTRUCTOR_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
						PreferenceMessages.CONSTRUCTOR_CHANGED_NON_STATIC_TO_STATIC,
						PreferenceMessages.CONSTRUCTOR_CHANGED_STATIC_TO_NON_STATIC,
						PreferenceMessages.CONSTRUCTOR_CHANGED_NON_FINAL_TO_FINAL,
						PreferenceMessages.CONSTRUCTOR_REMOVED_TYPE_PARAMETERS,
						PreferenceMessages.CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
						PreferenceMessages.CONSTRUCTOR_REMOVED_CLASS_BOUND,
						PreferenceMessages.CONSTRUCTOR_REMOVED_INTERFACE_BOUND,
						PreferenceMessages.CONSTRUCTOR_REMOVED_INTERFACE_BOUNDS,
					},
					new Key[] {
						KEY_CONSTRUCTOR_ADDED_CLASS_BOUND,
						KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUND,
						KEY_CONSTRUCTOR_ADDED_INTERFACE_BOUNDS,
						KEY_CONSTRUCTOR_ADDED_TYPE_PARAMETER,
						KEY_CONSTRUCTOR_CHANGED_CLASS_BOUND,
						KEY_CONSTRUCTOR_CHANGED_INTERFACE_BOUND,
						KEY_CONSTRUCTOR_CHANGED_TYPE_PARAMETER,
						KEY_CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
						KEY_CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
						KEY_CONSTRUCTOR_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
						KEY_CONSTRUCTOR_CHANGED_NON_STATIC_TO_STATIC,
						KEY_CONSTRUCTOR_CHANGED_STATIC_TO_NON_STATIC,
						KEY_CONSTRUCTOR_CHANGED_NON_FINAL_TO_FINAL,
						KEY_CONSTRUCTOR_REMOVED_ANNOTATION_DEFAULT_VALUE,
						KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETERS,
						KEY_CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
						KEY_CONSTRUCTOR_REMOVED_CLASS_BOUND,
						KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUND,
						KEY_CONSTRUCTOR_REMOVED_INTERFACE_BOUNDS,
					});
		}
		
		SWTFactory.createVerticalSpacer(page, 1);
		
		Group bcomp = SWTFactory.createGroup(page, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_setAllto, 3, 2, GridData.FILL_HORIZONTAL);
		Button button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_error_button, null, SWT.RIGHT);
		button.addSelectionListener(new SetAllSelectionAdapter(kind, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_error));
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_warning_button, null, SWT.RIGHT);
		button.addSelectionListener(new SetAllSelectionAdapter(kind, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_warning));
		button = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_ignore_button, null, SWT.RIGHT);
		button.addSelectionListener(new SetAllSelectionAdapter(kind, PreferenceMessages.ApiErrorsWarningsConfigurationBlock_ignore));
		return page;
	}

	void setAllTo(String newValue, Key[] keys) {
		for(int i = 0, max = keys.length; i < max; i++) {
			keys[i].setStoredValue(fLookupOrder[0], newValue, fManager);
		}
		updateControls();
		validateSettings(null, null, null);
		fDirty = true;
		fRebuildcount = 0;
	}

	/**
	 * Creates an {@link ExpandableComposite} with a client composite and a default grid layout
	 * @param parent
	 * @param title
	 * @return
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
	 * @return
	 */
	private ScrolledComposite getScrollingParent(Object obj) {
		if(obj instanceof ExpandableComposite) {
			ExpandableComposite ecomp = (ExpandableComposite) obj;
			Composite parent = ecomp.getParent();
			while(parent != null && !(parent instanceof ScrolledComposite)) {
				parent = parent.getParent();
			}
			if(parent != null) {
				return (ScrolledComposite) parent;
			}
		}
		if(obj instanceof ScrolledComposite) {
			return (ScrolledComposite) obj;
		}
		return null;
	}
	
	/**
	 * Handles one of the expandable composites being expanded 
	 */
	private void handleExpand(ScrolledComposite composite) {
		if(composite == null) {
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
		}
		finally {
			composite.setRedraw(true);
		}
	}
	
	/**
	 * Saves all of the changes on the page
	 */
	public void performOK()  {
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
		if(fDirty) {
			try {
				ArrayList changes = new ArrayList();
				collectChanges(fLookupOrder[0], changes);
				if(changes.size() > 0) {
					if(fRebuildcount < 1) {
						fRebuildcount++;
						fManager.applyChanges();
						IProject[] projects = Util.getApiProjects();
						String message = PreferenceMessages.ApiErrorsWarningsConfigurationBlock_0;
						if(fProject != null) {
							projects = new IProject[] {fProject};
							message = MessageFormat.format(PreferenceMessages.ApiErrorsWarningsConfigurationBlock_1, new String[] {fProject.getName()});
						}
						if(projects != null) {
							if(MessageDialog.openQuestion(fParent.getShell(), PreferenceMessages.ApiErrorsWarningsConfigurationBlock_2, message)) {
								Util.getBuildJob(projects).schedule();
							}
						}
					}
				}
				fDirty = false;
			}
			catch(BackingStoreException bse) {
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
		for(int i = 0; i < fgAllKeys.length; i++) {
			defval = fgAllKeys[i].getStoredValue(fLookupOrder, true, fManager);
			fgAllKeys[i].setStoredValue(fLookupOrder[0], defval, fManager);
		}
		updateControls();
		validateSettings(null, null, null);
		fDirty = true;
		fRebuildcount = 0;
	}
	
	/**
	 * Updates all of the registered {@link Control}s on the page.
	 * Registration implies that the {@link Control} control was added to the listing 
	 * of fCombos or fCheckBoxes
	 */
	private void updateControls() {
		for(int i = 0; i < fCombos.size(); i++) {
			Combo combo = (Combo) fCombos.get(i);
			ControlData data  = (ControlData) combo.getData();
			combo.select(data.getSelection(getValue(data.getKey())));
		}
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button button = (Button) fCheckBoxes.get(i);
			ControlData data= (ControlData) button.getData();
			String currValue= getValue(data.getKey());
			button.setSelection(data.getSelection(currValue) == 0);
		}
	}
	/**
	 * recursive method to enable/disable all of the controls on the main page
	 * @param ctrl
	 * @param enabled
	 */
	private void enableControl(Control ctrl, boolean enabled) {
		ctrl.setEnabled(enabled);
		if(ctrl instanceof Composite) {
			Composite comp = (Composite) ctrl;
			Control[] children = comp.getChildren();
			for(int i = 0; i< children.length; i++) {
				enableControl(children[i], enabled);
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
	 * Creates a {@link Label} | {@link Combo} control. The combo is initialized from the given {@link Key}
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
	}

	/**
	 * Restores the expansion state of the composites in this block.
	 * If there are no settings, than the first composite is expanded by default
	 */
	private void restoreExpansionState() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS);
		if(settings != null) {
			for(int i = 0; i < fExpComps.size(); i++) {
				((ExpandableComposite) fExpComps.get(i)).setExpanded(settings.getBoolean(Integer.toString(i)));
			}
		}
		else {
			((ExpandableComposite)fExpComps.get(0)).setExpanded(true);
		}
	}
	
	/**
	 * Stores the expansion state of the composites
	 */
	private void persistExpansionState() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS);
		for(int i = 0; i < fExpComps.size(); i++) {
			settings.put(Integer.toString(i), ((ExpandableComposite) fExpComps.get(i)).isExpanded());
		}
	}
	
	/**
	 * Sets using project specific settings
	 * @param enable
	 */
	public void useProjectSpecificSettings(boolean enable) {
		boolean disabled = fOldProjectSettings == null;
		if(enable != disabled && fProject != null) {
			if(enable) {
				for(int i = 0; i < fgAllKeys.length; i++) {
					fgAllKeys[i].setStoredValue(fLookupOrder[0], (String) fOldProjectSettings.get(fgAllKeys[i]), fManager);
				}
				fOldProjectSettings = null;
				updateControls();
			} else {
				fOldProjectSettings = new IdentityHashMap();
				String old = null;
				for(int i = 0; i < fgAllKeys.length; i++) {
					old = fgAllKeys[i].getStoredValue(fLookupOrder, false, fManager);
					fOldProjectSettings.put(fgAllKeys[i], old);
					fgAllKeys[i].setStoredValue(fLookupOrder[0], null, fManager);
				}
			}
		}
		fDirty = true;
		enableControl(fMainComp, enable);
		if (enable) {
			validateSettings(null, null, null);
		}
	}
	
	/**
	 * returns if this block has fProject specific settings
	 * @param fProject
	 * @return true if there are fProject specific settings, false otherwise
	 */
	public boolean hasProjectSpecificSettings(IProject project) {
		if (project != null) {
			IScopeContext projectContext= new ProjectScope(project);
			for (int i= 0; i < fgAllKeys.length; i++) {
				if (fgAllKeys[i].getStoredValue(projectContext, fManager) != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Collects the keys that have changed on the page into the specified list
	 * @param changes the {@link List} to collect changed keys into
	 */
	private void collectChanges(IScopeContext context, List changes) {
		boolean complete = fOldProjectSettings == null && fProject != null;
		for(int i = 0; i < fgAllKeys.length; i++) {
			Key key = fgAllKeys[i];
			String origval = key.getStoredValue(context, null);
			String newval = key.getStoredValue(context, fManager);
			if(newval == null) {
				if(origval != null) {
					changes.add(key);
				}
				else if(complete) {
					key.setStoredValue(context, key.getStoredValue(fLookupOrder, true, fManager), fManager);
					changes.add(key);
				}
			}
			else if(!newval.equals(origval)) {
				changes.add(key);
			}
		}
	}
	
	public static Key[] getAllKeys() {
		return fgAllKeys;
	}
	protected Button getCheckBox(Key key) {
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr= (Button) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;
	}
}
