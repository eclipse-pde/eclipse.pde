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
package org.eclipse.pde.internal.core.product;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArgumentsInfo extends ProductObject implements IArgumentsInfo {

	private static final long serialVersionUID = 1L;
	private String fProgramArgs = ""; //$NON-NLS-1$
	private String fProgramArgsLin = ""; //$NON-NLS-1$
	private String fProgramArgsMac = ""; //$NON-NLS-1$
	private String fProgramArgsSol = ""; //$NON-NLS-1$
	private String fProgramArgsWin = ""; //$NON-NLS-1$

	private String fVMArgs = ""; //$NON-NLS-1$
	private String fVMArgsLin = ""; //$NON-NLS-1$
	private String fVMArgsMac = ""; //$NON-NLS-1$
	private String fVMArgsSol = ""; //$NON-NLS-1$
	private String fVMArgsWin = ""; //$NON-NLS-1$

	public ArgumentsInfo(IProductModel model) {
		super(model);
	}

	public void setProgramArguments(String args, int platform) {
		String old;
		if (args == null)
			args = ""; //$NON-NLS-1$
		switch (platform) {
			case L_ARGS_ALL :
				old = fProgramArgs;
				fProgramArgs = args;
				if (isEditable())
					firePropertyChanged(P_PROG_ARGS, old, fProgramArgs);
				break;
			case L_ARGS_LINUX :
				old = fProgramArgsLin;
				fProgramArgsLin = args;
				if (isEditable())
					firePropertyChanged(P_PROG_ARGS_LIN, old, fProgramArgsLin);
				break;
			case L_ARGS_MACOS :
				old = fProgramArgsMac;
				fProgramArgsMac = args;
				if (isEditable())
					firePropertyChanged(P_PROG_ARGS_MAC, old, fProgramArgsMac);
				break;
			case L_ARGS_SOLAR :
				old = fProgramArgsSol;
				fProgramArgsSol = args;
				if (isEditable())
					firePropertyChanged(P_PROG_ARGS_SOL, old, fProgramArgsSol);
				break;
			case L_ARGS_WIN32 :
				old = fProgramArgsWin;
				fProgramArgsWin = args;
				if (isEditable())
					firePropertyChanged(P_PROG_ARGS_WIN, old, fProgramArgsWin);
				break;
		}
	}

	public String getProgramArguments(int platform) {
		switch (platform) {
			case L_ARGS_ALL :
				return fProgramArgs;
			case L_ARGS_LINUX :
				return fProgramArgsLin;
			case L_ARGS_MACOS :
				return fProgramArgsMac;
			case L_ARGS_SOLAR :
				return fProgramArgsSol;
			case L_ARGS_WIN32 :
				return fProgramArgsWin;
		}
		return ""; //$NON-NLS-1$
	}

	public String getCompleteProgramArguments(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return getCompleteArgs(getProgramArguments(L_ARGS_WIN32), fProgramArgs);
		} else if (Platform.OS_LINUX.equals(os)) {
			return getCompleteArgs(getProgramArguments(L_ARGS_LINUX), fProgramArgs);
		} else if (Platform.OS_MACOSX.equals(os)) {
			return getCompleteArgs(getProgramArguments(L_ARGS_MACOS), fProgramArgs);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return getCompleteArgs(getProgramArguments(L_ARGS_SOLAR), fProgramArgs);
		} else {
			return getProgramArguments(L_ARGS_ALL);
		}
	}

	public void setVMArguments(String args, int platform) {
		String old;
		if (args == null)
			args = ""; //$NON-NLS-1$
		switch (platform) {
			case L_ARGS_ALL :
				old = fVMArgs;
				fVMArgs = args;
				if (isEditable())
					firePropertyChanged(P_VM_ARGS, old, fVMArgs);
				break;
			case L_ARGS_LINUX :
				old = fVMArgsLin;
				fVMArgsLin = args;
				if (isEditable())
					firePropertyChanged(P_VM_ARGS_LIN, old, fVMArgsLin);
				break;
			case L_ARGS_MACOS :
				old = fVMArgsMac;
				fVMArgsMac = args;
				if (isEditable())
					firePropertyChanged(P_VM_ARGS_MAC, old, fVMArgsMac);
				break;
			case L_ARGS_SOLAR :
				old = fVMArgsSol;
				fVMArgsSol = args;
				if (isEditable())
					firePropertyChanged(P_VM_ARGS_SOL, old, fVMArgsSol);
				break;
			case L_ARGS_WIN32 :
				old = fVMArgsWin;
				fVMArgsWin = args;
				if (isEditable())
					firePropertyChanged(P_VM_ARGS_WIN, old, fVMArgsWin);
				break;
		}
	}

	public String getVMArguments(int platform) {
		switch (platform) {
			case L_ARGS_ALL :
				return fVMArgs;
			case L_ARGS_LINUX :
				return fVMArgsLin;
			case L_ARGS_MACOS :
				return fVMArgsMac;
			case L_ARGS_SOLAR :
				return fVMArgsSol;
			case L_ARGS_WIN32 :
				return fVMArgsWin;
		}
		return ""; //$NON-NLS-1$
	}

	public String getCompleteVMArguments(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return getCompleteArgs(getVMArguments(L_ARGS_WIN32), fVMArgs);
		} else if (Platform.OS_LINUX.equals(os)) {
			return getCompleteArgs(getVMArguments(L_ARGS_LINUX), fVMArgs);
		} else if (Platform.OS_MACOSX.equals(os)) {
			return getCompleteArgs(getVMArguments(L_ARGS_MACOS), fVMArgs);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return getCompleteArgs(getVMArguments(L_ARGS_SOLAR), fVMArgs);
		} else {
			return getVMArguments(L_ARGS_ALL);
		}
	}

	private String getCompleteArgs(String platformArgs, String univArgs) {
		String args = platformArgs;
		if (univArgs.length() > 0)
			args = univArgs + " " + args; //$NON-NLS-1$
		return args.trim();
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(P_PROG_ARGS)) {
					fProgramArgs = getText(child);
				} else if (child.getNodeName().equals(P_PROG_ARGS_LIN)) {
					fProgramArgsLin = getText(child);
				} else if (child.getNodeName().equals(P_PROG_ARGS_MAC)) {
					fProgramArgsMac = getText(child);
				} else if (child.getNodeName().equals(P_PROG_ARGS_SOL)) {
					fProgramArgsSol = getText(child);
				} else if (child.getNodeName().equals(P_PROG_ARGS_WIN)) {
					fProgramArgsWin = getText(child);
				} else if (child.getNodeName().equals(P_VM_ARGS)) {
					fVMArgs = getText(child);
				} else if (child.getNodeName().equals(P_VM_ARGS_LIN)) {
					fVMArgsLin = getText(child);
				} else if (child.getNodeName().equals(P_VM_ARGS_MAC)) {
					fVMArgsMac = getText(child);
				} else if (child.getNodeName().equals(P_VM_ARGS_SOL)) {
					fVMArgsSol = getText(child);
				} else if (child.getNodeName().equals(P_VM_ARGS_WIN)) {
					fVMArgsWin = getText(child);
				}
			}
		}
	}

	private String getText(Node node) {
		node.normalize();
		Node text = node.getFirstChild();
		if (text != null && text.getNodeType() == Node.TEXT_NODE) {
			return text.getNodeValue();
		}
		return ""; //$NON-NLS-1$
	}

	public void write(String indent, java.io.PrintWriter writer) {
		writer.println(indent + "<launcherArgs>"); //$NON-NLS-1$
		if (fProgramArgs.length() > 0) {
			writer.println(indent + "   " + "<" + P_PROG_ARGS + ">" + getWritableString(fProgramArgs) + "</" + P_PROG_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fProgramArgsLin.length() > 0) {
			writer.println(indent + "   " + "<" + P_PROG_ARGS_LIN + ">" + getWritableString(fProgramArgsLin) + "</" + P_PROG_ARGS_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fProgramArgsMac.length() > 0) {
			writer.println(indent + "   " + "<" + P_PROG_ARGS_MAC + ">" + getWritableString(fProgramArgsMac) + "</" + P_PROG_ARGS_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fProgramArgsSol.length() > 0) {
			writer.println(indent + "   " + "<" + P_PROG_ARGS_SOL + ">" + getWritableString(fProgramArgsSol) + "</" + P_PROG_ARGS_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fProgramArgsWin.length() > 0) {
			writer.println(indent + "   " + "<" + P_PROG_ARGS_WIN + ">" + getWritableString(fProgramArgsWin) + "</" + P_PROG_ARGS_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fVMArgs.length() > 0) {
			writer.println(indent + "   " + "<" + P_VM_ARGS + ">" + getWritableString(fVMArgs) + "</" + P_VM_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fVMArgsLin.length() > 0) {
			writer.println(indent + "   " + "<" + P_VM_ARGS_LIN + ">" + getWritableString(fVMArgsLin) + "</" + P_VM_ARGS_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fVMArgsMac.length() > 0) {
			writer.println(indent + "   " + "<" + P_VM_ARGS_MAC + ">" + getWritableString(fVMArgsMac) + "</" + P_VM_ARGS_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fVMArgsSol.length() > 0) {
			writer.println(indent + "   " + "<" + P_VM_ARGS_SOL + ">" + getWritableString(fVMArgsSol) + "</" + P_VM_ARGS_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fVMArgsWin.length() > 0) {
			writer.println(indent + "   " + "<" + P_VM_ARGS_WIN + ">" + getWritableString(fVMArgsWin) + "</" + P_VM_ARGS_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		writer.println(indent + "</launcherArgs>"); //$NON-NLS-1$
	}

}
