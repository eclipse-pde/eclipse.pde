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
package org.eclipse.pde.internal.core;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RequiredPluginsClasspathContainer extends PDEClasspathContainer {
	private IPluginModelBase model;

	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public RequiredPluginsClasspathContainer(IPluginModelBase model) {
		this.model = model;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (model==null) return new IClasspathEntry[0];
		if (entries == null) {
			entries = ClasspathUtilCore.computePluginEntries(model);
			entries = verifyWithAttachmentManager(entries);
		}
		return entries;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return PDECore.getResourceString("RequiredPluginsClasspathContainer.description"); //$NON-NLS-1$
	}
}
