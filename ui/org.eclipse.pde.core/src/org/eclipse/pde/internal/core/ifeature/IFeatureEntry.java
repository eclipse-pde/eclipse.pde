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
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureEntry extends IFeatureObject, IIdentifiable {
	String P_OS = "p_os";
	String P_WS = "p_ws";
	String P_NL = "p_nl";
	String P_ARCH = "p_arch";
	String P_DOWNLOAD_SIZE = "p_download_size";
	String P_INSTALL_SIZE = "p_install_size";

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
	 * 	Sets the estimated download size of this plug-in.
	 */
	public void setDownloadSize(long size) throws CoreException;
	/**
	 * Sets the estimated size of this plug-in when installed.
	 */
	public void setInstallSize(long size) throws CoreException;
}
