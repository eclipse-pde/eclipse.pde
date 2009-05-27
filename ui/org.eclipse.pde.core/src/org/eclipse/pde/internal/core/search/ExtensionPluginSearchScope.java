/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.search;

import java.util.HashSet;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;

public class ExtensionPluginSearchScope extends PluginSearchScope {

	PluginSearchInput fInput = null;

	public ExtensionPluginSearchScope(PluginSearchInput input) {
		super();
		fInput = input;
	}

	public ExtensionPluginSearchScope(int workspaceScope, int externalScope, HashSet selectedResources, PluginSearchInput input) {
		super(workspaceScope, externalScope, selectedResources);
		fInput = input;
	}

	public IPluginModelBase[] getMatchingModels() {
		if (fInput == null)
			return new IPluginModelBase[0];
		String pointId = fInput.getSearchString();
		PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
		IPluginModelBase[] models = null;
		if (fInput.getSearchLimit() == PluginSearchInput.LIMIT_REFERENCES) {
			models = registry.findExtensionPlugins(pointId, false);
		} else {
			IPluginModelBase base = registry.findExtensionPointPlugin(pointId);
			models = (base == null) ? new IPluginModelBase[0] : new IPluginModelBase[] {base};
		}
		return addRelevantModels(models);
	}

}
