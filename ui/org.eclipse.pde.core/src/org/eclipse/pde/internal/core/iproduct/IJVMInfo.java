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

public interface IJVMInfo extends IProductObject {
	
	public static final String JVM_LIN = "jvmLocationLin"; //$NON-NLS-1$
	public static final String JVM_MAC = "jvmLocationMac"; //$NON-NLS-1$
	public static final String JVM_SOL = "jvmLocationSol"; //$NON-NLS-1$
	public static final String JVM_WIN = "jvmLocationWin"; //$NON-NLS-1$$
	
	public static final int LINUX = 0;
	public static final int MACOS = 1;
	public static final int SOLAR = 2;
	public static final int WIN32 = 3;
	
	void setJVM(String args, int platform);
	
	String getJVM(int platform);
	
}
