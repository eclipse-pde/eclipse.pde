/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.update.configurator;

/**
 * Constants
 */
interface IConfigurationConstants {
	String ECLIPSE_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	String ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	String CFG = "config"; //$NON-NLS-1$
	String CFG_SITE = "site"; //$NON-NLS-1$
	String CFG_URL = "url"; //$NON-NLS-1$
	String CFG_POLICY = "policy"; //$NON-NLS-1$
	String[] CFG_POLICY_TYPE = { "USER-INCLUDE", "USER-EXCLUDE", "MANAGED-ONLY" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	String CFG_POLICY_TYPE_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	String CFG_LIST = "list"; //$NON-NLS-1$
	String CFG_UPDATEABLE = "updateable"; //$NON-NLS-1$
	String CFG_LINK_FILE = "linkfile"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY = "feature"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_ID = "id"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_PRIMARY = "primary"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_VERSION = "version"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER = "plugin-identifier"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_APPLICATION = "application"; //$NON-NLS-1$
	String CFG_FEATURE_ENTRY_ROOT = "root"; //$NON-NLS-1$
	String CFG_DATE = "date"; //$NON-NLS-1$
	String CFG_PLUGIN = "plugin"; //$NON-NLS-1$
	String CFG_ENABLED = "enabled"; //$NON-NLS-1$
	String CFG_SHARED_URL = "shared_ur"; //$NON-NLS-1$

	String CFG_VERSION = "version"; //$NON-NLS-1$
	String CFG_TRANSIENT = "transient"; //$NON-NLS-1$
	String VERSION = "3.0"; //$NON-NLS-1$

	int DEFAULT_POLICY_TYPE = IConfigurationConstants.USER_EXCLUDE;
	String[] DEFAULT_POLICY_LIST = new String[0];

	String PLUGINS = "plugins"; //$NON-NLS-1$
	String FEATURES = "features"; //$NON-NLS-1$
	String PLUGIN_XML = "plugin.xml"; //$NON-NLS-1$
	String FRAGMENT_XML = "fragment.xml"; //$NON-NLS-1$
	String META_MANIFEST_MF = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	String FEATURE_XML = "feature.xml"; //$NON-NLS-1$
	/**
	 * User-defined inclusion list. The list associated with this policy
	 * type is interpreted as path entries to included plugin.xml or
	 * fragment.xml <b>relative</b> to the site URL
	 */
	int USER_INCLUDE = 0;
	/**
	 * User-defined exclusion list. The list associated with this policy
	 * type is interpreted as path entries to excluded plugin.xml or
	 * fragment.xml <b>relative</b> to the site URL
	 */
	int USER_EXCLUDE = 1;
	/**
	 * When this site policy is used, only plug-ins specified by the
	 * configured features are contributed to the runtime.
	 *
	 * @since 3.1
	 */
	int MANAGED_ONLY = 2;
}
