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
package org.eclipse.pde.internal.core.search;

import java.util.*;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

public class PluginSearchScope {

	public static final int SCOPE_WORKSPACE = 0;
	public static final int SCOPE_SELECTION = 1;
	public static final int SCOPE_WORKING_SETS = 2;
	
	public static final int EXTERNAL_SCOPE_NONE = 0;
	public static final int EXTERNAL_SCOPE_ENABLED = 1;
	public static final int EXTERNAL_SCOPE_ALL = 2;
	
	private int workspaceScope;
	private int externalScope;
	private HashSet selectedResources;
	
	/**
	 * Create a scope object with the provided arguments.
	 * @param workspaceScope  one of SCOPE_WORKSPACE, SCOPE_SELECTION,
	 * SCOPE_WORKING_SETS
	 * @param externalScope  one of EXTERNAL_SCOPE_NONE, EXTERNAL_SCOPE_ENABLED,
	 * EXTERNAL_SCOPE_ALL
	 * @param workingSets  goes with SCOPE_WORKING_SETS, otherwise null
	 */
	public PluginSearchScope(
		int workspaceScope,
		int externalScope,
		HashSet selectedResources) {
			this.workspaceScope = workspaceScope;
			this.externalScope = externalScope;
			this.selectedResources = selectedResources;
	}
	
	
	/**
	 * Creates a default scope object that will return all the entries in the
	 * PluginSearchScope.  It is equivalent to workspace scope being set to
	 * 'Workspace' and external scope being set to 'Only Enabled'
	 */
	public PluginSearchScope() {
		this(SCOPE_WORKSPACE, EXTERNAL_SCOPE_ENABLED, null);
	}
	
	private void addExternalModel(IPluginModelBase candidate, ArrayList result) {
		if (externalScope == EXTERNAL_SCOPE_ALL)
			result.add(candidate);
		else if (externalScope == EXTERNAL_SCOPE_ENABLED && candidate.isEnabled())
			result.add(candidate);
	}
	
	private void addWorkspaceModel(IPluginModelBase candidate, ArrayList result) {
		if (workspaceScope == SCOPE_WORKSPACE) {
			result.add(candidate);
		} else if (selectedResources.contains(candidate.getUnderlyingResource().getProject())) {
				result.add(candidate);
		}
	}
		
	public IPluginModelBase[] getMatchingModels() {
		ArrayList result = new ArrayList();
		PluginModelManager modelManager =
			PDECore.getDefault().getModelManager();
		ModelEntry[] entries = modelManager.getEntries();
		for (int i = 0; i < entries.length; i++) {
			IPluginModelBase candidate = entries[i].getActiveModel();
			if (candidate.getUnderlyingResource() != null) {
				addWorkspaceModel(candidate, result);
			} else {
				addExternalModel(candidate, result);
			}
		}		
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}
	
}
