/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IJREInfo extends IProductObject {

	public static final String JRE_LIN = "linux"; //$NON-NLS-1$
	public static final String JRE_MAC = "macos"; //$NON-NLS-1$
	public static final String JRE_SOL = "solaris"; //$NON-NLS-1$
	public static final String JRE_WIN = "windows"; //$NON-NLS-1$

	public static final String JRE = "jre"; //$NON-NLS-1$
	public static final String EE = "ee"; //$NON-NLS-1$

	public static final int TYPE_JRE = 0;
	public static final int TYPE_EE = 1;

	public static final int LINUX = 0;
	public static final int MACOS = 1;
	public static final int SOLAR = 2;
	public static final int WIN32 = 3;

	public String getJVM(int platform);

	public int getJVMType(int platform);

	public String getJVMLocation(String os);

	public void setJVM(String args, int platform, int type);

}
