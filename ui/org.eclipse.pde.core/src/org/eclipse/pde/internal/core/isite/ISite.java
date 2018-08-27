/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

/**
 * The top-level model object of the Eclipse update site model.
 */
public interface ISite extends ISiteObject {
	String P_URL = "URL"; //$NON-NLS-1$

	String P_MIRRORS_URL = "mirrorsURL"; //$NON-NLS-1$

	String P_TYPE = "type"; //$NON-NLS-1$

	String P_DESCRIPTION = "description"; //$NON-NLS-1$

	String P_DIGEST_URL = "digestURL"; //$NON-NLS-1$

	String P_ASSOCIATE_SITES_URL = "associateSitesURL"; //$NON-NLS-1$

	String P_STATS = "stats"; //$NON-NLS-1$

	void setType(String type) throws CoreException;

	String getType();

	void setURL(String url) throws CoreException;

	void setMirrorsURL(String url) throws CoreException;

	void setDigestURL(String url) throws CoreException;

	void setAssociateSitesURL(String url) throws CoreException;

	String getURL();

	String getMirrorsURL();

	String getDigestURL();

	String getAssociateSitesURL();

	ISiteDescription getDescription();

	void setDescription(ISiteDescription description) throws CoreException;

	void addFeatures(ISiteFeature[] features) throws CoreException;

	void addBundles(ISiteBundle[] added) throws CoreException;

	void addArchives(ISiteArchive[] archives) throws CoreException;

	void addCategoryDefinitions(ISiteCategoryDefinition[] defs) throws CoreException;

	void addRepositoryReferences(IRepositoryReference[] repositories) throws CoreException;

	void removeFeatures(ISiteFeature[] features) throws CoreException;

	void removeBundles(ISiteBundle[] bundles) throws CoreException;

	void removeArchives(ISiteArchive[] archives) throws CoreException;

	void removeCategoryDefinitions(ISiteCategoryDefinition[] defs) throws CoreException;

	void removeRepositoryReferences(IRepositoryReference[] repositories) throws CoreException;

	ISiteFeature[] getFeatures();

	ISiteBundle[] getBundles();

	ISiteArchive[] getArchives();

	ISiteCategoryDefinition[] getCategoryDefinitions();

	IRepositoryReference[] getRepositoryReferences();

	IStatsInfo getStatsInfo();

	void setStatsInfo(IStatsInfo info) throws CoreException;

	@Override
	boolean isValid();

}
