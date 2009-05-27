/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.converter;

/**
 * Internal class copied from org.eclipse.osgi.  Referenced in PluginConverter and PluginConverterParser
 */
public interface IModel {

	public static final String FRAGMENT = "fragment"; //$NON-NLS-1$
	public static final String FRAGMENT_ID = "id"; //$NON-NLS-1$
	public static final String FRAGMENT_NAME = "name"; //$NON-NLS-1$
	public static final String FRAGMENT_PROVIDER = "provider-name"; //$NON-NLS-1$
	public static final String FRAGMENT_VERSION = "version"; //$NON-NLS-1$
	public static final String FRAGMENT_PLUGIN_ID = "plugin-id"; //$NON-NLS-1$
	public static final String FRAGMENT_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	public static final String FRAGMENT_PLUGIN_MATCH = "match"; //$NON-NLS-1$

	public static final String PLUGIN = "plugin"; //$NON-NLS-1$
	public static final String PLUGIN_ID = "id"; //$NON-NLS-1$
	public static final String PLUGIN_NAME = "name"; //$NON-NLS-1$
	public static final String PLUGIN_VENDOR = "vendor-name"; //$NON-NLS-1$
	public static final String PLUGIN_PROVIDER = "provider-name"; //$NON-NLS-1$
	public static final String PLUGIN_VERSION = "version"; //$NON-NLS-1$
	public static final String PLUGIN_CLASS = "class"; //$NON-NLS-1$

	public static final String RUNTIME = "runtime"; //$NON-NLS-1$
	public static final String PLUGIN_REQUIRES = "requires"; //$NON-NLS-1$

	public static final String EXTENSION_POINT = "extension-point"; //$NON-NLS-1$
	public static final String EXTENSION = "extension"; //$NON-NLS-1$

	public static final String LIBRARY = "library"; //$NON-NLS-1$
	public static final String LIBRARY_NAME = "name"; //$NON-NLS-1$
	public static final String LIBRARY_EXPORT_MASK = "name"; //$NON-NLS-1$
	public static final String LIBRARY_PACKAGES = "packages"; //$NON-NLS-1$
	public static final String LIBRARY_EXPORT = "export"; //$NON-NLS-1$

	public static final String PLUGIN_REQUIRES_PLUGIN = "plugin"; //$NON-NLS-1$
	public static final String PLUGIN_REQUIRES_IMPORT = "import"; //$NON-NLS-1$
	public static final String PLUGIN_REQUIRES_EXPORT = "export"; //$NON-NLS-1$
	public static final String PLUGIN_REQUIRES_MATCH = "match"; //$NON-NLS-1$
	public static final String PLUGIN_REQUIRES_PLUGIN_VERSION = "version"; //$NON-NLS-1$
	public static final String PLUGIN_REQUIRES_OPTIONAL = "optional"; //$NON-NLS-1$

	public static final String PLUGIN_REQUIRES_MATCH_PERFECT = "perfect"; //$NON-NLS-1$  // Brian
	public static final String PLUGIN_REQUIRES_MATCH_EQUIVALENT = "equivalent"; //$NON-NLS-1$ // Brian
	public static final String PLUGIN_REQUIRES_MATCH_COMPATIBLE = "compatible"; //$NON-NLS-1$ // Brian
	public static final String PLUGIN_REQUIRES_MATCH_GREATER_OR_EQUAL = "greaterOrEqual"; //$NON-NLS-1$  // Brian
}
