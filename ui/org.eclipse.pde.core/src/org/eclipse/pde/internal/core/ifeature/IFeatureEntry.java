/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576890: Ignore included features/plug-ins not matching target-environment
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;

/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureEntry extends IFeatureObject, IIdentifiable, IEnvironment {
	String P_OS = "p_os"; //$NON-NLS-1$
	String P_WS = "p_ws"; //$NON-NLS-1$
	String P_NL = "p_nl"; //$NON-NLS-1$
	String P_ARCH = "p_arch"; //$NON-NLS-1$
	String P_FILTER = "p_filter"; //$NON-NLS-1$
	String P_DOWNLOAD_SIZE = "p_download_size"; //$NON-NLS-1$
	String P_INSTALL_SIZE = "p_install_size"; //$NON-NLS-1$

	/**
	 * Returns an LDAP filter that must be satisfied for this entry
	 */
	String getFilter();

	/**
	 * 	Returns estimated download size of this plug-in.
	 */
	long getDownloadSize();

	/**
	 * Returns estimated size of this plug-in when installed.
	 */
	long getInstallSize();

	/**
	 * Sets an LDAP filter on this plugin
	 */
	void setFilter(String filter) throws CoreException;

	/**
	 * 	Sets the estimated download size of this plug-in.
	 */
	void setDownloadSize(long size) throws CoreException;

	/**
	 * Sets the estimated size of this plug-in when installed.
	 */
	void setInstallSize(long size) throws CoreException;
}
