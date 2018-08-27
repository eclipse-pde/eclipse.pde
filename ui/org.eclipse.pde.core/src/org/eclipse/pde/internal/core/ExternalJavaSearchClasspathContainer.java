/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

public class ExternalJavaSearchClasspathContainer implements IClasspathContainer {
	private IClasspathEntry[] fEntries;

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		if (fEntries == null) {
			try {
				SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
				fEntries = manager.computeContainerClasspathEntries();
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
		return fEntries;
	}

	@Override
	public String getDescription() {
		return PDECoreMessages.ExternalJavaSearchClasspathContainer_description;
	}

	@Override
	public int getKind() {
		return K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		return PDECore.JAVA_SEARCH_CONTAINER_PATH;
	}

}
