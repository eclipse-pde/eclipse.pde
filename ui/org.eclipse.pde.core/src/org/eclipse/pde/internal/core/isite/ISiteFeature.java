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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IVersionable;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface ISiteFeature extends IVersionable, ISiteObject {
	String P_TYPE = "type";
	String P_URL = "url";
	String P_OS = "os";
	String P_WS = "ws";
	String P_NL = "nl";
	String P_ARCH = "arch";
	String P_PATCH = "patch";
	void addCategories(ISiteCategory[] categories) throws CoreException;
	void removeCategories(ISiteCategory[] categories) throws CoreException;
	ISiteCategory [] getCategories();
	String getType();
	String getURL();
	String getOS();
	String getNL();
	String getArch();
	String getWS();
	boolean isPatch();
	void setType(String type) throws CoreException;
	void setURL(String url) throws CoreException;
	void setOS(String os) throws CoreException;
	void setWS(String ws) throws CoreException;
	void setArch(String arch) throws CoreException;
	void setNL(String nl) throws CoreException;
	void setIsPatch(boolean patch) throws CoreException;
	IFile getArchiveFile();
}
