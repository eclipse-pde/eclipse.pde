/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - on going enhancements and maintenance
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Class used to handle compiler related preferences. 
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CompilerFlags {

	/**
	 * Compiler flag options as integers
	 */
	public static final int ERROR = 0;
	public static final int WARNING = 1;
	public static final int IGNORE = 2;

	/**
	 * categories of flags 
	 */
	public static final int PLUGIN_FLAGS = 0;
	public static final int SCHEMA_FLAGS = 1;
	public static final int FEATURE_FLAGS = 2;
	public static final int SITE_FLAGS = 3;

	/**
	 * plugin preferences
	 */
	public static final String P_UNRESOLVED_IMPORTS = "compilers.p.unresolved-import"; //$NON-NLS-1$
	public static final String P_UNRESOLVED_EX_POINTS = "compilers.p.unresolved-ex-points"; //$NON-NLS-1$
	public static final String P_UNKNOWN_ELEMENT = "compilers.p.unknown-element"; //$NON-NLS-1$
	public static final String P_UNKNOWN_ATTRIBUTE = "compilers.p.unknown-attribute"; //$NON-NLS-1$
	public static final String P_UNKNOWN_CLASS = "compilers.p.unknown-class"; //$NON-NLS-1$
	public static final String P_UNKNOWN_RESOURCE = "compilers.p.unknown-resource"; //$NON-NLS-1$
	public static final String P_UNKNOWN_IDENTIFIER = "compilers.p.unknown-identifier"; //$NON-NLS-1$
	public static final String P_DISCOURAGED_CLASS = "compilers.p.discouraged-class"; //$NON-NLS-1$
	public static final String P_NO_REQUIRED_ATT = "compilers.p.no-required-att"; //$NON-NLS-1$
	public static final String P_NOT_EXTERNALIZED = "compilers.p.not-externalized-att"; //$NON-NLS-1$
	public static final String P_BUILD = "compilers.p.build"; //$NON-NLS-1$
	public static final String P_BUILD_MISSING_OUTPUT = "compilers.p.build.missing.output"; //$NON-NLS-1$
	public static final String P_BUILD_SOURCE_LIBRARY = "compilers.p.build.source.library"; //$NON-NLS-1$
	public static final String P_BUILD_OUTPUT_LIBRARY = "compilers.p.build.output.library"; //$NON-NLS-1$
	public static final String P_BUILD_SRC_INCLUDES = "compilers.p.build.src.includes"; //$NON-NLS-1$
	public static final String P_BUILD_BIN_INCLUDES = "compilers.p.build.bin.includes"; //$NON-NLS-1$
	public static final String P_BUILD_JAVA_COMPLIANCE = "compilers.p.build.java.compliance"; //$NON-NLS-1$
	public static final String P_BUILD_JAVA_COMPILER = "compilers.p.build.java.compiler"; //$NON-NLS-1$
	public static final String P_BUILD_ENCODINGS = "compilers.p.build.encodings"; //$NON-NLS-1$
	public static final String P_INCOMPATIBLE_ENV = "compilers.incompatible-environment"; //$NON-NLS-1$
	public static final String P_MISSING_EXPORT_PKGS = "compilers.p.missing-packages"; //$NON-NLS-1$
	public static final String P_DEPRECATED = "compilers.p.deprecated"; //$NON-NLS-1$
	public static final String P_INTERNAL = "compilers.p.internal"; //$NON-NLS-1$

	public static final String P_MISSING_VERSION_EXP_PKG = "compilers.p.missing-version-export-package"; //$NON-NLS-1$
	public static final String P_MISSING_VERSION_IMP_PKG = "compilers.p.missing-version-import-package"; //$NON-NLS-1$
	public static final String P_MISSING_VERSION_REQ_BUNDLE = "compilers.p.missing-version-require-bundle"; //$NON-NLS-1$

	/**
	 * schema preferences 
	 */
	public static final String S_CREATE_DOCS = "compilers.s.create-docs"; //$NON-NLS-1$
	public static final String S_DOC_FOLDER = "compilers.s.doc-folder"; //$NON-NLS-1$
	public static final String S_OPEN_TAGS = "compilers.s.open-tags"; //$NON-NLS-1$

	/**
	 * feature preferences 
	 */
	public static final String F_UNRESOLVED_PLUGINS = "compilers.f.unresolved-plugins"; //$NON-NLS-1$
	public static final String F_UNRESOLVED_FEATURES = "compilers.f.unresolved-features"; //$NON-NLS-1$

	/**
	 * Returns the value for the requested preference, or 0 if there was a problem getting the preference value
	 * @param project to use as a project specific settings scope, or null
	 * @param flagId the id of the preference to retrieve
	 * @return the value for the given preference id
	 */
	public static int getFlag(IProject project, String flagId) {
		try {
			return Integer.parseInt(getString(project, flagId));
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	/**
	 * Returns the boolean preference denoted by the flag id (preference id)
	 * @param project to use as a project specific settings scope, or null
	 * @param flagId the id of the preference to retrieve
	 * @return the boolean value for the given preference id
	 */
	public static boolean getBoolean(IProject project, String flagId) {
		return Boolean.valueOf(getString(project, flagId)).booleanValue();
	}

	/**
	 * Returns the string preference for the given preference id
	 * @param project to use as a project specific settings scope, or null
	 * @param flagId the id of the preference to retrieve
	 * @return preference value or an empty string, never <code>null</code>
	 */
	public static String getString(IProject project, String flagId) {
		IPreferencesService service = Platform.getPreferencesService();
		IScopeContext[] contexts = project == null ? null : new IScopeContext[] {new ProjectScope(project)};
		return service.getString(PDE.PLUGIN_ID, flagId, "", project == null ? null : contexts); //$NON-NLS-1$
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
