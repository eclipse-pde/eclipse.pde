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
package org.eclipse.pde.core.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
/**
 * A model object that represents the content of a plug-in or
 * fragment manifest. This object contains data that is common
 * for bo plug-ins and fragments.
 */
public interface IPluginBase extends IExtensions, IIdentifiable {
	/**
	 * A property that will be used to notify that
	 * the provider name has changed.
	 */
	public static final String P_PROVIDER = "provider-name";
	/**
	 * A property that will be used to notify
	 * that a version has changed.
	 */
	public static final String P_VERSION = "version";

	/**
	 * A property that will be used to notify
	 * that library order in a plug-in has changed. 
	 */
	public static final String P_LIBRARY_ORDER = "library_order";
	
	/**
	 * A property that will be used to notify
	 * that import order in a plug-in has changed. 
	 */
	public static final String P_IMPORT_ORDER = "import_order";
	
	/**
	 * A property that will be used to notify
	 * that 3.0 release compatibility flag has been changed. 
	 */
	public static final String P_SCHEMA_VERSION = "schema-version";
	/**
	 * Adds a new library to this plugin.
	 * This method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param library the new library object
	 */
	void add(IPluginLibrary library) throws CoreException;

	/**
	 * Adds a new plug-in import to this plugin.
	 * This method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param pluginImport the new import object
	 */
	void add(IPluginImport pluginImport) throws CoreException;
	/**
	 * Removes an import from the plugin. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param import the import object
	 */
	void remove(IPluginImport pluginImport) throws CoreException;
	/**
	 * Returns libraries referenced in this plug-in.
	 *
	 * @return an array of libraries
	 */
	IPluginLibrary[] getLibraries();
	/**
	 * Returns imports defined in this plug-in.
	 *
	 * @return an array of import objects
	 */
	IPluginImport[] getImports();
	/**
	 * Returns a name of the plug-in provider.
	 *
	 * @return plug-in provider name
	 */
	String getProviderName();
	/**
	 * Returns this plug-in's version
	 * @return the version of the plug-in
	 */
	String getVersion();
	/**
	 * Removes a library from the plugin. This
	 * method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param library the library object
	 */
	void remove(IPluginLibrary library) throws CoreException;
	/**
	 * Sets the name of the plug-in provider.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param providerName the new provider name
	 */
	void setProviderName(String providerName) throws CoreException;
	/**
	 * Sets the version of the plug-in.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param version the new plug-in version
	 */
	void setVersion(String version) throws CoreException;
	/**
	 * Swaps the positions of the provided libraries
	 * in the list of libraries. Libraries are looked up
	 * by the class loader in the order of declaration.
	 * If two libraries contain classes with the same
	 * name, library order will determine which one is
	 * encountered first.
	 *
	 * @param l1 the first library object
	 * @param l2 the second library object
	 */
	void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException;
	
	
	/**
	 * Swaps the positions of the plug-ins provided in
	 * in the dependency list. This order is the one used
	 * used by the classloader when loading classes.
	 *
	 * @param l1 the first library object
	 * @param l2 the second library object
	 */
	void swap(IPluginImport import1, IPluginImport import2) throws CoreException;
	
	/**
	 * Returns version of the manifest grammar
	 * @return version of the manifest grammer, or <samp>null</samp>
	 */
	String getSchemaVersion();
	/**
	 * Sets the R3.0 compatibility flag
	 * @param schemaVersion version of the manifest grammar
	 */
	void setSchemaVersion(String schemaVersion) throws CoreException;
}
