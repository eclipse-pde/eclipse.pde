package org.eclipse.pde.internal.ui.model.ifeature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
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
	public int getDownloadSize();
	/**
	 * Returns estimated size of this plug-in when installed.
	 */
	public int getInstallSize();
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
	public void setDownloadSize(int size) throws CoreException;
	/**
	 * Sets the estimated size of this plug-in when installed.
	 */
	public void setInstallSize(int size) throws CoreException;
}