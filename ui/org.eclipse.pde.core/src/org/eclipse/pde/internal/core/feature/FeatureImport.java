package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.w3c.dom.Node;

public class FeatureImport
	extends VersionableObject
	implements IFeatureImport {
	private int match = NONE;
	private IPlugin plugin;
	private IFeature feature;
	private int type = PLUGIN;
	private boolean patch = false;

	public FeatureImport() {
	}

	public IPlugin getPlugin() {
		return plugin;
	}
	
	public IFeature getFeature() {
		return feature;
	}

	public void setPlugin(IPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void setFeature(IFeature feature) {
		this.feature = feature;
	}
	
	protected void reset() {
		super.reset();
		patch = false;
		type = PLUGIN;
		match = NONE;
	}

	protected void parse(Node node) {
		super.parse(node);
		this.id = getNodeAttribute(node, "plugin");
		if (id != null)
			type = PLUGIN;
		else {
			this.id = getNodeAttribute(node, "feature");
			type = FEATURE;
		}
		String mvalue = getNodeAttribute(node, "match");
		if (mvalue != null && mvalue.length() > 0) {
			String[] choices = RULE_NAME_TABLE;
			for (int i = 0; i < choices.length; i++) {
				if (mvalue.equalsIgnoreCase(choices[i])) {
					match = i;
					break;
				}
			}
		}
		patch = getBooleanAttribute(node, "patch");
		if (type==PLUGIN)
			setPlugin(PDECore.getDefault().findPlugin(id, getVersion(), match));
		else 
			setFeature(PDECore.getDefault().findFeature(id, getVersion(), match));
	}
	
	public void loadFrom(IFeature feature) {
		reset();
		this.feature = feature;
		type = FEATURE;
		id = feature.getId();
		version = feature.getVersion();
	}

	public int getMatch() {
		return match;
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.match);
		this.match = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public int getType() {
		return type;
	}

	public void setType(int type) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.type);
		this.type = type;
		firePropertyChanged(P_TYPE, oldValue, new Integer(type));
	}

	public boolean isPatch() {
		return patch;
	}

	public void setPatch(boolean patch) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(this.patch);
		this.patch = patch;
		firePropertyChanged(P_PATCH, oldValue, new Boolean(patch));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_MATCH)) {
			setMatch(newValue != null ? ((Integer) newValue).intValue() : 0);
		} else if (name.equals(P_TYPE)) {
			setType(
				newValue != null ? ((Integer) newValue).intValue() : PLUGIN);
		} else if (name.equals(P_PATCH)) {
			setPatch(
				newValue != null ? ((Boolean) newValue).booleanValue() : false);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		String typeAtt = type==FEATURE ? "feature":"plugin";
		writer.print(indent + "<import "+typeAtt+"=\"" + getId() + "\"");
		if (getVersion() != null) {
			writer.print(" version=\"" + getVersion() + "\"");
		}
		if (match != NONE) {
			writer.print(" match=\"" + RULE_NAME_TABLE[match] + "\"");
		}
		if (patch) {
			writer.print(" patch=\"true\"");
		}
		writer.println("/>");
	}
	public String toString() {
		if (plugin != null)
			return plugin.getTranslatedName();
		else if (feature != null)
			return feature.getLabel();
		return getId();
	}
}