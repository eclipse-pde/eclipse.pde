/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.internal.PDE;
import org.osgi.service.prefs.BackingStoreException;

public class CompilerFlags {
	public static final int ERROR = 0;
	public static final int WARNING = 1;
	public static final int IGNORE = 2;

	public static final int MARKER = 0;
	public static final int BOOLEAN = 1;
	public static final int STRING = 2;

	public static final int PLUGIN_FLAGS = 0;
	public static final int SCHEMA_FLAGS = 1;
	public static final int FEATURE_FLAGS = 2;
	public static final int SITE_FLAGS = 3;

	// Project or Instance preferences
	public static final String USE_PROJECT_PREF =
		"compilers.use-project"; //$NON-NLS-1$
		
	// Manifest compiler flags
	public static final String P_UNRESOLVED_IMPORTS =
		"compilers.p.unresolved-import"; //$NON-NLS-1$
	public static final String P_UNRESOLVED_EX_POINTS =
		"compilers.p.unresolved-ex-points"; //$NON-NLS-1$
	public static final String P_UNKNOWN_ELEMENT =
		"compilers.p.unknown-element"; //$NON-NLS-1$
	public static final String P_UNKNOWN_ATTRIBUTE =
		"compilers.p.unknown-attribute"; //$NON-NLS-1$
	public static final String P_ILLEGAL_ATT_VALUE =
		"compilers.p.illegal-att-value"; //$NON-NLS-1$
	public static final String P_UNKNOWN_CLASS = "compilers.p.unknown-class"; //$NON-NLS-1$
	public static final String P_UNKNOWN_RESOURCE =
		"compilers.p.unknown-resource"; //$NON-NLS-1$
	public static final String P_NO_REQUIRED_ATT =
		"compilers.p.no-required-att"; //$NON-NLS-1$
	public static final String P_NOT_EXTERNALIZED = 
		"compilers.p.not-externalized-att"; //$NON-NLS-1$
	public static final String P_DEPRECATED = 
		"compilers.p.deprecated"; //$NON-NLS-1$
	public static final String P_NOT_USED = 
		"compilers.p.unused-element-or-attribute"; //$NON-NLS-1$

	public static final String S_CREATE_DOCS = "compilers.s.create-docs"; //$NON-NLS-1$
	public static final String S_DOC_FOLDER = "compilers.s.doc-folder"; //$NON-NLS-1$
	public static final String S_OPEN_TAGS = "compilers.s.open-tags"; //$NON-NLS-1$
	public static final String S_FORBIDDEN_END_TAGS = "compilers.s.forbidden-end-tags"; //$NON-NLS-1$
	public static final String S_OPTIONAL_END_TAGS = "compilers.s.optional-end-tags"; //$NON-NLS-1$
	public static final String F_UNRESOLVED_PLUGINS =
		"compilers.f.unresolved-plugins"; //$NON-NLS-1$
	public static final String F_UNRESOLVED_FEATURES =
		"compilers.f.unresolved-features"; //$NON-NLS-1$

	private static final String[][] flags =
		{
			{
				P_UNRESOLVED_IMPORTS,
				P_UNRESOLVED_EX_POINTS,
				P_NO_REQUIRED_ATT,
				P_UNKNOWN_ELEMENT,
				P_UNKNOWN_ATTRIBUTE,
				P_ILLEGAL_ATT_VALUE,
				P_UNKNOWN_CLASS,
				P_UNKNOWN_RESOURCE,
				P_NOT_EXTERNALIZED,
				P_DEPRECATED,
				P_NOT_USED},
			{
			S_CREATE_DOCS, S_DOC_FOLDER, S_OPEN_TAGS, S_FORBIDDEN_END_TAGS, S_OPTIONAL_END_TAGS}, {
			F_UNRESOLVED_PLUGINS, F_UNRESOLVED_FEATURES }, {
		}
	};

	public static int getFlagType(String flagId) {
		if (flagId.equals(S_CREATE_DOCS))
			return BOOLEAN;
		if (flagId.equals(S_DOC_FOLDER))
			return STRING;
		return MARKER;
	}

	public static int getFlag(IProject project, String flagId) {
		if (project == null) {
			return getFlag((String) null, flagId);
		}
		return getFlag(project.getName(), flagId);
	}

