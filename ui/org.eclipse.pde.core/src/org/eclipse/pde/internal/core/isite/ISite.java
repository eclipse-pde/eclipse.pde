/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	void addArchives(ISiteArchive[] archives) throws CoreException;

	void addCategoryDefinitions(ISiteCategoryDefinition[] defs) throws CoreException;

	void removeFeatures(ISiteFeature[] features) throws CoreException;

	void removeArchives(ISiteArchive[] archives) throws CoreException;

	void removeCategoryDefinitions(ISiteCategoryDefinition[] defs) throws CoreException;

	ISiteFeature[] getFeatures();

	ISiteArchive[] getArchives();

	ISiteCategoryDefinition[] getCategoryDefinitions();

	boolean isValid();
}
