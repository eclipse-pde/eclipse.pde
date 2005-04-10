/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 *
 */
public class ExternalJavaSearchClasspathContainer implements IClasspathContainer {
	private SearchablePluginsManager manager;
	
	private IClasspathEntry[] fEntries;

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
		if (fEntries == null) {
			try {
				fEntries = manager.computeContainerClasspathEntries();
			}
			catch (CoreException e) {
				PDECore.logException(e);
			}
		}
		return fEntries;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return PDECoreMessages.ExternalJavaSearchClasspathContainer_description; //$NON-NLS-1$
	}

	public int getKind() {
		return K_APPLICATION;
	}

	public IPath getPath() {
		return new Path(PDECore.JAVA_SEARCH_CONTAINER_ID);
	}
	
	public void reset() {
		fEntries = null;
	}
}
