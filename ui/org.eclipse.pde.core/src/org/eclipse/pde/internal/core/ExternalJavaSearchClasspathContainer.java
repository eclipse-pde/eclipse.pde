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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 *
 */
public class ExternalJavaSearchClasspathContainer extends PDEClasspathContainer {
	private SearchablePluginsManager manager;

	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public ExternalJavaSearchClasspathContainer(SearchablePluginsManager manager) {
		this.manager = manager;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (manager==null) return new IClasspathEntry[0];
		if (entries == null) {
			try {
				entries = manager.computeContainerClasspathEntries();
			}
			catch (CoreException e) {
				PDECore.logException(e);
			}
			entries = verifyWithAttachmentManager(entries);
		}
		return entries;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return PDECore.getResourceString("ExternalJavaSearchClasspathContainer.description"); //$NON-NLS-1$
	}
}
