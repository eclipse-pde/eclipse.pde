/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.core.runtime.model.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.*;

public class PluginImport
	extends IdentifiablePluginObject
	implements IPluginImport, Serializable {
	private int match = NONE;
	private boolean reexported = false;
	private boolean optional = false;
	private String version;

	public PluginImport() {
	}
	
	public boolean isValid() {
		return getId()!=null;
	}

	public int getMatch() {
		return match;
	}

	public String getVersion() {
		return version;
	}

	public boolean isReexported() {
		return reexported;
	}

	public boolean isOptional() {
		return optional;
	}

	void load(PluginPrerequisiteModel importModel) {
		this.id = importModel.getPlugin();
		this.reexported = importModel.getExport();
		this.version = importModel.getVersion();
		switch (importModel.getMatchByte()) {
			case PluginPrerequisiteModel.PREREQ_MATCH_PERFECT :
				this.match = PERFECT;
				break;
			case PluginPrerequisiteModel.PREREQ_MATCH_EQUIVALENT :
				this.match = EQUIVALENT;
				break;
			case PluginPrerequisiteModel.PREREQ_MATCH_COMPATIBLE :
				this.match = COMPATIBLE;
				break;
			case PluginPrerequisiteModel.PREREQ_MATCH_GREATER_OR_EQUAL :
				this.match = GREATER_OR_EQUAL;
				break;
		}
		this.optional = importModel.getOptional();
		range = new int [] { importModel.getStartLine(), importModel.getStartLine() };
	}
	
	public void load(BundleDescription description) {
		this.id = description.getSymbolicName();
	}
	
	public void load(BundleSpecification importModel) {
		this.id = importModel.getName();
		this.reexported = importModel.isExported();
		this.version = importModel.getVersionSpecification() != null ? importModel.getVersionSpecification().toString() : null;
		this.optional = importModel.isOptional();
		range = new int[] {0,0};
		switch (importModel.getMatchingRule()) {
			case VersionConstraint.GREATER_EQUAL_MATCH:
				match = IMatchRules.GREATER_OR_EQUAL;
				break;
			case VersionConstraint.NO_MATCH:
				match = IMatchRules.NONE;
				break;
			case VersionConstraint.MINOR_MATCH:
				match = IMatchRules.EQUIVALENT;
				break;
			case VersionConstraint.MICRO_MATCH:
				match = IMatchRules.PERFECT;
				break;
			case VersionConstraint.QUALIFIER_MATCH:
				match = IMatchRules.PERFECT;
				break;
			default:
				match = IMatchRules.COMPATIBLE;			
		}
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IPluginImport) {
			IPluginImport target = (IPluginImport) obj;
			// Objects from the same model must be
			// binary equal
			if (target.getModel().equals(getModel()))
				return false;

			if (target.getId().equals(getId())
				&& target.isReexported() == isReexported()
				&& stringEqualWithNull(target.getVersion(),getVersion())
				&& target.getMatch() == getMatch()
				&& target.isOptional() == isOptional())
				return true;
		}
		return false;
	}

	void load(Node node, Hashtable lineTable) {
		String id = getNodeAttribute(node, "plugin");
		String export = getNodeAttribute(node, "export");
		String option = getNodeAttribute(node, "optional");
		String version = getNodeAttribute(node, "version");
		String match = getNodeAttribute(node, "match");
		boolean reexport =
			export != null && export.toLowerCase().equals("true");
		boolean optional =
			option != null && option.toLowerCase().equals("true");
		this.match = NONE;
		if (match != null) {
			String lmatch = match.toLowerCase();
			if (lmatch.equals("exact"))
				lmatch = RULE_EQUIVALENT;
			for (int i = 0; i < RULE_NAME_TABLE.length; i++) {
				if (lmatch.equals(RULE_NAME_TABLE[i])) {
					this.match = i;
					break;
				}
			}
		}
		this.version = version;
		this.id = id;
		this.reexported = reexport;
		this.optional = optional;
		addComments(node);
		bindSourceLocation(node, lineTable);
	}
	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.match);
		this.match = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}
	public void setReexported(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(reexported);
		this.reexported = value;
		firePropertyChanged(P_REEXPORTED, oldValue, new Boolean(value));
	}
	public void setOptional(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(this.optional);
		this.optional = value;
		firePropertyChanged(P_OPTIONAL, oldValue, new Boolean(value));
	}
	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		String oldValue = this.version;
		this.version = version;
		firePropertyChanged(P_VERSION, oldValue, version);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_MATCH)) {
			setMatch(((Integer) newValue).intValue());
			return;
		}
		if (name.equals(P_REEXPORTED)) {
			setReexported(((Boolean) newValue).booleanValue());
			return;
		}
		if (name.equals(P_OPTIONAL)) {
			setOptional(((Boolean) newValue).booleanValue());
			return;
		}
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void write(String indent, PrintWriter writer) {
		writeComments(writer);
		writer.print(indent);
		writer.print("<import plugin=\"" + getId() + "\"");
		if (isReexported())
			writer.print(" export=\"true\"");
		if (isOptional())
			writer.print(" optional=\"true\"");
		if (version != null && version.length() > 0)
			writer.print(" version=\"" + version + "\"");
		if (match != NONE) {
			String matchValue = RULE_NAME_TABLE[match];
			writer.print(" match=\"" + matchValue + "\"");
		}
		writer.println("/>");
	}
}
