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
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;
/**
 * The top-level model object of the Eclipse update site model.
 */
public interface ISite extends ISiteObject {
	String P_URL = "URL";
	String P_TYPE = "type";
	String P_DESCRIPTION = "description";
	
	void setType(String type) throws CoreException;
	String getType();
	
	void setURL(String url) throws CoreException;
	String getURL();

	ISiteDescription getDescription();
	void setDescription(ISiteDescription description) throws CoreException;

	void addFeatures(ISiteFeature[] features) throws CoreException;
	void addArchives(ISiteArchive[] archives) throws CoreException;
	void addCategoryDefinitions(ISiteCategoryDefinition[] defs)
		throws CoreException;

	void removeFeatures(ISiteFeature[] features) throws CoreException;
	void removeArchives(ISiteArchive[] archives) throws CoreException;
	void removeCategoryDefinitions(ISiteCategoryDefinition[] defs)
		throws CoreException;

	ISiteFeature[] getFeatures();
	ISiteArchive[] getArchives();
	ISiteCategoryDefinition[] getCategoryDefinitions();
	boolean isValid();
}
