package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IResource;
import java.io.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.PlatformObject;

public class Fragment extends PluginBase implements IFragment {
	private String pluginId = "";
	private String pluginVersion = "";
	private int rule = IMatchRules.NONE;

	public Fragment() {
	}
	public String getPluginId() {
		return pluginId;
	}
	public String getPluginVersion() {
		return pluginVersion;
	}
	public int getRule() {
		return rule;
	}
	void load(PluginModel pm) {
		PluginFragmentModel pfm = (PluginFragmentModel) pm;
		this.pluginId = pfm.getPluginId();
		this.pluginVersion = pfm.getPluginVersion();
		switch (pfm.getMatch()) {
			case PluginFragmentModel.FRAGMENT_MATCH_COMPATIBLE :
				rule = IMatchRules.COMPATIBLE;
				break;
			case PluginFragmentModel.FRAGMENT_MATCH_EQUIVALENT :
				rule = IMatchRules.EQUIVALENT;
				break;
			case PluginFragmentModel.FRAGMENT_MATCH_PERFECT :
				rule = IMatchRules.PERFECT;
				break;
			case PluginFragmentModel.FRAGMENT_MATCH_GREATER_OR_EQUAL :
				rule = IMatchRules.GREATER_OR_EQUAL;
				break;
		}
		super.load(pm);
	}
	void load(Node node, Hashtable lineTable) {
		this.pluginId = getNodeAttribute(node, "plugin-id");
		this.pluginVersion = getNodeAttribute(node, "plugin-version");
		String match = getNodeAttribute(node, "match");
		if (match != null) {
			String value = match.toLowerCase();
			String[] table = IMatchRules.RULE_NAME_TABLE;
			for (int i = 0; i < table.length; i++) {
				if (value.equals(table[i])) {
					rule = i;
					break;
				}
			}
		}
		super.load(node, lineTable);
	}
	public void reset() {
		pluginId = "";
		pluginVersion = "";
		rule = IMatchRules.NONE;
		super.reset();
	}
	public void setPluginId(String newPluginId) throws CoreException {
		ensureModelEditable();
		String oldValue = this.pluginId;
		pluginId = newPluginId;
		firePropertyChanged(P_PLUGIN_ID, oldValue, pluginId);
	}
	public void setPluginVersion(String newPluginVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = this.pluginVersion;
		pluginVersion = newPluginVersion;
		firePropertyChanged(P_PLUGIN_VERSION, oldValue, pluginVersion);
	}
	public void setRule(int rule) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.rule);
		this.rule = rule;
		firePropertyChanged(P_RULE, oldValue, new Integer(rule));
	}
	
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_PLUGIN_ID)) {
			setPluginId(newValue!=null ? newValue.toString():null);
			return;
		}
		if (name.equals(P_PLUGIN_VERSION)) {
			setPluginVersion(newValue!=null ? newValue.toString():null);
			return;
		}
		if (name.equals(P_RULE)) {
			setRule(((Integer)newValue).intValue());
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}
	
	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		//writer.println("<!-- File written by PDE 1.0 -->");
		writer.print("<fragment");
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
			writer.print("   provider-name=\"" + getProviderName() + "\"");
		}
		if (getPluginId() != null) {
			writer.println();
			writer.print("   plugin-id=\"" + getPluginId() + "\"");
		}
		if (getPluginVersion() != null) {
			writer.println();
			writer.print("   plugin-version=\"" + getPluginVersion() + "\"");
		}
		if (getRule() != IMatchRules.NONE) {
			writer.println();
			writer.print("   match=\"" + IMatchRules.RULE_NAME_TABLE[getRule()] + "\"");
		}
		writer.println(">");
		writer.println();

		// add runtime
		Object[] children = getLibraries();
		writeChildren("runtime", children, writer);
		// add extension points
		writer.println();

		// add requires
		children = getImports();
		if (children.length > 0) {
			writeComments(writer, requiresComments);
			writeChildren("requires", children, writer);
			writer.println();
		}

		children = getExtensionPoints();
		for (int i = 0; i < children.length; i++) {
			((IPluginExtensionPoint) children[i]).write("", writer);
		}
		writer.println();

		// add extensions
		children = getExtensions();

		for (int i = 0; i < children.length; i++) {
			((IPluginExtension) children[i]).write("", writer);
		}
		writer.println("</fragment>");
	}
}