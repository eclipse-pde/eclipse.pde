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

import java.io.PrintWriter;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.Node;

public class Plugin extends PluginBase implements IPlugin {
	private String className;

	public Plugin() {
	}

	public String getClassName() {
		return className;
	}

	public IPlugin getPlugin() {
		return this;
	}
	void load(PluginModel pm) {
		PluginDescriptorModel pd = (PluginDescriptorModel) pm;
		this.className = pd.getPluginClass();
		super.load(pm);
	}
	
	public void load(IPluginBase srcPluginBase) {
		PluginBase base = (PluginBase)srcPluginBase;
		this.load(base);
	}

	void load(PluginBase srcPluginBase) {
		className = ((Plugin) srcPluginBase).className;
		super.load(srcPluginBase);
	}
	
	void load(Node node, String schemaVersion, Hashtable lineTable) {
		this.className = getNodeAttribute(node, "class");
		super.load(node, schemaVersion, lineTable);
	}

	public void reset() {
		className = null;
		super.reset();
	}
	public void setClassName(String newClassName) throws CoreException {
		ensureModelEditable();
		String oldValue = className;
		className = newClassName;
		firePropertyChanged(P_CLASS_NAME, oldValue, className);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_CLASS_NAME)) {
			setClassName(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		if (getSchemaVersion()!=null) {
			writer.print("<?eclipse version=\"");
			writer.print(getSchemaVersion());
			writer.println("\"?>");
		}		
		writer.print("<plugin");
		if (getId() != null) {
			writer.println();
			writer.print("   id=\"" + getId() + "\"");
		}
		if (getName() != null) {
			writer.println();
			writer.print("   name=\"" + getWritableString(getName()) + "\"");
		}
		if (getVersion() != null) {
			writer.println();
			writer.print("   version=\"" + getVersion() + "\"");
		}
		if (getProviderName() != null) {
			writer.println();
			writer.print(
				"   provider-name=\""
					+ getWritableString(getProviderName())
					+ "\"");
		}
		if (getClassName() != null) {
			writer.println();
			writer.print("   class=\"" + getClassName() + "\"");
		}
		writer.println(">");
		writer.println();

		String firstIndent = "   ";

		// add runtime
		Object[] children = getLibraries();
		if (children.length > 0)
			writeChildren(firstIndent, "runtime", children, writer);

		// add requires
		children = getImports();
		if (children.length > 0) {
			writeComments(writer, requiresComments);
			writeChildren(firstIndent, "requires", children, writer);
			writer.println();
		}

		children = getExtensionPoints();
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
		writeComments(writer, endComments);
		writer.println("</plugin>");
	}
}
