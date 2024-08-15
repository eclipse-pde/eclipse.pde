/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.w3c.dom.Node;

public class FeatureImport extends VersionableObject implements IFeatureImport {
	private static final long serialVersionUID = 1L;
	private int fMatch = NONE;
	private int fType = PLUGIN;
	private boolean fPatch = false;
	private String fFilter = null;

	public IPlugin getPlugin() {
		if (id != null && fType == PLUGIN) {
			IPluginModelBase model = PluginRegistry.findModel(id, version, VersionUtil.matchRuleFromLiteral(fMatch));
			return model instanceof IPluginModel ? ((IPluginModel) model).getPlugin() : null;
		}
		return null;
	}

	@Override
	public IFeature getFeature() {
		if (id != null && fType == FEATURE) {
			return findFeature(id, getVersion(), fMatch);
		}
		return null;
	}

	/**
	 * Finds a feature with the given ID and satisfying constraints
	 * of the version and the match.
	 * @return IFeature or null
	 */
	public IFeature findFeature(String id, String version, int match) {
		List<IFeatureModel> models = PDECore.getDefault().getFeatureModelManager().findFeatureModels(id);
		for (IFeatureModel model : models) {
			IFeature feature = model.getFeature();
			if (id.equals(feature.getId()) && VersionUtil.compare(feature.getVersion(), version, match)) {
				return feature;
			}
		}
		return null;
	}

	@Override
	protected void reset() {
		super.reset();
		fPatch = false;
		fType = PLUGIN;
		fMatch = NONE;
		fFilter = null;
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		this.id = getNodeAttribute(node, "plugin"); //$NON-NLS-1$
		if (id != null) {
			fType = PLUGIN;
		} else {
			this.id = getNodeAttribute(node, "feature"); //$NON-NLS-1$
			if (id != null) {
				fType = FEATURE;
			}
		}
		String mvalue = getNodeAttribute(node, "match"); //$NON-NLS-1$
		if (mvalue != null && mvalue.length() > 0) {
			String[] choices = RULE_NAME_TABLE;
			for (int i = 0; i < choices.length; i++) {
				if (mvalue.equalsIgnoreCase(choices[i])) {
					fMatch = i;
					break;
				}
			}
		}
		fPatch = getBooleanAttribute(node, "patch"); //$NON-NLS-1$
		fFilter = getNodeAttribute(node, "filter"); //$NON-NLS-1$
	}

	public void loadFrom(IFeature feature) {
		reset();
		fType = FEATURE;
		id = feature.getId();
		version = feature.getVersion();
	}

	@Override
	public int getMatch() {
		return fMatch;
	}

	@Override
	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = Integer.valueOf(this.fMatch);
		this.fMatch = match;
		firePropertyChanged(P_MATCH, oldValue, Integer.valueOf(match));
	}

	@Override
	public int getType() {
		return fType;
	}

	@Override
	public void setType(int type) throws CoreException {
		ensureModelEditable();
		Integer oldValue = Integer.valueOf(this.fType);
		this.fType = type;
		firePropertyChanged(P_TYPE, oldValue, Integer.valueOf(type));
	}

	@Override
	public boolean isPatch() {
		return fPatch;
	}

	@Override
	public void setPatch(boolean patch) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = Boolean.valueOf(this.fPatch);
		this.fPatch = patch;
		firePropertyChanged(P_PATCH, oldValue, Boolean.valueOf(patch));
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		switch (name) {
		case P_MATCH:
			setMatch(newValue != null ? ((Integer) newValue).intValue() : 0);
			break;
		case P_TYPE:
			setType(newValue != null ? ((Integer) newValue).intValue() : PLUGIN);
			break;
		case P_PATCH:
			setPatch(newValue != null ? ((Boolean) newValue).booleanValue() : false);
			break;
		default:
			super.restoreProperty(name, oldValue, newValue);
			break;
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		String typeAtt = fType == FEATURE ? "feature" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(indent + "<import " + typeAtt + "=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String version = getVersion();
		if (version != null && version.length() > 0) {
			writer.print(" version=\"" + version + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!fPatch && fMatch != NONE) {
			writer.print(" match=\"" + RULE_NAME_TABLE[fMatch] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fPatch) {
			writer.print(" patch=\"true\""); //$NON-NLS-1$
		}
		if (fFilter != null) {
			writer.print(" filter=\"" + fFilter + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		IPlugin plugin = getPlugin();
		if (plugin != null) {
			return plugin.getTranslatedName();
		}
		IFeature feature = getFeature();
		if (feature != null) {
			return feature.getLabel();
		}
		return getId();
	}

	@Override
	public String getFilter() {
		return fFilter;
	}

	@Override
	public void setFilter(String filter) throws CoreException {
		this.fFilter = filter;

	}
}
