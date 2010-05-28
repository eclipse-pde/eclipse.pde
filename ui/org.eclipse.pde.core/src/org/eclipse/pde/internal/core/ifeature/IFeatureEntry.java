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

/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureEntry extends IFeatureObject, IIdentifiable {
	String P_OS = "p_os"; //$NON-NLS-1$
	String P_WS = "p_ws"; //$NON-NLS-1$
	String P_NL = "p_nl"; //$NON-NLS-1$
	String P_ARCH = "p_arch"; //$NON-NLS-1$
	String P_FILTER = "p_filter"; //$NON-NLS-1$
	String P_DOWNLOAD_SIZE = "p_download_size"; //$NON-NLS-1$
	String P_INSTALL_SIZE = "p_install_size"; //$NON-NLS-1$

	/**
	 * Returns a comma-separated list of the operating systems this plug-in supports.
	 */
	public String getOS();

	/**
	 * Returns a comma-separated list of the window systems this plug-in supports.
	 */
	public String getWS();

	/**
	 * Returns a comma-separated list of the locales this plug-in supports.
	 */
	public String getNL();

	/**
	 * Returns a comma-separated list of the architecture this plug-in supports.
	 */
	public String getArch();

	/**
	 * Returns an LDAP filter that must be satisfied for this entry 
	 */
	public String getFilter();

	/**
	 * 	Returns estimated download size of this plug-in.
	 */
	public long getDownloadSize();

	/**
	 * Returns estimated size of this plug-in when installed.
	 */
	public long getInstallSize();

	/**
	 * Sets a comma-separated list of the operating systems this plug-in supports.
	 */
	public void setOS(String os) throws CoreException;

	/**
	 * Sets a comma-separated list of the window systems this plug-in supports.
	 */
	public void setWS(String ws) throws CoreException;

	/**
	 * Sets a comma-separated list of the locales this plug-in supports.
	 */
	public void setNL(String nl) throws CoreException;

	/**
	 * Sets a comma-separated list of the archiecture this plug-in supports.
	 */
	public void setArch(String arch) throws CoreException;

	/**
	 * Sets an LDAP filter on this plugin
	 */
	public void setFilter(String filter) throws CoreException;

	/**
	 * 	Sets the estimated download size of this plug-in.
	 */
	public void setDownloadSize(long size) throws CoreException;

	/**
	 * Sets the estimated size of this plug-in when installed.
	 */
	public void setInstallSize(long size) throws CoreException;
}
