package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.w3c.dom.Node;

public class FeatureImport
	extends VersionableObject
	implements IFeatureImport {
	private int match = NONE;
	private IPlugin plugin;
	private int kind = KIND_PLUGIN;
	private boolean patch = false;

	public FeatureImport() {
	}

	public IPlugin getPlugin() {
		return plugin;
	}

	public void setPlugin(IPlugin plugin) {
		this.plugin = plugin;
	}

	protected void parse(Node node) {
		super.parse(node);
		this.id = getNodeAttribute(node, "plugin");
		if (this.id == null) {
			this.id = getNodeAttribute(node, "feature");
			if (this.id != null) {
				kind = KIND_FEATURE;
			}
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

		if (kind == KIND_PLUGIN)
			setPlugin(PDECore.getDefault().findPlugin(id, getVersion(), match));
	}

	public int getMatch() {
		return match;
	}

	public boolean isPatch() {
		return patch;
	}

	public int getKind() {
		return kind;
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.match);
		this.match = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public void setPatch(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(this.patch);
		this.patch = value;
		firePropertyChanged(P_PATCH, oldValue, new Boolean(value));
	}
	
	public void setKind(int kind) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.kind);
		this.kind = kind;
		firePropertyChanged(P_KIND, oldValue, new Integer(kind));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_MATCH))
			setMatch(newValue != null ? ((Integer) newValue).intValue() : 0);
		else if (name.equals(P_PATCH))
			setPatch(
				newValue != null ? ((Boolean) newValue).booleanValue() : false);
		else if (name.equals(P_KIND))
			setKind(
				newValue != null
					? ((Integer) newValue).intValue()
					: KIND_PLUGIN);
		else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		String target = "plugin";
		if (kind==KIND_FEATURE) target = "feature";
		writer.print(indent + "<import "+target+"=\"" + getId() + "\"");
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
		return getId();
	}
}