/*******************************************************************************
 *  Copyright (c) 2005, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArgumentsInfo extends ProductObject implements IArgumentsInfo {

	private static final long serialVersionUID = 1L;
	private final String[] fProgramArgs = new String[8];
	private final String[] fProgramArgsFbsd = new String[8];
	private final String[] fProgramArgsLin = new String[8];
	private final String[] fProgramArgsMac = new String[8];
	private final String[] fProgramArgsSol = new String[8];
	private final String[] fProgramArgsWin = new String[8];

	private final String[] fVMArgs = new String[8];
	private final String[] fVMArgsFbsd = new String[8];
	private final String[] fVMArgsLin = new String[8];
	private final String[] fVMArgsMac = new String[8];
	private final String[] fVMArgsSol = new String[8];
	private final String[] fVMArgsWin = new String[8];

	public ArgumentsInfo(IProductModel model) {
		super(model);
		this.initializeArgs(fProgramArgs);
		this.initializeArgs(fProgramArgsFbsd);
		this.initializeArgs(fProgramArgsLin);
		this.initializeArgs(fProgramArgsMac);
		this.initializeArgs(fProgramArgsSol);
		this.initializeArgs(fProgramArgsWin);
		this.initializeArgs(fVMArgs);
		this.initializeArgs(fVMArgsFbsd);
		this.initializeArgs(fVMArgsLin);
		this.initializeArgs(fVMArgsMac);
		this.initializeArgs(fVMArgsSol);
		this.initializeArgs(fVMArgsWin);
	}

	private void initializeArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			args[i] = ""; //$NON-NLS-1$
		}
	}

	@Override
	public void setProgramArguments(String args, int platform) {
		setProgramArguments(args, platform, L_ARGS_ARCH_ALL);
	}

	@Override
	public void setProgramArguments(String args, int platform, int arch) {
		String old;
		if (args == null) {
			args = ""; //$NON-NLS-1$
		}
		switch (platform) {
			case L_ARGS_ALL :
				old = fProgramArgs[arch];
				fProgramArgs[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_PROG_ARGS, old, fProgramArgs[arch]);
				}
				break;
			case L_ARGS_FREEBSD :
				old = fProgramArgsFbsd[arch];
				fProgramArgsFbsd[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_PROG_ARGS_FBSD, old, fProgramArgsFbsd[arch]);
				}
				break;
			case L_ARGS_LINUX :
				old = fProgramArgsLin[arch];
				fProgramArgsLin[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_PROG_ARGS_LIN, old, fProgramArgsLin[arch]);
				}
				break;
			case L_ARGS_MACOS :
				old = fProgramArgsMac[arch];
				fProgramArgsMac[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_PROG_ARGS_MAC, old, fProgramArgsMac[arch]);
				}
				break;
			case L_ARGS_WIN32 :
				old = fProgramArgsWin[arch];
				fProgramArgsWin[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_PROG_ARGS_WIN, old, fProgramArgsWin[arch]);
				}
				break;
		}
	}

	@Override
	public String getProgramArguments(int platform) {
		return getProgramArguments(platform, L_ARGS_ARCH_ALL);
	}

	@Override
	public String getProgramArguments(int platform, int arch) {
		switch (platform) {
			case L_ARGS_ALL :
				return fProgramArgs[arch];
			case L_ARGS_FREEBSD :
				return fProgramArgsFbsd[arch];
			case L_ARGS_LINUX :
				return fProgramArgsLin[arch];
			case L_ARGS_MACOS :
				return fProgramArgsMac[arch];
			case L_ARGS_WIN32 :
				return fProgramArgsWin[arch];
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getCompleteProgramArguments(String os) {
		return getCompleteProgramArguments(os, ""); //$NON-NLS-1$
	}

	@Override
	public String getCompleteProgramArguments(String os, String arch) {
		int archIndex = L_ARGS_ARCH_ALL;
		if (arch != null && arch.length() > 0) {
			if (ARCH_X86.equals(arch)) {
				archIndex = L_ARGS_ARCH_X86;
			} else if (Platform.ARCH_X86_64.equals(arch)) {
				archIndex = L_ARGS_ARCH_X86_64;
			}
		}
		String archArgsAllPlatforms = archIndex > 0 ? getProgramArguments(L_ARGS_ALL, archIndex) : ""; //$NON-NLS-1$
		String archArgs;
		if (Platform.OS_WIN32.equals(os)) {
			archArgs = archIndex > 0 ? getProgramArguments(L_ARGS_WIN32, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getProgramArguments(L_ARGS_WIN32), fProgramArgs[L_ARGS_ARCH_ALL]);
		} else if (Platform.OS_FREEBSD.equals(os)) {
			archArgs = archIndex > 0 ? getProgramArguments(L_ARGS_FREEBSD, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getProgramArguments(L_ARGS_FREEBSD), fProgramArgs[L_ARGS_ARCH_ALL]);
		} else if (Platform.OS_LINUX.equals(os)) {
			archArgs = archIndex > 0 ? getProgramArguments(L_ARGS_LINUX, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getProgramArguments(L_ARGS_LINUX), fProgramArgs[L_ARGS_ARCH_ALL]);
		} else if (Platform.OS_MACOSX.equals(os)) {
			archArgs = archIndex > 0 ? getProgramArguments(L_ARGS_MACOS, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getProgramArguments(L_ARGS_MACOS), fProgramArgs[L_ARGS_ARCH_ALL]);
		} else {
			return getCompleteArgs(archArgsAllPlatforms, "", fProgramArgs[L_ARGS_ALL]); //$NON-NLS-1$
		}
	}

	@Override
	public void setVMArguments(String args, int platform) {
		setVMArguments(args, platform, L_ARGS_ARCH_ALL);
	}

	@Override
	public void setVMArguments(String args, int platform, int arch) {
		String old;
		if (args == null) {
			args = ""; //$NON-NLS-1$
		}
		switch (platform) {
			case L_ARGS_ALL :
				old = fVMArgs[arch];
				fVMArgs[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_VM_ARGS, old, fVMArgs[arch]);
				}
				break;
			case L_ARGS_FREEBSD :
				old = fVMArgsFbsd[arch];
				fVMArgsFbsd[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_VM_ARGS_FBSD, old, fVMArgsFbsd[arch]);
				}
				break;
			case L_ARGS_LINUX :
				old = fVMArgsLin[arch];
				fVMArgsLin[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_VM_ARGS_LIN, old, fVMArgsLin[arch]);
				}
				break;
			case L_ARGS_MACOS :
				old = fVMArgsMac[arch];
				fVMArgsMac[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_VM_ARGS_MAC, old, fVMArgsMac[arch]);
				}
				break;
			case L_ARGS_WIN32 :
				old = fVMArgsWin[arch];
				fVMArgsWin[arch] = args;
				if (isEditable()) {
					firePropertyChanged(P_VM_ARGS_WIN, old, fVMArgsWin[arch]);
				}
				break;
		}
	}

	@Override
	public String getVMArguments(int platform) {
		return getVMArguments(platform, L_ARGS_ARCH_ALL);
	}

	@Override
	public String getVMArguments(int platform, int arch) {
		switch (platform) {
			case L_ARGS_ALL :
				return fVMArgs[arch];
			case L_ARGS_FREEBSD :
				return fVMArgsFbsd[arch];
			case L_ARGS_LINUX :
				return fVMArgsLin[arch];
			case L_ARGS_MACOS :
				return fVMArgsMac[arch];
			case L_ARGS_WIN32 :
				return fVMArgsWin[arch];
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getCompleteVMArguments(String os) {
		return getCompleteVMArguments(os, ""); //$NON-NLS-1$
	}

	@Override
	public String getCompleteVMArguments(String os, String arch) {
		int archIndex = L_ARGS_ARCH_ALL;
		if (arch != null && arch.length() > 0) {
			if (ARCH_X86.equals(arch)) {
				archIndex = L_ARGS_ARCH_X86;
			} else if (Platform.ARCH_X86_64.equals(arch)) {
				archIndex = L_ARGS_ARCH_X86_64;
			}
		}

		String archArgsAllPlatforms = archIndex > 0 ? getVMArguments(L_ARGS_ALL, archIndex) : ""; //$NON-NLS-1$
		String archArgs;
		if (Platform.OS_WIN32.equals(os)) {
			archArgs = archIndex > 0 ? getVMArguments(L_ARGS_WIN32, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getVMArguments(L_ARGS_WIN32), fVMArgs[L_ARGS_ARCH_ALL]);
		} else if (Platform.OS_FREEBSD.equals(os)) {
			archArgs = archIndex > 0 ? getVMArguments(L_ARGS_FREEBSD, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getVMArguments(L_ARGS_FREEBSD), fVMArgs[L_ARGS_ARCH_ALL]);
		} else if (Platform.OS_LINUX.equals(os)) {
			archArgs = archIndex > 0 ? getVMArguments(L_ARGS_LINUX, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getVMArguments(L_ARGS_LINUX), fVMArgs[L_ARGS_ARCH_ALL]);
		} else if (Platform.OS_MACOSX.equals(os)) {
			archArgs = archIndex > 0 ? getVMArguments(L_ARGS_MACOS, archIndex) + " " + archArgsAllPlatforms : archArgsAllPlatforms; //$NON-NLS-1$
			return getCompleteArgs(archArgs, getVMArguments(L_ARGS_MACOS), fVMArgs[L_ARGS_ARCH_ALL]);
		} else {
			return getCompleteArgs(archArgsAllPlatforms, "", fVMArgs[L_ARGS_ARCH_ALL]); //$NON-NLS-1$
		}
	}

	private String getCompleteArgs(String archArgs, String platformArgs, String univArgs) {
		String args = archArgs;
		if (platformArgs.length() > 0) {
			args = platformArgs + " " + args; //$NON-NLS-1$
		}
		if (univArgs.length() > 0) {
			args = univArgs + " " + args; //$NON-NLS-1$
		}
		return args.trim();
	}

	@Override
	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			String[] parentArgs = fProgramArgs;
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				switch (child.getNodeName()) {
				case P_PROG_ARGS:
					parentArgs = fProgramArgs;
					fProgramArgs[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_PROG_ARGS_FBSD:
					parentArgs = fProgramArgsFbsd;
					fProgramArgsFbsd[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_PROG_ARGS_LIN:
					parentArgs = fProgramArgsLin;
					fProgramArgsLin[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_PROG_ARGS_MAC:
					parentArgs = fProgramArgsMac;
					fProgramArgsMac[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_PROG_ARGS_WIN:
					parentArgs = fProgramArgsWin;
					fProgramArgsWin[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_VM_ARGS:
					parentArgs = fVMArgs;
					fVMArgs[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_VM_ARGS_FBSD:
					parentArgs = fVMArgsFbsd;
					fVMArgsFbsd[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_VM_ARGS_LIN:
					parentArgs = fVMArgsLin;
					fVMArgsLin[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_VM_ARGS_MAC:
					parentArgs = fVMArgsMac;
					fVMArgsMac[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				case P_VM_ARGS_WIN:
					parentArgs = fVMArgsWin;
					fVMArgsWin[L_ARGS_ARCH_ALL] = getText(child).trim();
					break;
				default:
					break;
				}
				// Look for child nodes which would be arch specific.
				NodeList childNodes = child.getChildNodes();
				for (int j = 0; j < childNodes.getLength(); j++) {
					Node arch = childNodes.item(j);
					if (arch.getNodeType() == Node.ELEMENT_NODE) {
						if (arch.getNodeName().equals(P_ARGS_ARCH_X86)) {
							parentArgs[L_ARGS_ARCH_X86] = getText(arch).trim();
						} else if (arch.getNodeName().equals(P_ARGS_ARCH_X86_64)) {
							parentArgs[L_ARGS_ARCH_X86_64] = getText(arch).trim();
						}
					}
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

	@Override
	public void write(String indent, java.io.PrintWriter writer) {
		writer.println(indent + "<launcherArgs>"); //$NON-NLS-1$
		String subIndent = indent + "   "; //$NON-NLS-1$
		if (hasArgs(fProgramArgs)) {
			writer.print(subIndent + "<" + P_PROG_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fProgramArgs[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fProgramArgs[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fProgramArgs, subIndent, writer);
			writer.println(subIndent + "</" + P_PROG_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fProgramArgsFbsd)) {
			writer.print(subIndent + "<" + P_PROG_ARGS_FBSD + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fProgramArgsFbsd[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fProgramArgsFbsd[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fProgramArgsFbsd, subIndent, writer);
			writer.println(subIndent + "</" + P_PROG_ARGS_FBSD + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fProgramArgsLin)) {
			writer.print(subIndent + "<" + P_PROG_ARGS_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fProgramArgsLin[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fProgramArgsLin[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fProgramArgsLin, subIndent, writer);
			writer.println(subIndent + "</" + P_PROG_ARGS_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fProgramArgsMac)) {
			writer.print(subIndent + "<" + P_PROG_ARGS_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fProgramArgsMac[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fProgramArgsMac[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fProgramArgsMac, subIndent, writer);
			writer.println(subIndent + "</" + P_PROG_ARGS_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fProgramArgsWin)) {
			writer.print(subIndent + "<" + P_PROG_ARGS_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fProgramArgsWin[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fProgramArgsWin[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fProgramArgsWin, subIndent, writer);
			writer.println(subIndent + "</" + P_PROG_ARGS_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fVMArgs)) {
			writer.print(subIndent + "<" + P_VM_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fVMArgs[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fVMArgs[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fVMArgs, subIndent, writer);
			writer.println(subIndent + "</" + P_VM_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fVMArgsFbsd)) {
			writer.print(subIndent + "<" + P_VM_ARGS_FBSD + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fVMArgsFbsd[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fVMArgsFbsd[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fVMArgsFbsd, subIndent, writer);
			writer.println(subIndent + "</" + P_VM_ARGS_FBSD + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fVMArgsLin)) {
			writer.print(subIndent + "<" + P_VM_ARGS_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fVMArgsLin[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fVMArgsLin[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fVMArgsLin, subIndent, writer);
			writer.println(subIndent + "</" + P_VM_ARGS_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fVMArgsMac)) {
			writer.print(subIndent + "<" + P_VM_ARGS_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fVMArgsMac[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fVMArgsMac[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fVMArgsMac, subIndent, writer);
			writer.println(subIndent + "</" + P_VM_ARGS_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (hasArgs(fVMArgsWin)) {
			writer.print(subIndent + "<" + P_VM_ARGS_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			if (fVMArgsWin[L_ARGS_ARCH_ALL].length() > 0) {
				writer.print(getWritableString(fVMArgsWin[L_ARGS_ARCH_ALL]));
			}
			writer.println();
			writeArchArgs(fVMArgsWin, subIndent, writer);
			writer.println(subIndent + "</" + P_VM_ARGS_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(indent + "</launcherArgs>"); //$NON-NLS-1$
	}

	private boolean hasArgs(String[] args) {
		for (String arg : args) {
			if (arg.length() > 0) {
				return true;
			}
		}
		return false;
	}

	private void writeArchArgs(String[] args, String indent, java.io.PrintWriter writer) {
		if (args[L_ARGS_ARCH_X86].length() > 0) {
			writer.println(indent + "   " + "<" + P_ARGS_ARCH_X86 + ">" + getWritableString(args[L_ARGS_ARCH_X86]) + "</" + P_ARGS_ARCH_X86 + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 		}
		}
		if (args[L_ARGS_ARCH_X86_64].length() > 0) {
			writer.println(indent + "   " + "<" + P_ARGS_ARCH_X86_64 + ">" + getWritableString(args[L_ARGS_ARCH_X86_64]) + "</" + P_ARGS_ARCH_X86_64 + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 		}
		}
	}
}
