package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.PDEPlugin;

public class FeatureImport
	extends VersionableObject
	implements IFeatureImport {
	private int match = NONE;
	private IPlugin plugin;

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
		setPlugin(PDEPlugin.getDefault().findPlugin(id, getVersion(), match));
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

	public void write(String indent, PrintWriter writer) {
		writer.print(indent+"<import plugin=\""+getId()+"\"");
		if (getVersion()!=null) {
			writer.print(" version=\""+getVersion()+"\"");
		}
		if (match!=NONE) {
			writer.print(" match=\""+RULE_NAME_TABLE[match]+"\"");
		}
		writer.println("/>");
	}
	public String toString() {
		if (plugin!=null) return plugin.getTranslatedName();
		return getId();
	}
}