package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.internal.base.model.IWritable;
/**
 * A base interface for all the objects in the plug-in model.
 */
public interface IPluginObject extends IWritable, IAdaptable {
/**
 * A property name that will be used to notify
 * that the "name" field has changed.
 */
	String P_NAME = "name";
/**
 * Returns the model that owns this object.
 * @return the model instance
 */
IPluginModelBase getModel();
/**
 * Returns the name of this model object
 *@return the object name
 */
String getName();
/**
 * Returns the parent of this model object.
 *
 * @return the object's parent
 */
public IPluginObject getParent();
/**
 * Returns the top-level model object.
 *
 * @return the top-level model object
 */
public IPluginBase getPluginBase();
/**
 * Returns a string by locating the provided
 * key in the resource bundle associated with
 * the model.
 *
 * @param key the name to use for resource bundle lookup
 * @return value in the resource bundle for
 * the provided key, or the key itself if
 * not found.
 */
String getResourceString(String key);
/**
 * Chances the name of this model object.
 * This method may throw a CoreException
 * if the model is not editable.
 *
 * @param name the new object name
 */
void setName(String name) throws CoreException;
}
