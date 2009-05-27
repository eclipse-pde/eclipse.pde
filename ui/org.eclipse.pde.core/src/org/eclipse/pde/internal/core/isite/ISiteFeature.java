/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IVersionable;

public interface ISiteFeature extends IVersionable, ISiteObject {
	String P_TYPE = "type"; //$NON-NLS-1$
	String P_URL = "url"; //$NON-NLS-1$
	String P_OS = "os"; //$NON-NLS-1$
	String P_WS = "ws"; //$NON-NLS-1$
	String P_NL = "nl"; //$NON-NLS-1$
	String P_ARCH = "arch"; //$NON-NLS-1$
	String P_PATCH = "patch"; //$NON-NLS-1$

	void addCategories(ISiteCategory[] categories) throws CoreException;

	void removeCategories(ISiteCategory[] categories) throws CoreException;

	ISiteCategory[] getCategories();

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
