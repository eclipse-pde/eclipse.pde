package org.eclipse.pde.internal.core.util;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.internal.core.PDECore;

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

	public static final int PLUGIN_FLAGS = 0;
	public static final int FEATURE_FLAGS = 1;
	public static final int SITE_FLAGS = 2;

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
		}, {
		}
	};

	public static int getFlag(String flagId) {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		return pref.getInt(flagId);
	}

	public static void initializeDefaults() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		pref.setDefault(P_UNRESOLVED_IMPORTS, IGNORE);
		pref.setDefault(P_UNRESOLVED_EX_POINTS, IGNORE);
		pref.setDefault(P_UNKNOWN_ELEMENT, IGNORE);
		pref.setDefault(P_UNKNOWN_ATTRIBUTE, IGNORE);
		pref.setDefault(P_ILLEGAL_ATT_VALUE, IGNORE);
		pref.setDefault(P_UNKNOWN_CLASS, IGNORE);
		pref.setDefault(P_UNKNOWN_RESOURCE, IGNORE);
		pref.setDefault(P_NO_REQUIRED_ATT, IGNORE);
	}
	
	public static boolean isGroupActive(int group) {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String[] flagIds = getFlags(group);

		for (int i = 0; i < flagIds.length; i++) {
			String flagId = flagIds[i];
			if (pref.getInt(flagId)!=IGNORE)
				return true;
		}
		return false;
	}

	public static void restoreDefaults() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		restoreDefaults(pref, PLUGIN_FLAGS);
		restoreDefaults(pref, FEATURE_FLAGS);
		restoreDefaults(pref, SITE_FLAGS);
	}

	private static void restoreDefaults(Preferences pref, int group) {
		String[] flagIds = getFlags(group);
		for (int i = 0; i < flagIds.length; i++) {
			String flagId = flagIds[i];
			pref.setValue(flagId, pref.getDefaultInt(flagId));
		}
	}

	public static String[] getFlags(int group) {
		return flags[group];
	}

	public static void setFlag(String flagId, int value) {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		pref.setValue(flagId, value);
	}
}