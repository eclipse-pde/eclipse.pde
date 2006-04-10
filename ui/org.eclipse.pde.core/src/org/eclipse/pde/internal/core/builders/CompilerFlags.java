/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

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
	public static final String USE_PROJECT_PREF = "compilers.use-project"; //$NON-NLS-1$

	// Manifest compiler flags
	public static final String P_UNRESOLVED_IMPORTS = "compilers.p.unresolved-import"; //$NON-NLS-1$

	public static final String P_UNRESOLVED_EX_POINTS = "compilers.p.unresolved-ex-points"; //$NON-NLS-1$

	public static final String P_UNKNOWN_ELEMENT = "compilers.p.unknown-element"; //$NON-NLS-1$

	public static final String P_UNKNOWN_ATTRIBUTE = "compilers.p.unknown-attribute"; //$NON-NLS-1$

	public static final String P_UNKNOWN_CLASS = "compilers.p.unknown-class"; //$NON-NLS-1$

	public static final String P_UNKNOWN_RESOURCE = "compilers.p.unknown-resource"; //$NON-NLS-1$

	public static final String P_NO_REQUIRED_ATT = "compilers.p.no-required-att"; //$NON-NLS-1$

	public static final String P_NOT_EXTERNALIZED = "compilers.p.not-externalized-att"; //$NON-NLS-1$
	
	public static final String P_BUILD = "compilers.p.build"; //$NON-NLS-1$
	
	public static final String P_INCOMPATIBLE_ENV = "compilers.incompatible-environment"; //$NON-NLS-1$

	public static final String P_DEPRECATED = "compilers.p.deprecated"; //$NON-NLS-1$

	public static final String S_CREATE_DOCS = "compilers.s.create-docs"; //$NON-NLS-1$

	public static final String S_DOC_FOLDER = "compilers.s.doc-folder"; //$NON-NLS-1$

	public static final String S_OPEN_TAGS = "compilers.s.open-tags"; //$NON-NLS-1$

	public static final String F_UNRESOLVED_PLUGINS = "compilers.f.unresolved-plugins"; //$NON-NLS-1$

	public static final String F_UNRESOLVED_FEATURES = "compilers.f.unresolved-features"; //$NON-NLS-1$

	private static final String[][] fFlags = {
			{ P_UNRESOLVED_IMPORTS,
			  P_INCOMPATIBLE_ENV,
			  P_UNRESOLVED_EX_POINTS, 
			  P_NO_REQUIRED_ATT,
			  P_UNKNOWN_ELEMENT, 
			  P_UNKNOWN_ATTRIBUTE, 
			  P_DEPRECATED,
			  P_UNKNOWN_CLASS, 
			  P_UNKNOWN_RESOURCE, 
			  P_NOT_EXTERNALIZED,
			  P_BUILD},
			{ S_CREATE_DOCS, 
			  S_DOC_FOLDER, 
			  S_OPEN_TAGS },
			{ F_UNRESOLVED_PLUGINS, 
			  F_UNRESOLVED_FEATURES }};

	public static int getFlagType(String flagId) {
		if (flagId.equals(S_CREATE_DOCS))
			return BOOLEAN;
		if (flagId.equals(S_DOC_FOLDER))
			return STRING;
		return MARKER;
	}

	public static int getFlag(IProject project, String flagId) {
		try {
			return Integer.parseInt(getString(project, flagId));
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	public static boolean getBoolean(IProject project, String flagId) {
		return Boolean.valueOf(getString(project, flagId)).booleanValue();
	}

	/**
	 * 
	 * @param project
	 *            project to use PROJECT,INSTANCE,DEFAULT scope or null to use
	 *            only INSTANCE,DEFAULT scope
	 * @param flagId
	 * @return value or ""
	 */
	public static String getString(IProject project, String flagId) {
		IPreferencesService service = Platform.getPreferencesService();
		IScopeContext[] contexts = project == null ? null
				: new IScopeContext[] { new ProjectScope(project) };
		return service.getString(PDE.PLUGIN_ID, flagId, "", //$NON-NLS-1$
				project == null ? null : contexts);
	}

	public static int getDefaultFlag(String flagId) {
		return new DefaultScope().getNode(PDE.PLUGIN_ID).getInt(flagId, 0);
	}

	public static String getDefaultString(String flagId) {
		return new DefaultScope().getNode(PDE.PLUGIN_ID).get(flagId, ""); //$NON-NLS-1$
	}

	public static boolean getDefaultBoolean(String flagId) {
		return new DefaultScope().getNode(PDE.PLUGIN_ID).getBoolean(flagId,
				false);
	}

	public static void setFlag(String flagId, int value) {
		if (getDefaultFlag(flagId) == value)
			new InstanceScope().getNode(PDE.PLUGIN_ID).remove(flagId);
		else
			new InstanceScope().getNode(PDE.PLUGIN_ID).putInt(flagId, value);
	}

	public static void setFlag(IProject project, String flagId, int value) {
		setString(project, flagId, Integer.toString(value));
	}

	public static void setBoolean(String flagId, boolean value) {
		if (getDefaultBoolean(flagId) == value)
			new InstanceScope().getNode(PDE.PLUGIN_ID).remove(flagId);
		else
			new InstanceScope().getNode(PDE.PLUGIN_ID)
					.putBoolean(flagId, value);
	}

	public static void setBoolean(IProject project, String flagId, boolean value) {
		setString(project, flagId, Boolean.toString(value));
	}

	public static void setString(String flagId, String value) {
		if (getDefaultString(flagId).equals(value))
			new InstanceScope().getNode(PDE.PLUGIN_ID).remove(flagId);
		else
			new InstanceScope().getNode(PDE.PLUGIN_ID).put(flagId, value);
	}

	/**
	 * Sets preference on PROJECT scope
	 * 
	 * @param project
	 * @param flagId
	 * @param value
	 */
	public static void setString(IProject project, String flagId, String value) {
		if (project == null)
			return;
		Preferences preferences = new ProjectScope(project)
				.getNode(PDE.PLUGIN_ID);
		preferences.put(flagId, value);
		try {
			preferences.flush();
		} catch (BackingStoreException bse) {
		}
	}

	/**
	 * Clears preference from Project scope
	 * 
	 * @param project
	 * @param flagId
	 */
	public static void clear(IProject project, String flagId) {
		if (project == null)
			return;
		Preferences preferences = new ProjectScope(project)
				.getNode(PDE.PLUGIN_ID);
		preferences.remove(flagId);
		try {
			preferences.flush();
		} catch (BackingStoreException bse) {
		}
	}

	public static void initializeDefaults() {
		Preferences node = new DefaultScope().getNode(PDE.PLUGIN_ID);
		node.putInt(P_UNRESOLVED_IMPORTS, ERROR);
		node.putInt(P_UNRESOLVED_EX_POINTS, ERROR);
		node.putInt(P_NO_REQUIRED_ATT, ERROR);
		node.putInt(P_UNKNOWN_ELEMENT, WARNING);
		node.putInt(P_UNKNOWN_ATTRIBUTE, WARNING);
		node.putInt(P_DEPRECATED, WARNING);
		node.putInt(P_UNKNOWN_CLASS, WARNING);
		node.putInt(P_UNKNOWN_RESOURCE, WARNING);
		node.putInt(P_NOT_EXTERNALIZED, IGNORE);
		node.putInt(P_BUILD, WARNING);
		node.putInt(P_INCOMPATIBLE_ENV, WARNING);

		node.putBoolean(S_CREATE_DOCS, false);
		node.put(S_DOC_FOLDER, "doc"); //$NON-NLS-1$
		node.putInt(S_OPEN_TAGS, WARNING);

		node.putInt(F_UNRESOLVED_PLUGINS, WARNING);
		node.putInt(F_UNRESOLVED_FEATURES, WARNING);
	}

	public static String[] getFlags(int group) {
		return fFlags[group];
	}

	/**
	 * Saves INSTANCE preferences
	 */
	public static void save() {
		try {
			new InstanceScope().getNode(PDE.PLUGIN_ID).flush();
		} catch (BackingStoreException e) {
		}
	}
}
