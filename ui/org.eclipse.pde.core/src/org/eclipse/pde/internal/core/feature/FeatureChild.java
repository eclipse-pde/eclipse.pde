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
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class FeatureChild extends IdentifiableObject implements IFeatureChild {
	private static final long serialVersionUID = 1L;
	private String fVersion;
	private IFeature fFeature;
	private String fName;
	private boolean fOptional;
	private int fSearchLocation = ROOT;
	private int fMatch = NONE;
	private String fOs;
	private String fWs;
	private String fArch;
	private String fNl;

	protected void reset() {
		super.reset();
		fVersion = null;
		fOptional = false;
		fName = null;
		fSearchLocation = ROOT;
		fMatch = NONE;
		fOs = null;
		fWs = null;
		fArch = null;
		fNl = null;
	}
	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		bindSourceLocation(node, lineTable);
		fVersion = getNodeAttribute(node, "version"); //$NON-NLS-1$
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fOptional = getBooleanAttribute(node, "optional"); //$NON-NLS-1$
		fOs = getNodeAttribute(node, "os"); //$NON-NLS-1$
		fWs = getNodeAttribute(node, "ws"); //$NON-NLS-1$
		fArch = getNodeAttribute(node, "arch"); //$NON-NLS-1$
		fNl = getNodeAttribute(node, "nl"); //$NON-NLS-1$
		String matchName = getNodeAttribute(node, "match"); //$NON-NLS-1$
		if (matchName != null) {
			for (int i = 0; i < RULE_NAME_TABLE.length; i++) {
				if (matchName.equals(RULE_NAME_TABLE[i])) {
					fMatch = i;
					break;
				}
			}
		}
		String searchLocationName = getNodeAttribute(node, "search_location"); //$NON-NLS-1$
		if (searchLocationName == null)
			searchLocationName = getNodeAttribute(node, "search-location"); //$NON-NLS-1$
		if (searchLocationName != null) {
			if (searchLocationName.equals("root")) //$NON-NLS-1$
				fSearchLocation = ROOT;
			else if (searchLocationName.equals("self")) //$NON-NLS-1$
				fSearchLocation = SELF;
			else if (searchLocationName.equals("both")) //$NON-NLS-1$
				fSearchLocation = BOTH;
		}
		hookWithWorkspace();
	}

	public void loadFrom(IFeature feature) {
		id = feature.getId();
		fVersion = feature.getVersion();
		fOptional = false;
		fName = feature.getLabel();
		this.fFeature = feature;
	}
	/**
	 * @see IFeatureChild#getVersion()
	 */
	public String getVersion() {
		return fVersion;
	}

	public boolean isOptional() {
		return fOptional;
	}

	public String getName() {
		return fName;
	}

	public int getSearchLocation() {
		return fSearchLocation;
	}

	public int getMatch() {
		return fMatch;
	}
	
	public String getOS() {
		return fOs;
	}
	
	public String getWS() {
		return fWs;
	}
	
	public String getArch() {
		return fArch;
	}

	public String getNL() {
		return fNl;
	}

	public IFeature getReferencedFeature() {
		if (fFeature == null)
			hookWithWorkspace();
		return fFeature;
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
				if (fVersion == null || feature.getVersion().equals(fVersion)) {
					this.fFeature = feature;
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
		Object oldValue = this.fVersion;
		this.fVersion = version;
		firePropertyChanged(P_VERSION, oldValue, version);
		hookWithWorkspace();
	}

	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fName;
		this.fName = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.fMatch);
		this.fMatch = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public void setSearchLocation(int searchLocation) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.fSearchLocation);
		this.fSearchLocation = searchLocation;
		firePropertyChanged(
			P_SEARCH_LOCATION,
			oldValue,
			new Integer(searchLocation));
	}

	public void setOptional(boolean optional) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(this.fOptional);
		this.fOptional = optional;
		firePropertyChanged(P_NAME, oldValue, new Boolean(optional));
	}
	
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fOs;
		this.fOs = os;
		firePropertyChanged(P_OS, oldValue, os);
	}
	
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fWs;
		this.fWs = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}
	
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fArch;
		this.fArch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fNl;
		this.fNl = nl;
		firePropertyChanged(P_NL, oldValue, nl);
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
		} else if (name.equals(P_NL)) {
			setNL((String)newValue);
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
		writer.print(indent + "<includes"); //$NON-NLS-1$
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		if (getId() != null) {
			writer.println();
			writer.print(indent2 + "id=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getName() != null) {
			writer.println();
			writer.print(indent2 + "name=\"" + getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (isOptional()) {
			writer.println();
			writer.print(indent2 + "optional=\"true\""); //$NON-NLS-1$
		}
		if (fMatch!=NONE) {
			writer.println();
			writer.print(indent2 + "match=\""+RULE_NAME_TABLE[fMatch]+"\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getOS() != null) {
			writer.println();
			writer.print(indent2 + "os=\""+getOS() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getWS() != null) {
			writer.println();
			writer.print(indent2 + "ws=\""+getWS() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getArch() != null) {
			writer.println();
			writer.print(indent2 + "arch=\""+getArch() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getNL() != null) {
			writer.println();
			writer.print(indent2 + "nl=\""+getNL() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fSearchLocation!=ROOT) {
			writer.println();
			String value=fSearchLocation==SELF?"self":"both"; //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(indent2 + "search-location=\""+value+"\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}
}
