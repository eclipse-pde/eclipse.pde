package org.eclipse.pde.internal.base.model.plugin;

import org.eclipse.core.runtime.*;
/**
 * A model object that represents the content of the fragment.xml
 * file.
 */
public interface IFragment extends IPluginBase {
/**
 * A property that will be used to notify
 * that a plugin id has changed.
 */
public static final String P_PLUGIN_ID = "plugin-id";
/**
 * A property that will be used to notify
 * that a plugin version has changed.
 */
public static final String P_PLUGIN_VERSION = "plugin-version";
/**
 * Returns the id of the plug-in that is the target
 * of this fragment.
 * @return target plug-in id
 */
String getPluginId();
/**
 * Returns the version of the plug-in that is the target
 * of this fragment.
 * @return target plug-in version
 */
String getPluginVersion();
/**
 * Sets the id of the plug-in that will be the target of this fragment.
 * @exception org.eclipse.core.runtime.CoreException attempts to modify a read-only fragment will result in an exception
 */
void setPluginId(String id) throws CoreException;
/**
 * Sets the version of the plug-in that will be the target of this fragment.
 * @exception org.eclipse.core.runtime.CoreException attempts to modify a read-only fragment will result in an exception
 */
void setPluginVersion(String version) throws CoreException;
}
