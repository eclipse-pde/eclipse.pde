/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginReference;

public class PluginReference implements IPluginReference {
	private int match = NONE;
	private String version;
	private String id;

	/**
	 * Constructor for PluginReference.
	 */
	public PluginReference() {
		super();
	}
	
	public boolean equals(Object object) {
		if (object instanceof IPluginReference) {
			IPluginReference source = (IPluginReference)object;
			if (id==null) return false;
			if (id.equals(source.getId())==false) return false;
			if (version==null && source.getVersion()==null) return true;
			return version.equals(source.getVersion());
		}
		return false;
	}
	
	public PluginReference(String id, String version, int match) {
		this.id = id;
		this.version = version;
		this.match = match;
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
