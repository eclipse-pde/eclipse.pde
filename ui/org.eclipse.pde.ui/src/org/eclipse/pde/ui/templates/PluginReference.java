/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.ui.templates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginReference;

/**
 * Implementation of the IPluginReference
 * <p>
 * This class may be instantiated. This class is not intended to be sub-classed by clients.
 * </p>
 * @see IPluginReference
 * @since 3.4
 * 
 * @noextend This class is not intended to be sub-classed by clients.
 */
public class PluginReference implements IPluginReference {

	private String id;
	private String version;
	private int match = IMatchRules.NONE;

	/**
	 * 
	 * Constructs a plug-in reference to be used in the PDE template framework
	 * 
	 * @param id
	 * 			the id of the plug-in
	 * @param version
	 * 			the version of the plug-in
	 * @param match
	 * 			the match rule for the plug-in {@link IMatchRules}
	 */
	public PluginReference(String id, String version, int match) {
		this.id = id;
		this.version = version;
		this.match = match;
	}

	/*
	 * @see IPluginReference#equals(Object) 
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof IPluginReference) {
			IPluginReference source = (IPluginReference) object;
			if (id == null)
				return false;
			return id.equals(source.getId()) && ((version == null && source.getVersion() == null) || version.equals(source.getVersion()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (id != null) {
			return id.hashCode() + (version == null ? 0 : version.hashCode());
		}
		return super.hashCode();
	}

	/*
	 * @see IPluginReference#getMatch()
	 */
	public int getMatch() {
		return match;
	}

	/*
	 * @see IPluginReference#getVersion()
	 */
	public String getVersion() {
		return version;
	}

	/*
	 * @see IPluginReference#setMatch(int)
	 */
	public void setMatch(int match) throws CoreException {
		this.match = match;
	}

	/*
	 * @see IPluginReference#setVersion(String)
	 */
	public void setVersion(String version) throws CoreException {
		this.version = version;
	}

	/*
	 * @see IIdentifiable#getId()
	 */
	public String getId() {
		return id;
	}

	/*
	 * @see IIdentifiable#setId(String)
	 */
	public void setId(String id) throws CoreException {
		this.id = id;
	}

}