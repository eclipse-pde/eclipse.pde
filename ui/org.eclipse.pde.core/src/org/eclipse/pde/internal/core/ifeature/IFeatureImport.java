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
import org.eclipse.pde.core.plugin.IPluginReference;

public interface IFeatureImport extends IFeatureObject, IPluginReference {
	String P_TYPE = "type"; //$NON-NLS-1$

	String P_PATCH = "patch"; //$NON-NLS-1$

	String P_ID_MATCH = "id-match"; //$NON-NLS-1$

	int PLUGIN = 0;

	int FEATURE = 1;

	int getType();

	void setType(int type) throws CoreException;

	boolean isPatch();

	void setPatch(boolean patch) throws CoreException;

	int getIdMatch();

	void setIdMatch(int idMatch) throws CoreException;

	String getFilter();

	void setFilter(String filter) throws CoreException;
}
