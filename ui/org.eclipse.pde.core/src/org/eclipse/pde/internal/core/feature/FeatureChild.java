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
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class FeatureChild extends IdentifiableObject implements IFeatureChild {
	private String version;
	private IFeature feature;
	private String name;
	private boolean optional;
	private int searchLocation = ROOT;
	private int match = NONE;
	private String os;
	private String ws;
	private String arch;

	protected void reset() {
		super.reset();
		version = null;
		optional = false;
		name = null;
		searchLocation = ROOT;
		match = NONE;
		os = null;
		ws = null;
		arch = null;
	}
	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		bindSourceLocation(node, lineTable);
		version = getNodeAttribute(node, "version");
		name = getNodeAttribute(node, "name");
		optional = getBooleanAttribute(node, "optional");
		os = getNodeAttribute(node, "os");
		ws = getNodeAttribute(node, "ws");
		arch = getNodeAttribute(node, "arch");
		String matchName = getNodeAttribute(node, "match");
		if (matchName != null) {
			for (int i = 0; i < RULE_NAME_TABLE.length; i++) {
				if (matchName.equals(RULE_NAME_TABLE[i])) {
					match = i;
					break;
				}
			}
		}
		String searchLocationName = getNodeAttribute(node, "search_location");
		if (searchLocationName == null)
			searchLocationName = getNodeAttribute(node, "search-location");
		if (searchLocationName != null) {
			if (searchLocationName.equals("root"))
				searchLocation = ROOT;
			else if (searchLocationName.equals("self"))
				searchLocation = SELF;
			else if (searchLocationName.equals("both"))
				searchLocation = BOTH;
		}
		hookWithWorkspace();
	}

	public void loadFrom(IFeature feature) {
		id = feature.getId();
		version = feature.getVersion();
		optional = false;
		name = feature.getLabel();
		this.feature = feature;
	}
	/**
	 * @see IFeatureChild#getVersion()
	 */
	public String getVersion() {
		return version;
	}

	public boolean isOptional() {
		return optional;
	}

	public String getName() {
		return name;
	}

	public int getSearchLocation() {
		return searchLocation;
	}

	public int getMatch() {
		return match;
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

	public IFeature getReferencedFeature() {
		if (feature == null)
			hookWithWorkspace();
		return feature;
	}

	public void hookWithWorkspace() {
		IFeatureModel[] models =
			PDECore
				.getDefault()
				.getWorkspaceModelManager()
				.getFeatureModels();
		for (int i = 0; i < models.length; i++) {
			IFeature feature = models[i].getFeature();

			if (feature != null && feature.getId().equals(getId())) {
				if (version == null || feature.getVersion().equals(version)) {
					this.feature = feature;
					break;
				}
			}
		}
	}

	/**
	 * @see IFeatureChild#setVersion(String)
	 */
	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.version;
		this.version = version;
		firePropertyChanged(P_VERSION, oldValue, version);
		hookWithWorkspace();
	}

	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.match);
		this.match = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public void setSearchLocation(int searchLocation) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.searchLocation);
		this.searchLocation = searchLocation;
		firePropertyChanged(
			P_SEARCH_LOCATION,
			oldValue,
			new Integer(searchLocation));
	}

	public void setOptional(boolean optional) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(this.optional);
		this.optional = optional;
		firePropertyChanged(P_NAME, oldValue, new Boolean(optional));
	}
	
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.os;
		this.os = os;
		firePropertyChanged(P_OS, oldValue, os);
	}
	
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.ws;
		this.ws = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}
	
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.arch;
		this.arch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion((String) newValue);
		} else if (name.equals(P_OPTIONAL)) {
			setOptional(((Boolean) newValue).booleanValue());
		} else if (name.equals(P_NAME)) {
			setName((String) newValue);
		} else if (name.equals(P_MATCH)) {
			setMatch(newValue != null ? ((Integer) newValue).intValue() : NONE);
		} else if (name.equals(P_OS)) {
			setOS((String)newValue);
		} else if (name.equals(P_WS)) {
			setWS((String)newValue);
		} else if (name.equals(P_ARCH)) {
			setArch((String)newValue);
		} else if (name.equals(P_SEARCH_LOCATION)) {
			setSearchLocation(
				newValue != null ? ((Integer) newValue).intValue() : ROOT);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void setId(String id) throws CoreException {
		super.setId(id);
		hookWithWorkspace();
	}

	/**
	 * @see IWritable#write(String, PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<includes");
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		if (getId() != null) {
			writer.println();
			writer.print(indent2 + "id=\"" + getId() + "\"");
		}
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\"");
		}
		if (getName() != null) {
			writer.println();
			writer.print(indent2 + "name=\"" + getName() + "\"");
		}
		if (isOptional()) {
			writer.println();
			writer.print(indent2 + "optional=\"true\"");
		}
		if (match!=NONE) {
			writer.println();
			writer.print(indent2 + "match=\""+RULE_NAME_TABLE[match]+"\"");
		}
		if (getOS() != null) {
			writer.println();
			writer.print(indent2 + "os=\""+getOS() + "\"");
		}
		if (getWS() != null) {
			writer.println();
			writer.print(indent2 + "ws=\""+getWS() + "\"");
		}
		if (getArch() != null) {
			writer.println();
			writer.print(indent2 + "arch=\""+getArch() + "\"");
		}
		if (searchLocation!=ROOT) {
			writer.println();
			String value=searchLocation==SELF?"self":"both";
			writer.print(indent2 + "search_location=\""+value+"\"");
		}
		writer.println("/>");
	}
}