package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * A model object that represents the content of the plugin.xml
 * file.
 */
public interface IPlugin extends IPluginBase {
/**
 * A property that will be used when "className"
 * field is changed.
 */
public static final String P_CLASS_NAME = "class_name";
/**
 * Adds a new plug-in import to this plugin.
 * This method will throw a CoreException if
 * model is not editable.
 *
 * @param pluginImport the new import object
 */
void add(IPluginImport pluginImport) throws CoreException;
/**
 * Returns a plug-in class name.
 * @return plug-in class name or <samp>null</samp> if not specified.
 */
String getClassName();
/**
 * Returns imports defined in this plug-in.
 *
 * @return an array of import objects
 */
IPluginImport[] getImports();
/**
 * Removes an import from the plugin. This
 * method will throw a CoreException if
 * the model is not editable.
 *
 * @param import the import object
 */
void remove(IPluginImport pluginImport) throws CoreException;
/**
 * Sets the name of the plug-in class.
 * This method will throw a CoreException
 * if the model is not editable.
 *
 * @param className the new class name
 */
void setClassName(String className) throws CoreException;
}
