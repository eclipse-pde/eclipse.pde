package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeaturePlugin extends IFeatureObject, IVersonable {
	String P_OS = "p_os";
	String P_WS = "p_ws";
	String P_NL = "p_nl";
	String P_DOWNLOAD_SIZE = "p_download_size";
	String P_INSTALL_SIZE = "p_install_size";

	/**
	 * Returns whether this is a reference to a fragment.
	 * @return <samp>true</samp> if this is a fragment, <samp>false</samp> otherwise.
	 */
	public boolean isFragment();
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
	 * 	Returns estimated download size of this plug-in.
	 */
	public int getDownloadSize();
	/**
	 * Returns estimated size of this plug-in when installed.
	 */
	public int getInstallSize();

	/**
	 * 
	 */
	public void setFragment(boolean fragment) throws CoreException;
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
	 * 	Sets the estimated download size of this plug-in.
	 */
	public void setDownloadSize(int size) throws CoreException;
	/**
	 * Sets the estimated size of this plug-in when installed.
	 */
	public void setInstallSize(int size) throws CoreException;
}