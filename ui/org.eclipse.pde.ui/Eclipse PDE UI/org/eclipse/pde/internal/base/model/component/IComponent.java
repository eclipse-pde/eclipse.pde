package org.eclipse.pde.internal.base.model.component;

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * The top-level model object of the Eclipse component model.
 */
public interface IComponent extends IComponentObject, IIdentifiable {
/**
 * The name of the property that will be used to notify
 * about changes in "id" field
 */
public static final String P_ID = "id";
/**
 * The name of the property that will be used to notify
 * about changes in "description" field
 */
public static final String P_DESCRIPTION = "description";
/**
 * The name of the property that will be used to notify
 * about changes in "provider" field
 */
public static final String P_PROVIDER = "provider";
/**
 * The name of the property that will be used to notify
 * about changes in "version" field
 */
public static final String P_VERSION = "version";
/**
 * The name of the property that will be used to notify
 * about changes in "url" field
 */
public static final String P_URL = "url";
/**
 * Adds a fragment reference to this component.
 * This method may throw a CoreException if
 * the model is not editable.
 *
 * @param reference a fragment reference to add
 */
public void addFragment(IComponentFragment fragment) throws CoreException;
/**
 * Adds a plug-in reference to this component.
 * This method may throw a CoreException if
 * the model is not editable.
 *
 * @param reference a fragment reference to add
 */
public void addPlugin(IComponentPlugin plugin) throws CoreException;
/**
 * Returns a description of this component
 *
 * @return the component description, or <samp>null</samp> if not set
 */
public String getDescription();
/**
 * Returns references to fragments in this component
 *
 * @return an array of fragment references in this component
 */
public IComponentFragment [] getFragments();
/**
 * Returns references to plug-ins in this component
 *
 * @return an array of plug-in references in this component
 */
public IComponentPlugin [] getPlugins();
/**
 * Returns a component provider name
 *
 * @return the component provider name, or <samp>null</samp> if not set
 */
public String getProviderName();
/**
 *
 */
IPluginModelBase getReferencedModel(IComponentReference reference);
/**
 * Returns a component URL model object
 *
 * @return the component URL model object, or <samp>null</samp> if not set
 */
public IComponentURL getURL();
/**
 * Returns a component version
 *
 * @return the component version name, or <samp>null</samp> if not set
 */
public String getVersion();
/**
 * Removes a fragment reference from this component. This
 * method may throw a CoreException if the model
 * is not editable.
 *
 * @param a fragment reference to remove 
 */
public void removeFragment(IComponentFragment fragment) throws CoreException;
/**
 * Removes a plug-in reference from this component. This
 * method may throw a CoreException if the model
 * is not editable.
 *
 * @param a plug-in reference to remove 
 */
public void removePlugin(IComponentPlugin plugin) throws CoreException;
/**
 * Sets the description of this component. This method
 * may throw a CoreException if the model is not editable.
 *
 * @param the new description
 */
public void setDescription(String description) throws CoreException;
/**
 * Sets the provider name of this component. This method
 * may throw a CoreException if the model is not editable.
 *
 * @param the new provider name
 */
public void setProviderName(String providerName) throws CoreException;
/**
 * Sets the URL model object of this component.
 *
 *@param url The URL model object.
 */
public void setURL(IComponentURL url) throws CoreException;
/**
 * Sets the version of this component. This method
 * may throw a CoreException if the model is not editable.
 *
 * @param the new version
 */
public void setVersion(String version) throws CoreException;
}
