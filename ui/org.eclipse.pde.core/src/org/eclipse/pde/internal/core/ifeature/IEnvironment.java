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
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;

public interface IEnvironment {
	static final String P_OS = "os"; //$NON-NLS-1$
	static final String P_WS = "ws"; //$NON-NLS-1$
	static final String P_ARCH = "arch"; //$NON-NLS-1$
	static final String P_NL = "nl"; //$NON-NLS-1$

	String getOS();

	String getWS();

	String getArch();

	String getNL();

	void setOS(String os) throws CoreException;

	void setWS(String ws) throws CoreException;

	void setArch(String arch) throws CoreException;

	void setNL(String nl) throws CoreException;
}
