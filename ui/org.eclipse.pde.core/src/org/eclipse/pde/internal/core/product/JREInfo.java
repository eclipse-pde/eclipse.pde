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
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JREInfo extends ProductObject implements IJREInfo {

	private static final long serialVersionUID = 1L;
	private String fJVMLin = ""; //$NON-NLS-1$
	private String fJVMMac = ""; //$NON-NLS-1$
	private String fJVMSol = ""; //$NON-NLS-1$
	private String fJVMWin = ""; //$NON-NLS-1$

	private int fJVMLinType = 0;
	private int fJVMMacType = 0;
	private int fJVMSolType = 0;
	private int fJVMWinType = 0;

	public JREInfo(IProductModel model) {
		super(model);
	}

	public String getJVM(int platform) {
		switch (platform) {
		case LINUX:
			return fJVMLin;
		case MACOS:
			return fJVMMac;
		case SOLAR:
			return fJVMSol;
		case WIN32:
			return fJVMWin;
		}
		return ""; //$NON-NLS-1$
	}

	public String getJVMLocation(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return getJVMLocation(fJVMWin, fJVMWinType);
		} else if (Platform.OS_LINUX.equals(os)) {
			return getJVMLocation(fJVMLin, fJVMLinType);
		} else if (Platform.OS_MACOSX.equals(os)) {
			return getJVMLocation(fJVMMac, fJVMMacType);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return getJVMLocation(fJVMSol, fJVMSolType);
		}
		return ""; //$NON-NLS-1$
	}

	private String getJVMLocation(String name, int type) {
		if(type == TYPE_EE) {
			IExecutionEnvironmentsManager manager = 
				JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment environment= manager.getEnvironment(name);
			IVMInstall vm = null;
			if (environment != null) {
				vm = environment.getDefaultVM();
				if (vm == null) {
					IVMInstall[] installs = environment.getCompatibleVMs();
					// take the first strictly compatible vm if there is no default
					for (int i = 0; i < installs.length; i++) {
						IVMInstall install = installs[i];
						if (environment.isStrictlyCompatible(install)) {
							return install.getInstallLocation().getAbsolutePath();
						}
					}
					// use the first vm failing that
					if (vm == null && installs.length > 0) 
						return installs[0].getInstallLocation().getAbsolutePath();
				}
				return vm.getInstallLocation().getAbsolutePath();
			}
		}
		else if(type == TYPE_JRE) {
			IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
			for (int i = 0; i < types.length; i++) {
				IVMInstall[] installs = types[i].getVMInstalls();
				for (int k = 0; k < installs.length; k++) {
					if (installs[i].getName().equals(name))
						return installs[i].getInstallLocation().getAbsolutePath();
				}
			}
		}
		// if we're broken somehow, let's use the default vm instead of bombing
		return JavaRuntime.getDefaultVMInstall().getInstallLocation().getAbsolutePath();
	}

	public void setJVM(String args, int platform, int type) {
		String old;
		if (args == null)
			args = ""; //$NON-NLS-1$
		switch (platform) {
		case LINUX:
			old = fJVMLin;
			fJVMLin = args;
			fJVMLinType = type;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
			break;
		case MACOS:
			old = fJVMMac;
			fJVMMac = args;
			fJVMMacType = type;
			if (isEditable())
				firePropertyChanged(JRE_MAC, old, fJVMMac);
			break;
		case SOLAR:
			old = fJVMSol;
			fJVMSol = args;
			fJVMSolType = type;
			if (isEditable())
				firePropertyChanged(JRE_SOL, old, fJVMSol);
			break;
		case WIN32:
			old = fJVMWin;
			fJVMWin = args;
			fJVMWinType = type;
			if (isEditable())
				firePropertyChanged(JRE_WIN, old, fJVMWin);
			break;
		}
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(JRE_LIN)) {
					fJVMLin = getText(child);
					fJVMLinType = parseTypeString(((Element) child).getAttribute("type")); //$NON-NLS-1$
				} else if (child.getNodeName().equals(JRE_MAC)) {
					fJVMMac = getText(child);
					fJVMMacType = parseTypeString(((Element) child).getAttribute("type")); //$NON-NLS-1$
				} else if (child.getNodeName().equals(JRE_SOL)) {
					fJVMSol = getText(child);
					fJVMSolType = parseTypeString(((Element) child).getAttribute("type")); //$NON-NLS-1$
				} else if (child.getNodeName().equals(JRE_WIN)) {
					fJVMWin = getText(child);
					fJVMWinType = parseTypeString(((Element) child).getAttribute("type")); //$NON-NLS-1$
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

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<vm>"); //$NON-NLS-1$
		if (fJVMLin.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_LIN + " type=\"" + getWritableTypeString(fJVMLinType) + "\">" + getWritableString(fJVMLin) + "</" + JRE_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		if (fJVMMac.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_MAC + " type=\"" + getWritableTypeString(fJVMMacType) + "\">" + getWritableString(fJVMMac) + "</" + JRE_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		if (fJVMSol.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_SOL + " type=\"" + getWritableTypeString(fJVMSolType) + "\">" + getWritableString(fJVMSol) + "</" + JRE_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		if (fJVMWin.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_WIN + " type=\"" + getWritableTypeString(fJVMWinType) + "\">" + getWritableString(fJVMWin) + "</" + JRE_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		writer.println(indent + "</vm>"); //$NON-NLS-1$
	}

	private String getWritableTypeString(int type) {
		if(type == TYPE_EE)
			return EE;
		if(type == TYPE_JRE)
			return JRE;
		return ""; //$NON-NLS-1$
	}

	private int parseTypeString(String type) {
		if(type.equalsIgnoreCase(EE))
			return TYPE_EE;
		if(type.equalsIgnoreCase(JRE))
			return TYPE_JRE;
		return TYPE_JRE;
	}



}
