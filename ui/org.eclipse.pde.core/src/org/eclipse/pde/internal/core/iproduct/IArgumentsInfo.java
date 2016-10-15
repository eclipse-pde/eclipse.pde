/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IArgumentsInfo extends IProductObject {

	public static final String P_PROG_ARGS = "programArgs"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_LIN = "programArgsLin"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_MAC = "programArgsMac"; //$NON-NLS-1$
	public static final String P_PROG_ARGS_WIN = "programArgsWin"; //$NON-NLS-1$

	public static final String P_VM_ARGS = "vmArgs"; //$NON-NLS-1$
	public static final String P_VM_ARGS_LIN = "vmArgsLin"; //$NON-NLS-1$
	public static final String P_VM_ARGS_MAC = "vmArgsMac"; //$NON-NLS-1$
	public static final String P_VM_ARGS_WIN = "vmArgsWin"; //$NON-NLS-1$

	public static final String P_ARGS_ARCH_X86 = "argsX86"; //$NON-NLS-1$
	public static final String P_ARGS_ARCH_X86_64 = "argsX86_64"; //$NON-NLS-1$
	public static final String P_ARGS_ARCH_PPC = "argsPPC"; //$NON-NLS-1$
	public static final String P_ARGS_ARCH_IA_64 = "argsIA_64"; //$NON-NLS-1$
	public static final String P_ARGS_ARCH_IA_64_32 = "argsIA_64_32"; //$NON-NLS-1$
	public static final String P_ARGS_ARCH_PA_RISC = "argsPA_RISC"; //$NON-NLS-1$
	public static final String P_ARGS_ARCH_SPARC = "argsSPARC"; //$NON-NLS-1$

	public static final int L_ARGS_ALL = 0;
	public static final int L_ARGS_LINUX = 1;
	public static final int L_ARGS_MACOS = 2;
	public static final int L_ARGS_WIN32 = 3;

	public static final int L_ARGS_ARCH_ALL = 0;
	public static final int L_ARGS_ARCH_X86 = 1;
	public static final int L_ARGS_ARCH_X86_64 = 2;
	public static final int L_ARGS_ARCH_PPC = 3;
	public static final int L_ARGS_ARCH_IA_64 = 4;
	public static final int L_ARGS_ARCH_IA_64_32 = 5;
	public static final int L_ARGS_ARCH_PA_RISC = 6;
	public static final int L_ARGS_ARCH_SPARC = 7;

	void setProgramArguments(String args, int platform);

	void setProgramArguments(String args, int platform, int arch);

	String getProgramArguments(int platform);

	String getProgramArguments(int platform, int arch);

	String getCompleteProgramArguments(String os);

	String getCompleteProgramArguments(String os, String arch);

	void setVMArguments(String args, int platform);

	void setVMArguments(String args, int platform, int arch);

	String getVMArguments(int platform);

	String getVMArguments(int platform, int arch);

	String getCompleteVMArguments(String os);

	String getCompleteVMArguments(String os, String arch);
}
