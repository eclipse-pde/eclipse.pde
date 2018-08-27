/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	 * @see #PluginReference(String id, String version, int match)
	 *
	 * @param id
	 *            the id of the plug-in
	 * @since 3.9
	 */
	public PluginReference(String id) {
		this(id, null, 0);
	}

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

	@Override
	public int getMatch() {
		return match;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void setMatch(int match) throws CoreException {
		this.match = match;
	}

	@Override
	public void setVersion(String version) throws CoreException {
		this.version = version;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) throws CoreException {
		this.id = id;
	}

}