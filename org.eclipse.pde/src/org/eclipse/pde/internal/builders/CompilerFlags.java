/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

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

	// Manifest compiler flags
	public static final String P_UNRESOLVED_IMPORTS =
		"compilers.p.unresolved-import";
	public static final String P_UNRESOLVED_EX_POINTS =
		"compilers.p.unresolved-ex-points";
	public static final String P_UNKNOWN_ELEMENT =
		"compilers.p.unknown-element";
	public static final String P_UNKNOWN_ATTRIBUTE =
		"compilers.p.unknown-attribute";
	public static final String P_ILLEGAL_ATT_VALUE =
		"compilers.p.illegal-att-value";
	public static final String P_UNKNOWN_CLASS = "compilers.p.unknown-class";
	public static final String P_UNKNOWN_RESOURCE =
		"compilers.p.unknown-resource";
	public static final String P_NO_REQUIRED_ATT =
		"compilers.p.no-required-att";

	public static final String S_CREATE_DOCS = "compilers.s.create-docs";
	public static final String S_DOC_FOLDER = "compilers.s.doc-folder";
	public static final String S_OPEN_TAGS = "compilers.s.open-tags";
	public static final String S_FORBIDDEN_END_TAGS = "compilers.s.forbidden-end-tags";
	public static final String S_OPTIONAL_END_TAGS = "compilers.s.optional-end-tags";
	public static final String F_UNRESOLVED_PLUGINS =
		"compilers.f.unresolved-plugins";
	public static final String F_UNRESOLVED_FEATURES =
		"compilers.f.unresolved-features";

	private static final String[][] flags =
		{
			{
				P_UNRESOLVED_IMPORTS,
				P_UNRESOLVED_EX_POINTS,
				P_UNKNOWN_ELEMENT,
				P_UNKNOWN_ATTRIBUTE,
				P_ILLEGAL_ATT_VALUE,
				P_UNKNOWN_CLASS,
				P_UNKNOWN_RESOURCE,
				P_NO_REQUIRED_ATT },
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

	public static int getFlag(String flagId) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		return pref.getInt(flagId);
	}

	public static boolean getBoolean(String flagId) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		return pref.getBoolean(flagId);
	}

	public static String getString(String flagId) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		return pref.getString(flagId);
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

	public static void setBoolean(String flagId, boolean value) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setValue(flagId, value);
	}

	public static void setString(String flagId, String value) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setValue(flagId, value);
	}

	public static void initializeDefaults() {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		pref.setDefault(P_UNRESOLVED_IMPORTS, WARNING);
		pref.setDefault(P_UNRESOLVED_EX_POINTS, WARNING);
		pref.setDefault(P_UNKNOWN_ELEMENT, WARNING);
		pref.setDefault(P_UNKNOWN_ATTRIBUTE, WARNING);
		pref.setDefault(P_ILLEGAL_ATT_VALUE, WARNING);
		pref.setDefault(P_UNKNOWN_CLASS, IGNORE);
		pref.setDefault(P_UNKNOWN_RESOURCE, IGNORE);
		pref.setDefault(P_NO_REQUIRED_ATT, WARNING);

		pref.setDefault(S_CREATE_DOCS, false);
		pref.setDefault(S_DOC_FOLDER, "doc");
		pref.setDefault(S_OPEN_TAGS, WARNING);
		pref.setDefault(S_FORBIDDEN_END_TAGS, WARNING);
		pref.setDefault(S_OPTIONAL_END_TAGS, IGNORE);

		pref.setDefault(F_UNRESOLVED_PLUGINS, WARNING);
		pref.setDefault(F_UNRESOLVED_FEATURES, WARNING);
	}

	public static boolean isGroupActive(int group) {
		Preferences pref = PDE.getDefault().getPluginPreferences();
		String[] flagIds = getFlags(group);

		for (int i = 0; i < flagIds.length; i++) {
			String flagId = flagIds[i];
			if (pref.getInt(flagId) != IGNORE)
				return true;
		}
		return false;
	}

	public static String[] getFlags(int group) {
		return flags[group];
	}

	public static void save() {
		PDE.getDefault().savePluginPreferences();
	}
}