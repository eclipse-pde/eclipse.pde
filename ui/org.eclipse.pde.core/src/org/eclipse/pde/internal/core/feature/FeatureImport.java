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
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class FeatureImport
	extends VersionableObject
	implements IFeatureImport {
	private static final long serialVersionUID = 1L;
	private int match = NONE;
	private int idMatch = PERFECT;
	private IPlugin plugin;
	private IFeature feature;
	private int type = PLUGIN;
	private boolean patch = false;
	private String os;
	private String ws;
	private String arch;

	public FeatureImport() {
	}

	public IPlugin getPlugin() {
		if (id != null && type == PLUGIN && plugin == null) {
			setPlugin(PDECore.getDefault().findPlugin(id, getVersion(), match));
		}
		return plugin;
	}

	public IFeature getFeature() {
		if (id != null && type == FEATURE && feature == null) { 
			setFeature(PDECore.getDefault().findFeature(id, getVersion(), match));
		}
		return feature;
	}

	public int getIdMatch() {
		return idMatch;
	}

	public void setPlugin(IPlugin plugin) {
		this.plugin = plugin;
	}

	public void setFeature(IFeature feature) {
		this.feature = feature;
	}

	public String getOS() {
		return os;
	}

	public String getWS() {
		return ws;
	}

	public String getArch() {
		return arch;
	}

	protected void reset() {
		super.reset();
		patch = false;
		type = PLUGIN;
		match = NONE;
		idMatch = PERFECT;
		arch = null;
		os = null;
		ws = null;
	}

	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		bindSourceLocation(node, lineTable);
		this.id = getNodeAttribute(node, "plugin"); //$NON-NLS-1$
		if (id != null)
			type = PLUGIN;
		else {
			this.id = getNodeAttribute(node, "feature"); //$NON-NLS-1$
			if (id != null)
				type = FEATURE;
		}
		this.os = getNodeAttribute(node, "os"); //$NON-NLS-1$
		this.ws = getNodeAttribute(node, "ws"); //$NON-NLS-1$
		this.arch = getNodeAttribute(node, "arch"); //$NON-NLS-1$
		String mvalue = getNodeAttribute(node, "match"); //$NON-NLS-1$
		if (mvalue != null && mvalue.length() > 0) {
			String[] choices = RULE_NAME_TABLE;
			for (int i = 0; i < choices.length; i++) {
				if (mvalue.equalsIgnoreCase(choices[i])) {
					match = i;
					break;
				}
			}
		}
		mvalue = getNodeAttribute(node, "id-match"); //$NON-NLS-1$

		if (mvalue != null && mvalue.length() > 0) {
			if (mvalue.equalsIgnoreCase(RULE_PREFIX))
				idMatch = PREFIX;
		}
		patch = getBooleanAttribute(node, "patch"); //$NON-NLS-1$
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

	public void setIdMatch(int idMatch) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.idMatch);
		this.idMatch = idMatch;
		firePropertyChanged(P_ID_MATCH, oldValue, new Integer(idMatch));
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

	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		String oldValue = this.os;
		this.os = os;
		firePropertyChanged(P_OS, oldValue, os);
	}

	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		String oldValue = this.ws;
		this.ws = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}

	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		String oldValue = this.arch;
		this.arch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_MATCH)) {
			setMatch(newValue != null ? ((Integer) newValue).intValue() : 0);
		} else if (name.equals(P_ID_MATCH)) {
			setIdMatch(newValue != null ? ((Integer) newValue).intValue() : 0);
		} else if (name.equals(P_TYPE)) {
			setType(
				newValue != null ? ((Integer) newValue).intValue() : PLUGIN);
		} else if (name.equals(P_PATCH)) {
			setPatch(
				newValue != null ? ((Boolean) newValue).booleanValue() : false);
		} else if (name.equals(P_OS)) {
			setOS((String) newValue);
		} else if (name.equals(P_WS)) {
			setWS((String) newValue);
		} else if (name.equals(P_ARCH)) {
			setArch((String) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		String typeAtt = type == FEATURE ? "feature" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(indent + "<import " + typeAtt + "=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (getVersion() != null) {
			writer.print(" version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!patch && match != NONE) {
			writer.print(" match=\"" + RULE_NAME_TABLE[match] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (idMatch == PREFIX) {
			writer.print(" id-match=\"prefix\""); //$NON-NLS-1$
		}
		if (os != null) {
			writer.print(" os=\"" + getOS() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (ws != null) {
			writer.print(" ws=\"" + getWS() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (arch != null) {
			writer.print(" arch=\"" + getArch() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (patch) {
			writer.print(" patch=\"true\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
	}
	public String toString() {
		if (plugin != null)
			return plugin.getTranslatedName();
		else if (feature != null)
			return feature.getLabel();
		return getId();
	}
}
