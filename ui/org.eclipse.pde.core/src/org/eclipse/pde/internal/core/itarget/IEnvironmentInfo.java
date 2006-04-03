/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.itarget;

public interface IEnvironmentInfo extends ITargetObject{
	
	public static final String P_OS = "os"; //$NON-NLS-1$
	public static final String P_WS = "ws"; //$NON-NLS-1$
	public static final String P_ARCH = "arch"; //$NON-NLS-1$
	public static final String P_NL = "nl"; //$NON-NLS-1$
	
	public String getOS();
	
	public String getWS();
	
	public String getArch();
	
	public String getNL();
	
	public void setOS(String os);
	
	public void setWS(String ws);
	
	public void setArch(String arch);
	
	public void setNL(String nl);
	
	public String getDisplayOS();
	
	public String getDisplayWS();
	
	public String getDisplayArch();
	
	public String getDisplayNL();

}
