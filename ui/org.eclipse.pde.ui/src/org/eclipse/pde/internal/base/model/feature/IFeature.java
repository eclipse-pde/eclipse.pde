package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * The top-level model object of the Eclipse feature model.
 */
public interface IFeature extends IFeatureObject, IVersonable {
/**
 * The name of the property that will be used to notify
 * about changes in "description" field
 */
public static final String P_DESCRIPTION = "description";
/**
 * The name of the property that will be used to notify
 * about changes in "copyright" field
 */
public static final String P_COPYRIGHT = "copyright";
/**
 * The name of the property that will be used to notify
 * about changes in "license" field
 */
public static final String P_LICENSE = "license";
/**
 * The name of the property that will be used to notify
 * about changes in "provider" field
 */
public static final String P_PROVIDER = "provider";
/**
 * The name of the property that will be used to notify
 * about changes in "url" field
 */
public static final String P_URL = "url";


public static final int INFO_DESCRIPTION = 0;
public static final int INFO_LICENSE = 1;
public static final int INFO_COPYRIGHT = 2;
public static final String [] INFO_TAGS = { "description", "license", "copyright" };
/**
 * Adds a plug-in reference to this feature.
 * This method may throw a CoreException if
 * the model is not editable.
 *
 * @param reference a plug-in reference to add
 */
public void addPlugin(IFeaturePlugin plugin) throws CoreException;

/**
 * Adds a required plug-in reference to this feature.
 * This method may throw a CoreException if
 * the model is not editable.
 *
 * @param reference a required plug-in reference to add
 */
public void addImport(IFeatureImport iimport) throws CoreException;
/**
 * Returns references to plug-ins in this feature
 *
 * @return an array of plug-in references in this feature
 */
public IFeaturePlugin [] getPlugins();

/**
 * 
 */
public void setPlugins(IFeaturePlugin [] plugins) throws CoreException;
/**
 * Returns references to required plug-ins in this feature
 *
 * @return an array of plug-in references in this feature
 */
public IFeatureImport [] getImports();
/**
 * Returns a feature provider name
 *
 * @return the feature provider name, or <samp>null</samp> if not set
 */
public String getProviderName();
/**
 *
 */
IPluginModelBase getReferencedModel(IFeaturePlugin reference);
/**
 * Returns a feature URL model object
 *
 * @return the feature URL model object, or <samp>null</samp> if not set
 */
public IFeatureURL getURL();

public IFeatureInfo getFeatureInfo(int index);

public void setFeatureInfo(IFeatureInfo info, int index) throws CoreException;

/**
 * Removes a plug-in reference from this feature. This
 * method may throw a CoreException if the model
 * is not editable.
 *
 * @param plugin a plug-in reference to remove 
 */
public void removePlugin(IFeaturePlugin plugin) throws CoreException;
/**
 * Removes a required plug-in reference from this feature.
 * This method may throw a CoreException if
 * the model is not editable.
 *
 * @param iimport a required plug-in reference to add
 */
public void removeImport(IFeatureImport iimport) throws CoreException;
/**
 * Sets the provider name of this feature. This method
 * may throw a CoreException if the model is not editable.
 *
 * @param the new provider name
 */
public void setProviderName(String providerName) throws CoreException;
/**
 * Sets the URL model object of this feature.
 *
 *@param url The URL model object.
 */
public void setURL(IFeatureURL url) throws CoreException;

public void computeImports() throws CoreException;
}
