/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.w3c.dom.Node;

public class FeaturePlugin extends FeatureData implements IFeaturePlugin {
	private static final long serialVersionUID = 1L;
	private String fVersion;

	@Override
	protected void reset() {
		super.reset();
		fVersion = null;
	}

	@Override
	public boolean isFragment() {
		return getPluginBase() instanceof IFragment;
	}

	public IPluginBase getPluginBase() {
		if (id == null) {
			return null;
		}
		String version = getVersion();
		IPluginModelBase model = null;
		if (version == null || version.equals(ICoreConstants.DEFAULT_VERSION)) {
			model = PluginRegistry.findModel(id);
		} else {
			ModelEntry entry = PluginRegistry.findEntry(id);
			// if no plug-ins match the id, entry == null
			if (entry != null) {
				model = Arrays.stream(entry.getActiveModels())
						.filter(base -> base.getPluginBase().getVersion().equals(version))
						.findFirst().orElse(null);
			}
		}
		if (model != null) {
			return model.getPluginBase();
		}
		return null;
	}

	@Override
	public String getVersion() {
		return fVersion;
	}

	@Override
	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fVersion;
		this.fVersion = version;
		firePropertyChanged(this, P_VERSION, oldValue, version);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		fVersion = getNodeAttribute(node, "version"); //$NON-NLS-1$
	}

	public void loadFrom(IPluginBase plugin) {
		id = plugin.getId();
		label = plugin.getTranslatedName();
		fVersion = plugin.getVersion();
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin"); //$NON-NLS-1$
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		writeAttributes(indent2, writer);
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	@Override
	public String getLabel() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase != null) {
			return pluginBase.getTranslatedName();
		}
		String name = super.getLabel();
		if (name == null) {
			name = getId();
		}
		return name;
	}

	@Override
	public String toString() {
		return getLabel();
	}

}
