/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.w3c.dom.*;

public class Fragment extends PluginBase implements IFragment {
	private String pluginId = ""; //$NON-NLS-1$
	private String pluginVersion = ""; //$NON-NLS-1$
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
	
	protected boolean hasRequiredAttributes() {
		if (pluginId==null || pluginVersion==null) return false;
		return super.hasRequiredAttributes();
	}

	void load(BundleDescription bundleDescription, PDEState state) {
		HostSpecification host = bundleDescription.getHost();
		this.pluginId = host.getName();
		VersionRange versionRange = host.getVersionRange();
		if (versionRange != null) {
			this.pluginVersion = versionRange.getMinimum() != null ? versionRange.getMinimum().toString() : null;
			this.rule = PluginBase.getMatchRule(versionRange);
		}
		super.load(bundleDescription, state);
	}
	
	public void load(IPluginBase srcPluginBase) {
		pluginId= ((Fragment)srcPluginBase).pluginId;
		pluginVersion= ((Fragment)srcPluginBase).pluginVersion;
		rule= ((Fragment)srcPluginBase).rule;
		super.load(srcPluginBase);
	}
	void load(Node node, String schemaVersion, Hashtable lineTable) {
		this.pluginId = getNodeAttribute(node, "plugin-id"); //$NON-NLS-1$
		this.pluginVersion = getNodeAttribute(node, "plugin-version"); //$NON-NLS-1$
		String match = getNodeAttribute(node, "match"); //$NON-NLS-1$
		if (match != null) {
			String[] table = IMatchRules.RULE_NAME_TABLE;
			for (int i = 0; i < table.length; i++) {
				if (match.equalsIgnoreCase(table[i])) {
					this.rule = i;
					break;
				}
			}
		}
		super.load(node, schemaVersion, lineTable);
	}
	public void reset() {
		pluginId = ""; //$NON-NLS-1$
		pluginVersion = ""; //$NON-NLS-1$
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

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_PLUGIN_ID)) {
			setPluginId(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_PLUGIN_VERSION)) {
			setPluginVersion(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_RULE)) {
			setRule(((Integer) newValue).intValue());
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		if (getSchemaVersion()!=null) {
			writer.println("<?eclipse version=\"" + getSchemaVersion() +"\"?>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.print("<fragment"); //$NON-NLS-1$
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
			writer.print("   provider-name=\"" + getWritableString(getProviderName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getPluginId() != null) {
			writer.println();
			writer.print("   plugin-id=\"" + getPluginId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getPluginVersion() != null) {
			writer.println();
			writer.print("   plugin-version=\"" + getPluginVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getRule() != IMatchRules.NONE) {
			writer.println();
			writer.print("   match=\"" + IMatchRules.RULE_NAME_TABLE[getRule()] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (children.length > 0) {
			for (int i = 0; i < children.length; i++) {
				((IPluginExtensionPoint) children[i]).write(firstIndent, writer);
			}
			writer.println();
		}

		// add extensions
		children = getExtensions();
		for (int i = 0; i < children.length; i++) {
			((IPluginExtension) children[i]).write(firstIndent, writer);
		}
		writer.println("</fragment>"); //$NON-NLS-1$
	}
}
