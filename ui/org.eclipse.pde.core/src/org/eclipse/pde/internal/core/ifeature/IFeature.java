/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * The top-level model object of the Eclipse feature model.
 */
public interface IFeature extends IFeatureObject, IVersionable, IEnvironment {
	/**
	 * The name of the property that will be used to notify
	 * about changes in "description" field
	 */
	public static final String P_DESCRIPTION = "description"; //$NON-NLS-1$
	/**
	 * The name of the property that will be used to notify
	 * about changes in "copyright" field
	 */
	public static final String P_COPYRIGHT = "copyright"; //$NON-NLS-1$
	/**
	 * The name of the property that will be used to notify
	 * about changes in "license" field
	 */
	public static final String P_LICENSE = "license"; //$NON-NLS-1$
	/**
	 * The name of the property that will be used to notify
	 * about changes in "provider" field
	 */
	public static final String P_PROVIDER = "provider"; //$NON-NLS-1$
	/**
	 * The name of the property that will be used to notify
	 * about changes in "image" field
	 */
	public static final String P_IMAGE = "image"; //$NON-NLS-1$
	/**
	 * The name of the property that will be used to notify
	 * about changes in "url" field
	 */
	public static final String P_URL = "url"; //$NON-NLS-1$

	public static final String P_INSTALL_HANDLER = "installHandler"; //$NON-NLS-1$

	public static final String P_PRIMARY = "primary"; //$NON-NLS-1$
	public static final String P_EXCLUSIVE = "exclusive"; //$NON-NLS-1$
	public static final String P_PLUGIN = "plugin"; //$NON-NLS-1$

	public static final String P_COLLOCATION_AFFINITY = "colocation-affinity"; //$NON-NLS-1$
	public static final String P_APPLICATION = "application"; //$NON-NLS-1$

	public static final int INFO_DESCRIPTION = 0;
	public static final int INFO_COPYRIGHT = 1;
	public static final int INFO_LICENSE = 2;

	static final String[] INFO_TAGS = {"description", "copyright", "license"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/**
	 * Adds a plug-in reference to this feature.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param reference a plug-in reference to add
	 */
	public void addPlugins(IFeaturePlugin[] plugins) throws CoreException;

	/**
	 * Adds a data reference to this feature.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param entries a data entries to add
	 */
	public void addData(IFeatureData[] entries) throws CoreException;

	/**
	 * Adds included feature to this feature.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param features features to include
	 */
	public void addIncludedFeatures(IFeatureChild[] features) throws CoreException;

	/**
	 * Remove included feature from this feature.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param features included features to remove
	 */
	public void removeIncludedFeatures(IFeatureChild[] features) throws CoreException;

	/**
	 * Adds a required plug-in reference to this feature.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param reference a required plug-in reference to add
	 */
	public void addImports(IFeatureImport[] imports) throws CoreException;

	/**
	 * Returns references to plug-ins in this feature
	 *
	 * @return an array of plug-in references in this feature
	 */
	public IFeaturePlugin[] getPlugins();

	/**
	 * Returns references to data in this feature
	 *
	 * @return an array of data references in this feature
	 */
	public IFeatureData[] getData();

	/**
	 * Returns references to required plug-ins in this feature
	 *
	 * @return an array of plug-in references in this feature
	 */
	public IFeatureImport[] getImports();

	/**
	 * Returns references to included features
	 *
	 * @return an array of feature references included in this feature
	 */
	public IFeatureChild[] getIncludedFeatures();

	/**
	 * Returns a feature provider name
	 *
	 * @return the feature provider name, or <samp>null</samp> if not set
	 */
	public String getProviderName();

	/**
	 * Returns a feature image name
	 *
	 * @return the feature image name, or <samp>null</samp> if not set
	 */
	public String getImageName();

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

	public IFeatureInstallHandler getInstallHandler();

	public void setInstallHandler(IFeatureInstallHandler handler) throws CoreException;

	/**
	 * Return the feature info for the given info type or <code>null</code> if
	 * the expected information does not exist.
	 * 
	 * @param infoType One of {@link IFeature#INFO_LICENSE}, {@link IFeature#INFO_COPYRIGHT} or {@link IFeature#INFO_DESCRIPTION}
	 * @return the requested feature info or <code>null</code>
	 */
	public IFeatureInfo getFeatureInfo(int infoType);

	public void setFeatureInfo(IFeatureInfo info, int index) throws CoreException;

	/**
	 * Removes a plug-in reference from this feature. This
	 * method may throw a CoreException if the model
	 * is not editable.
	 *
	 * @param plugin a plug-in reference to remove 
	 */
	public void removePlugins(IFeaturePlugin[] plugins) throws CoreException;

	/**
	 * Removes a data reference from this feature. This
	 * method may throw a CoreException if the model
	 * is not editable.
	 *
	 * @param entries data entries to remove 
	 */
	public void removeData(IFeatureData[] entries) throws CoreException;

	/**
	 * Removes a required plug-in reference from this feature.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param iimport a required plug-in reference to add
	 */
	public void removeImports(IFeatureImport[] imports) throws CoreException;

	/**
	 * Sets the provider name of this feature. This method
	 * may throw a CoreException if the model is not editable.
	 *
	 * @param the new provider name
	 */
	public void setProviderName(String providerName) throws CoreException;

	/**
	 * Sets the image name of this feature. This method
	 * may throw a CoreException if the model is not editable.
	 *
	 * @param the new image name
	 */
	public void setImageName(String imageName) throws CoreException;

	/**
	/**
	 * Sets the URL model object of this feature.
	 *
	 *@param url The URL model object.
	 */
	public void setURL(IFeatureURL url) throws CoreException;

	public void computeImports() throws CoreException;

	boolean isPrimary();

	public String getLicenseFeatureID();

	public void setLicenseFeatureID(String licenseFeatureID);

	public String getLicenseFeatureVersion();

	public void setLicenseFeatureVersion(String version);

	public void setPrimary(boolean value) throws CoreException;

	boolean isExclusive();

	public void setExclusive(boolean value) throws CoreException;

	String getPlugin();

	void setPlugin(String value) throws CoreException;

	String getColocationAffinity();

	void setColocationAffinity(String value) throws CoreException;

	String getApplication();

	void setApplication(String value) throws CoreException;

	boolean isValid();

	public void swap(IFeatureChild feature1, IFeatureChild feature2);
}
