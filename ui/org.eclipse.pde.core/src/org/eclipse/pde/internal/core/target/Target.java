/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IRuntimeInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Target extends TargetObject implements ITarget {

	private static final long serialVersionUID = 1L;
	private IArgumentsInfo fArgsInfo;
	private IEnvironmentInfo fEnvInfo;
	private IRuntimeInfo fRuntimeInfo;
	
	public Target(ITargetModel model) {
		super(model);
	}

	public void reset() {
		fArgsInfo = null;
		fEnvInfo = null;
		fRuntimeInfo = null;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE 
				&& node.getNodeName().equals("target")) { //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			ITargetModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("launcherArgs")) { //$NON-NLS-1$
						fArgsInfo = factory.createArguments();
						fArgsInfo.parse(child);
					} else if (name.equals("environment")) { //$NON-NLS-1$
						fEnvInfo = factory.createEnvironment();
						fEnvInfo.parse(child);
					} else if (name.equals("targetJRE")) { //$NON-NLS-1$
						fRuntimeInfo = factory.createJREInfo();
						fRuntimeInfo.parse(child);
					}
				}
			}
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<target>"); //$NON-NLS-1$
		if (fArgsInfo != null) {
			fArgsInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fEnvInfo != null) {
			fEnvInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		if (fRuntimeInfo != null) {
			fRuntimeInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}
		writer.println();
		writer.println(indent + "</target>"); //$NON-NLS-1$
	}
	
	public IArgumentsInfo getArguments() {
		return fArgsInfo;
	}
	
	public void setArguments(IArgumentsInfo info) {
		fArgsInfo = info;
	}

	public IEnvironmentInfo getEnvironment() {
		return fEnvInfo;
	}

	public void setEnvironment(IEnvironmentInfo info) {
		fEnvInfo = info;
	}

	public IRuntimeInfo getTargetJREInfo() {
		return fRuntimeInfo;
	}

	public void setTargetJREInfo(IRuntimeInfo info) {
		fRuntimeInfo = info;
		
	}
}
