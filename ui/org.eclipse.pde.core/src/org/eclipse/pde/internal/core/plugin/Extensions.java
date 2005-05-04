/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;

import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Extensions
	extends AbstractExtensions {
	private static final long serialVersionUID = 1L;
	private boolean valid;
	private boolean fIsFragment;

	public Extensions() {
	}

	void load(Extensions srcPluginBase) {
		super.load(srcPluginBase);
		valid = hasRequiredAttributes();
	}

	void load(Node node) {
		if (node == null)
			return;
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				processChild(child);
			}
		}
		valid = hasRequiredAttributes();
	}
	
	void load(Node[] nodes) {
		for (int i = 0; i < nodes.length; i++) {
			Node child = nodes[i];
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				processChild(child);
			}
		}
		valid = hasRequiredAttributes();	
	}

	public void reset() {
		super.reset();
		valid=false;
	}
	
	public boolean isValid() {
		return valid;
	}

	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.print(fIsFragment ? "<fragment>" : "<plugin>"); //$NON-NLS-1$ //$NON-NLS-2$
		
		writer.println();
	
		String firstIndent = "   "; //$NON-NLS-1$
	
		Object [] children = getExtensionPoints();
		if (children.length > 0)
			writer.println();
		for (int i = 0; i < children.length; i++) {
			((IPluginExtensionPoint) children[i]).write(firstIndent, writer);
		}
	
		// add extensions
		children = getExtensions();
		if (children.length > 0)
			writer.println();
		for (int i = 0; i < children.length; i++) {
			((IPluginExtension) children[i]).write(firstIndent, writer);
		}
		writer.println();
		writer.println(fIsFragment ? "</fragment>" : "</plugin>"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void setIsFragment(boolean isFragment) {
		fIsFragment = isFragment;
	}
}
