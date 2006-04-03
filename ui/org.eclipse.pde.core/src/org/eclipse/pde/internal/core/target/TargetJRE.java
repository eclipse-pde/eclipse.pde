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
package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TargetJRE extends TargetObject implements ITargetJRE {

	private static final long serialVersionUID = 1L;
	private int fType;
	private String fName;

	public TargetJRE(ITargetModel model) {
		super(model);
	}

	public int getJREType() {
		return fType;
	}

	public String getJREName() {
		return fName;
	}

	public void setNamedJRE(String name) {
		int oldType = fType;
		String oldName = fName;
		fName =  (name == null) ? "" : name; //$NON-NLS-1$
		fType = TYPE_NAMED;
		if (oldType != fType)
			firePropertyChanged(P_TARGET_JRE, new Integer(oldType), new Integer(fType));
		else
			firePropertyChanged(P_TARGET_JRE, oldName, fName);
	}

	public void setExecutionEnvJRE(String name) {
		int oldType = fType;
		String oldName = fName;
		fName =  (name == null) ? "" : name; //$NON-NLS-1$
		fType = TYPE_EXECUTION_ENV;
		if (oldType != fType)
			firePropertyChanged(P_TARGET_JRE, new Integer(oldType), new Integer(fType));
		else
			firePropertyChanged(P_TARGET_JRE, oldName, fName);
	}

	public void setDefaultJRE() {
		int oldType = fType;
		fName =  "";  //$NON-NLS-1$
		fType = TYPE_DEFAULT;
		if (oldType != fType)
			firePropertyChanged(P_TARGET_JRE, new Integer(oldType), new Integer(fType));
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("jreName")) { //$NON-NLS-1$
					fType = TYPE_NAMED;
					fName = getText(child);
				} else if (child.getNodeName().equals("execEnv")) { //$NON-NLS-1$
					fType = TYPE_EXECUTION_ENV;
					fName = getText(child);
				} 
			}
		}
		if (list.getLength() == 0) {
			fType = TYPE_DEFAULT;
			fName = ""; //$NON-NLS-1$
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
		if (fType == 0) 
			return;
		writer.println();
		writer.println(indent + "<targetJRE>"); //$NON-NLS-1$
		if (fType == 1) 
			writer.println(indent + "   <jreName>" + getWritableString(fName) + "</jreName>"); //$NON-NLS-1$ //$NON-NLS-2$
		else if (fType == 2)
			writer.println(indent + "   <execEnv>" + getWritableString(fName) + "</execEnv>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent + "</targetJRE>"); //$NON-NLS-1$
	}
	
	public String getCompatibleJRE() {
		int jreType = getJREType();

		switch (jreType) {
		case ITargetJRE.TYPE_DEFAULT:
			return JavaRuntime.getDefaultVMInstall().getName();
		case ITargetJRE.TYPE_NAMED:
			return getJREName();
		case ITargetJRE.TYPE_EXECUTION_ENV:
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment environment = manager.getEnvironment(getJREName());
			IVMInstall vm = null;
			if (environment != null) {
				vm = environment.getDefaultVM();
				if (vm == null) {
					IVMInstall[] installs = environment.getCompatibleVMs();
					// take the first strictly compatible vm if there is no default
					for (int i = 0; i < installs.length; i++) {
						IVMInstall install = installs[i];
						if (environment.isStrictlyCompatible(install)) {
							return install.getName();
						}
					}
					// use the first vm failing that
					if (vm == null && installs.length > 0) 
						return installs[0].getName();
				}
				return vm.getName();
			}
		}
		return JavaRuntime.getDefaultVMInstall().getName();
	}

}
