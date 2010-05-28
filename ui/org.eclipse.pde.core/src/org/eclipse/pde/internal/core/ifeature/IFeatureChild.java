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
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IMatchRules;

/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureChild extends IFeatureObject, IIdentifiable, IMatchRules, IEnvironment {
	String P_VERSION = "version"; //$NON-NLS-1$
	String P_OPTIONAL = "optional"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
	String P_MATCH = "match"; //$NON-NLS-1$
	String P_FILTER = "filter"; //$NON-NLS-1$
	String P_SEARCH_LOCATION = "search-location"; //$NON-NLS-1$

	int ROOT = 0;
	int SELF = 1;
	int BOTH = 2;

	String getVersion();

	void setVersion(String version) throws CoreException;

	boolean isOptional();

	void setOptional(boolean optional) throws CoreException;

	String getName();

	void setName(String name) throws CoreException;

	int getSearchLocation();

	void setSearchLocation(int location) throws CoreException;

	int getMatch();

	void setMatch(int match) throws CoreException;

	String getFilter();

	void setFilter(String filter) throws CoreException;
}
