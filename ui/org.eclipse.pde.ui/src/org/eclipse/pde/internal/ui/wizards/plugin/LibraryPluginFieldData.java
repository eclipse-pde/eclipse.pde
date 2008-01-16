/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.core.plugin.IPluginModelBase;

public class LibraryPluginFieldData extends PluginFieldData {
	private String[] fLibraryPaths;
	private IPluginModelBase[] fPluginsToUpdate;

	private boolean fUnzipLibraries = false;
	private boolean fFindDependencies = false;
	private boolean fUpdateReferences = false;

	public String[] getLibraryPaths() {
		return fLibraryPaths;
	}

	public void setLibraryPaths(String[] libraryPaths) {
		fLibraryPaths = libraryPaths;
	}

	public boolean isUnzipLibraries() {
		return fUnzipLibraries;
	}

	public void setUnzipLibraries(boolean jarred) {
		fUnzipLibraries = jarred;
	}

	public void setFindDependencies(boolean findDependencies) {
		fFindDependencies = findDependencies;
	}

	public boolean doFindDependencies() {
		return fFindDependencies;
	}

	public boolean isUpdateReferences() {
		return fUpdateReferences;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences = update;
	}

	public void setPluginsToUpdate(IPluginModelBase[] plugins) {
		fPluginsToUpdate = plugins;
	}

	public IPluginModelBase[] getPluginsToUpdate() {
		return fPluginsToUpdate;
	}

}
