package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface IEnvironment {
	static final String P_OS = "os";
	static final String P_WS = "ws";
	static final String P_ARCH = "arch";
	
	String getOS();
	String getWS();
	String getArch();
	
	void setOS(String os) throws CoreException;
	void setWS(String ws) throws CoreException;
	void setArch(String arch) throws CoreException;
}
