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
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.w3c.dom.Node;

public class FeaturePlugin extends FeatureData implements IFeaturePlugin {
	private IPluginBase pluginBase;
	private boolean fragment;
	private String version;

	public FeaturePlugin() {
	}

	protected void reset() {
		super.reset();
		version = null;
		fragment = false;
	}

	public boolean isFragment() {
		return fragment;
	}

	public IPluginBase getPluginBase() {
		return pluginBase;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.version;
		this.version = version;
		firePropertyChanged(this, P_VERSION, oldValue, version);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void setFragment(boolean fragment) throws CoreException {
		ensureModelEditable();
		this.fragment = fragment;
	}

	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		version = getNodeAttribute(node, "version");
		String f = getNodeAttribute(node, "fragment");
		if (f != null && f.equalsIgnoreCase("true"))
			fragment = true;
		if (id!=null && version!=null) hookWithWorkspace();
	}

	public void hookWithWorkspace() {
		if (fragment) {
			IFragmentModel[] fragments =
				PDECore.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
			for (int i = 0; i < fragments.length; i++) {
				IFragment fragment = fragments[i].getFragment();
				if (fragment.getId().equals(id)) {
					if (version == null || fragment.getVersion().equals(version)) {
						pluginBase = fragment;
						break;
					}
				}
			}
		} else {
			pluginBase = PDECore.getDefault().findPlugin(id, version, 0);
		}
	}

	public void loadFrom(IPluginBase plugin) {
		id = plugin.getId();
		label = plugin.getTranslatedName();
		version = plugin.getVersion();
		fragment = plugin instanceof IFragment;
		this.pluginBase = plugin;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin");
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		writeAttributes(indent2, writer);
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\"");
		}
		if (isFragment()) {
			writer.println();
			writer.print(indent2 + "fragment=\"true\"");
		}
		writer.println("/>");
		//writer.println(indent + "</plugin>");
	}

	public String getLabel() {
		if (pluginBase != null) {
			return pluginBase.getTranslatedName();
		}
		String name = super.getLabel();
		if (name == null)
			name = getId();
		return name;
	}

	public String toString() {
		return getLabel();
	}
	
}
