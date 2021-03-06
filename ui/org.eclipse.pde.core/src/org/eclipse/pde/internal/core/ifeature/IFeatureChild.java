/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;

/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureChild extends IFeatureObject, IIdentifiable, IEnvironment {
	String P_VERSION = "version"; //$NON-NLS-1$
	String P_OPTIONAL = "optional"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
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

	String getFilter();

	void setFilter(String filter) throws CoreException;
}
