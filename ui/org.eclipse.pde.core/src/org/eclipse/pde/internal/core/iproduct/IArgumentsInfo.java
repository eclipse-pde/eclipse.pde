/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IArgumentsInfo extends IProductObject {

	public static final String P_PROG_ARGS = "programArgs"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_LIN = "programArgsLin"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_MAC = "programArgsMac"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_SOL = "programArgsSol"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_WIN = "programArgsWin"; //$NON-NLS-1$

	public static final String P_VM_ARGS = "vmArgs"; //$NON-NLS-1$
	public static final String P_VM_ARGS_LIN = "vmArgsLin"; //$NON-NLS-1$
	public static final String P_VM_ARGS_MAC = "vmArgsMac"; //$NON-NLS-1$
	public static final String P_VM_ARGS_SOL = "vmArgsSol"; //$NON-NLS-1$
	public static final String P_VM_ARGS_WIN = "vmArgsWin"; //$NON-NLS-1$

	public static final int L_ARGS_ALL = 0;
	public static final int L_ARGS_LINUX = 1;
	public static final int L_ARGS_MACOS = 2;
	public static final int L_ARGS_SOLAR = 3;
	public static final int L_ARGS_WIN32 = 4;

	void setProgramArguments(String args, int platform);

	String getProgramArguments(int platform);

	String getCompleteProgramArguments(String os);

	void setVMArguments(String args, int platform);

	String getVMArguments(int platform);

	String getCompleteVMArguments(String os);
}
