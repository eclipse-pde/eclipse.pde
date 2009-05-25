/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site.compatibility;

/**
 */
public class FeatureEntry implements IPlatformEntry {
	private final String id;
	private final String version;
	private String url;
	private String os;
	private String ws;
	private String arch;
	private String nl;
	private String match;
	private final boolean isPlugin;
	private boolean isFragment = false;
	private boolean isRequires = false;
	private Boolean unpack = null;
	private boolean optional = false;

	/**
	 * Temporary field to add provisioning filters to features
	 */
	private String filter;

	public static FeatureEntry createRequires(String id, String version, String match, String filter, boolean isPlugin) {
		FeatureEntry result = new FeatureEntry(id, version, isPlugin);
		result.match = match;
		result.isRequires = true;
		if (filter != null)
			result.setFilter(filter);
		return result;
	}

	public FeatureEntry(String id, String version, boolean isPlugin) {
		this.id = id;
		this.version = version;
		this.isPlugin = isPlugin;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String value) {
		url = value;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public boolean isPlugin() {
		return isPlugin;
	}

	public boolean isRequires() {
		return isRequires;
	}

	public boolean isFragment() {
		return isFragment;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append(isPlugin ? "Plugin: " : "Feature: "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(id != null ? id.toString() : ""); //$NON-NLS-1$
		result.append(version != null ? " " + version.toString() : ""); //$NON-NLS-1$ //$NON-NLS-2$
		return result.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FeatureEntry other = (FeatureEntry) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public void setEnvironment(String os, String ws, String arch, String nl) {
		this.os = os;
		this.ws = ws;
		this.arch = arch;
		this.nl = nl;
	}

	public void setFragment(boolean value) {
		isFragment = value;
	}

	public void setUnpack(boolean value) {
		unpack = Boolean.valueOf(value);
	}

	public boolean isUnpack() {
		return (unpack == null || unpack.booleanValue());
	}

	public boolean unpackSet() {
		return unpack != null;
	}

	public void setOptional(boolean value) {
		optional = value;
	}

	/**
	 * Temporary method to add provisioning filters to features
	 */
	public void setFilter(String filter) {
		this.filter = filter;

	}

	/**
	 * Temporary method to add provisioning filters to features
	 */
	public String getFilter() {
		return filter;
	}

	public String getMatch() {
		return match;
	}

	public boolean isOptional() {
		return optional;
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

	public String getNL() {
		return nl;
	}
}
