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

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.osgi.framework.*;
import org.w3c.dom.*;

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

	void load(BundleDescription bundleDescription, PDEState state) {
		Dictionary manifest = state.getManifest(bundleDescription.getBundleId());
		this.className = (String)manifest.get(Constants.BUNDLE_ACTIVATOR);
		super.load(bundleDescription, state);
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
		this.className = getNodeAttribute(node, "class"); //$NON-NLS-1$
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
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		if (getSchemaVersion()!=null) {
			writer.print("<?eclipse version=\"" + getSchemaVersion() + "\"?>"); //$NON-NLS-1$ //$NON-NLS-2$
		}		
		writer.print("<plugin"); //$NON-NLS-1$
		if (getId() != null) {
			writer.println();
			writer.print("   id=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getName() != null) {
			writer.println();
			writer.print("   name=\"" + getWritableString(getName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getVersion() != null) {
			writer.println();
			writer.print("   version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getProviderName() != null) {
			writer.println();
			writer.print(
				"   provider-name=\"" //$NON-NLS-1$
					+ getWritableString(getProviderName())
					+ "\""); //$NON-NLS-1$
		}
		if (getClassName() != null) {
			writer.println();
			writer.print("   class=\"" + getClassName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$
		writer.println();

		String firstIndent = "   "; //$NON-NLS-1$

		// add runtime
		Object[] children = getLibraries();
		if (children.length > 0) {
			writeChildren(firstIndent, "runtime", children, writer); //$NON-NLS-1$
			writer.println();
		}

		// add requires
		children = getImports();
		if (children.length > 0) {
			writeChildren(firstIndent, "requires", children, writer); //$NON-NLS-1$
			writer.println();
		}

		children = getExtensionPoints();
		for (int i = 0; i < children.length; i++) {
			((IPluginExtensionPoint) children[i]).write(firstIndent, writer);
		}
		if (children.length > 0)
			writer.println();

		// add extensions
		children = getExtensions();
		for (int i = 0; i < children.length; i++) {
			((IPluginExtension) children[i]).write(firstIndent, writer);
		}
		if (children.length > 0)
			writer.println();
		
		writer.println("</plugin>"); //$NON-NLS-1$
	}
}
