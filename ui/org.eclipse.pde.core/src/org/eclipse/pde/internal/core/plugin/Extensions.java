/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.util.*;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;

public class Extensions
	extends AbstractExtensions {
	private boolean valid;
	private boolean fIsFragment;

	public Extensions() {
	}

	void load(Extensions srcPluginBase) {
		range= srcPluginBase.range;
		super.load(srcPluginBase);
		valid = hasRequiredAttributes();
	}
	public void load(IPluginBase srcPluginBase) {
		this.load(srcPluginBase);
	}

	void load(Node node, Hashtable lineTable) {
		bindSourceLocation(node, lineTable);

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				processChild(child, lineTable);
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
		writer.println(PDECore.getResourceString("Plugin.xmlHeader")); //$NON-NLS-1$
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
