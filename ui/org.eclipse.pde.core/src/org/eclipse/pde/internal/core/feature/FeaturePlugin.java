/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.w3c.dom.Node;

public class FeaturePlugin extends FeatureData implements IFeaturePlugin {
	private static final long serialVersionUID = 1L;
	private boolean fFragment;
	private String fVersion;
	private boolean fUnpack = true;

	public FeaturePlugin() {
	}

	protected void reset() {
		super.reset();
		fVersion = null;
		fFragment = false;
	}

	public boolean isFragment() {
		return fFragment;
	}

	public IPluginBase getPluginBase() {
		if (id == null) {
			return null;
		}
		String version = getVersion();
		IPluginModelBase model = null;
		if (version == null || version.equals("0.0.0")) //$NON-NLS-1$
			model = PluginRegistry.findModel(id);
		else {
			ModelEntry entry = PluginRegistry.findEntry(id);
			// if no plug-ins match the id, entry == null
			if (entry != null) {
				IPluginModelBase bases[] = entry.getActiveModels();
				for (int i = 0; i < bases.length; i++) {
					if (bases[i].getPluginBase().getVersion().equals(version)) {
						model = bases[i];
						break;
					}
				}
			}
		}
		if (fFragment && model instanceof IFragmentModel)
			return model.getPluginBase();
		if (!fFragment && model instanceof IPluginModel)
			return model.getPluginBase();
		return null;
	}

	public String getVersion() {
		return fVersion;
	}

	public boolean isUnpack() {
		return fUnpack;
	}

	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fVersion;
		this.fVersion = version;
		firePropertyChanged(this, P_VERSION, oldValue, version);
	}

	public void setUnpack(boolean unpack) throws CoreException {
		ensureModelEditable();
		boolean oldValue = fUnpack;
		this.fUnpack = unpack;
		firePropertyChanged(this, P_UNPACK, new Boolean(oldValue), new Boolean(unpack));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void setFragment(boolean fragment) throws CoreException {
		ensureModelEditable();
		this.fFragment = fragment;
	}

	protected void parse(Node node) {
		super.parse(node);
		fVersion = getNodeAttribute(node, "version"); //$NON-NLS-1$
		String f = getNodeAttribute(node, "fragment"); //$NON-NLS-1$
		if (f != null && f.equalsIgnoreCase("true")) //$NON-NLS-1$
			fFragment = true;
		String unpack = getNodeAttribute(node, "unpack"); //$NON-NLS-1$
		if (unpack != null && unpack.equalsIgnoreCase("false")) //$NON-NLS-1$
			fUnpack = false;
	}

	public void loadFrom(IPluginBase plugin) {
		id = plugin.getId();
		label = plugin.getTranslatedName();
		fVersion = plugin.getVersion();
		fFragment = plugin instanceof IFragment;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin"); //$NON-NLS-1$
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		writeAttributes(indent2, writer);
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (isFragment()) {
			writer.println();
			writer.print(indent2 + "fragment=\"true\""); //$NON-NLS-1$
		}
		if (!isUnpack()) {
			writer.println();
			writer.print(indent2 + "unpack=\"false\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
		//writer.println(indent + "</plugin>");
	}

	public String getLabel() {
		IPluginBase pluginBase = getPluginBase();
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
