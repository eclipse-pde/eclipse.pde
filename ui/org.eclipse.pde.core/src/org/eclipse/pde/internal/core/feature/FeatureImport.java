/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	private int fMatch = NONE;
	private int fIdMatch = PERFECT;
	private int fType = PLUGIN;
	private boolean fPatch = false;

	public FeatureImport() {
	}

	public IPlugin getPlugin() {
		if (id != null && fType == PLUGIN) {
			return PDECore.getDefault().findPlugin(id, getVersion(), fMatch);
		}
		return null;
	}

	public IFeature getFeature() {
		if (id != null && fType == FEATURE) { 
			return PDECore.getDefault().findFeature(id, getVersion(), fMatch);
		}
		return null;
	}

	public int getIdMatch() {
		return fIdMatch;
	}

	protected void reset() {
		super.reset();
		fPatch = false;
		fType = PLUGIN;
		fMatch = NONE;
		fIdMatch = PERFECT;
	}

	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		bindSourceLocation(node, lineTable);
		this.id = getNodeAttribute(node, "plugin"); //$NON-NLS-1$
		if (id != null)
			fType = PLUGIN;
		else {
			this.id = getNodeAttribute(node, "feature"); //$NON-NLS-1$
			if (id != null)
				fType = FEATURE;
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
		mvalue = getNodeAttribute(node, "id-match"); //$NON-NLS-1$

		if (mvalue != null && mvalue.length() > 0) {
			if (mvalue.equalsIgnoreCase(RULE_PREFIX))
				fIdMatch = PREFIX;
		}
		fPatch = getBooleanAttribute(node, "patch"); //$NON-NLS-1$
	}

	public void loadFrom(IFeature feature) {
		reset();
		fType = FEATURE;
		id = feature.getId();
		version = feature.getVersion();
	}

	public int getMatch() {
		return fMatch;
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.fMatch);
		this.fMatch = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public void setIdMatch(int idMatch) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.fIdMatch);
		this.fIdMatch = idMatch;
		firePropertyChanged(P_ID_MATCH, oldValue, new Integer(idMatch));
	}

	public int getType() {
		return fType;
	}

	public void setType(int type) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.fType);
		this.fType = type;
		firePropertyChanged(P_TYPE, oldValue, new Integer(type));
	}

	public boolean isPatch() {
		return fPatch;
	}

	public void setPatch(boolean patch) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(this.fPatch);
		this.fPatch = patch;
		firePropertyChanged(P_PATCH, oldValue, new Boolean(patch));
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
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		String typeAtt = fType == FEATURE ? "feature" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(indent + "<import " + typeAtt + "=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (getVersion() != null) {
			writer.print(" version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!fPatch && fMatch != NONE) {
			writer.print(" match=\"" + RULE_NAME_TABLE[fMatch] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fIdMatch == PREFIX) {
			writer.print(" id-match=\"prefix\""); //$NON-NLS-1$
		}
		if (fPatch) {
			writer.print(" patch=\"true\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	public String toString() {
		IPlugin plugin = getPlugin();
		if (plugin != null)
			return plugin.getTranslatedName();
		IFeature feature = getFeature();
		if (feature != null)
			return feature.getLabel();
		return getId();
	}
}