	private static int getFlag(String project, String flagId) {
		String value = getString(project, flagId);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException nfe){
			return 0;
		}
	}

	public static boolean getBoolean(IProject project, String flagId) {
		if (project == null) {
			return getBoolean((String) null, flagId);
		}
		return getBoolean(project.getName(), flagId);
	}
	
	private static boolean getBoolean(String project, String flagId) {
		String value = getString(project, flagId);
		return Boolean.valueOf(value).booleanValue();
	}

	/**
	 * 
	 * @param project
	 *            project to use PROJECT,INSTANCE,DEFAULT scope or null to
	 *            use only INSTANCE,DEFAULT scope
	 * @param flagId
	 * @return value or ""
	 */
	public static String getString(IProject project, String flagId) {
		if (project == null) {
			return getString((String) null, flagId);
		}
		return getString(project.getName(), flagId);
	}
	
	/**
	 * 
	 * @param projectName
	 *            project name to use PROJECT,INSTANCE,DEFAULT scope or null to
	 *            use only INSTANCE,DEFAULT scope
	 * @param flagId
	 * @return value or ""
	 */
	private static String getString(String projectName, String flagId) {
		IPreferencesService service = Platform.getPreferencesService();
		IEclipsePreferences root = service.getRootNode();
		org.osgi.service.prefs.Preferences projectNode = null;
		if (projectName != null) {
			projectNode = root.node(ProjectScope.SCOPE).node(projectName).node(
					PDE.PLUGIN_ID);
		}
		org.osgi.service.prefs.Preferences instanceNode = root.node(
				InstanceScope.SCOPE).node(PDE.PLUGIN_ID);
		org.osgi.service.prefs.Preferences defaultNode = root.node(
				DefaultScope.SCOPE).node(PDE.PLUGIN_ID);
		org.osgi.service.prefs.Preferences[] nodes;
		if (projectNode != null)
			nodes = new org.osgi.service.prefs.Preferences[] { projectNode,
					instanceNode, defaultNode };
		else
			nodes = new org.osgi.service.prefs.Preferences[] { instanceNode,
					defaultNode };
		return service.get(flagId, "", nodes); //$NON-NLS-1$
	}

	public static int getDefaultFlag(String flagId) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		return pref.getDefaultInt(flagId);
	}

	public static String getDefaultString(String flagId) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		return pref.getDefaultString(flagId);
	}

	public static boolean getDefaultBoolean(String flagId) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		return pref.getDefaultBoolean(flagId);
	}

	public static void setFlag(String flagId, int value) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setValue(flagId, value);
	}

	public static void setFlag(IProject project, String flagId, int value) {
		setString(project, flagId, Integer.toString(value));
	}

	public static void setBoolean(String flagId, boolean value) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setValue(flagId, value);
	}

	public static void setBoolean(IProject project, String flagId, boolean value) {
		setString(project, flagId, Boolean.toString(value));
	}

	public static void setString(String flagId, String value) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setValue(flagId, value);
	}

	/**
	 * Sets preference on PROJECT scope
	 * @param project
	 * @param flagId
	 * @param value
	 */
	public static void setString(IProject project, String flagId, String value) {
		if(project == null){
			return;
		}
		IPreferencesService service = Platform.getPreferencesService();
		IEclipsePreferences root = service.getRootNode();
		org.osgi.service.prefs.Preferences preferences = root.node(ProjectScope.SCOPE).node(project.getName()).node(PDE.PLUGIN_ID);
		preferences.put(flagId, value);
		try{
			preferences.flush();
		} catch (BackingStoreException bse){
		}
	}
	/**
	 * Clears preference from Project scope
	 * @param project
	 * @param flagId
	 */
	public static void clear(IProject project, String flagId) {
		if(project == null){
			return;
		}
		IPreferencesService service = Platform.getPreferencesService();
		IEclipsePreferences root = service.getRootNode();
		org.osgi.service.prefs.Preferences preferences = root.node(ProjectScope.SCOPE).node(project.getName()).node(PDE.PLUGIN_ID);
		preferences.remove(flagId);
		try{
			preferences.flush();
		} catch (BackingStoreException bse){
		}
	}

	public static void initializeDefaults() {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setDefault(P_UNRESOLVED_IMPORTS, ERROR);
		pref.setDefault(P_UNRESOLVED_EX_POINTS, ERROR);
		pref.setDefault(P_NO_REQUIRED_ATT, ERROR);
		pref.setDefault(P_UNKNOWN_ELEMENT, ERROR);
		pref.setDefault(P_UNKNOWN_ATTRIBUTE, ERROR);
		pref.setDefault(P_ILLEGAL_ATT_VALUE, ERROR);
		pref.setDefault(P_UNKNOWN_CLASS, IGNORE);
		pref.setDefault(P_UNKNOWN_RESOURCE, IGNORE);
		pref.setDefault(P_NOT_EXTERNALIZED, IGNORE);
		pref.setDefault(P_DEPRECATED, IGNORE);
		pref.setDefault(P_NOT_USED, WARNING);

		pref.setDefault(S_CREATE_DOCS, false);
		pref.setDefault(S_DOC_FOLDER, "doc"); //$NON-NLS-1$
		pref.setDefault(S_OPEN_TAGS, WARNING);
		pref.setDefault(S_FORBIDDEN_END_TAGS, WARNING);
		pref.setDefault(S_OPTIONAL_END_TAGS, IGNORE);

		pref.setDefault(F_UNRESOLVED_PLUGINS, WARNING);
		pref.setDefault(F_UNRESOLVED_FEATURES, WARNING);
	}

	public static String[] getFlags(int group) {
		return flags[group];
	}
	
	/**
	 * Saves INSTANCE preferences
	 */
	public static void save() {
		PDE.getDefault().savePluginPreferences();
	}
}
