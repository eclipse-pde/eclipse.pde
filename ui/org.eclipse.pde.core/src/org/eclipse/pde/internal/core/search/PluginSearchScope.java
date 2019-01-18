/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.core.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class PluginSearchScope {

	public static final int SCOPE_WORKSPACE = 0;
	public static final int SCOPE_SELECTION = 1;
	public static final int SCOPE_WORKING_SETS = 2;

	public static final int EXTERNAL_SCOPE_NONE = 0;
	public static final int EXTERNAL_SCOPE_ENABLED = 1;
	public static final int EXTERNAL_SCOPE_ALL = 2;

	private final int workspaceScope;
	private final int externalScope;
	private final HashSet<?> selectedResources;

	/**
	 * Create a scope object with the provided arguments.
	 * @param workspaceScope  one of SCOPE_WORKSPACE, SCOPE_SELECTION,
	 * SCOPE_WORKING_SETS
	 * @param externalScope  one of EXTERNAL_SCOPE_NONE, EXTERNAL_SCOPE_ENABLED,
	 * EXTERNAL_SCOPE_ALL
	 * @param selectedResources  goes with SCOPE_WORKING_SETS, otherwise null
	 */
	public PluginSearchScope(int workspaceScope, int externalScope, HashSet<?> selectedResources) {
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

	protected final void addExternalModel(IPluginModelBase candidate, ArrayList<IPluginModelBase> result) {
		if (externalScope == EXTERNAL_SCOPE_ALL) {
			result.add(candidate);
		} else if (externalScope == EXTERNAL_SCOPE_ENABLED && candidate.isEnabled()) {
			result.add(candidate);
		}
	}

	protected final void addExternalModel(final IFeatureModel candidate, final List<IFeatureModel> result) {
		if (externalScope == EXTERNAL_SCOPE_ALL) {
			result.add(candidate);
		} else if (externalScope == EXTERNAL_SCOPE_ENABLED && candidate.isEnabled()) {
			result.add(candidate);
		}
	}

	protected final void addWorkspaceModel(IPluginModelBase candidate, ArrayList<IPluginModelBase> result) {
		if (workspaceScope == SCOPE_WORKSPACE) {
			result.add(candidate);
		} else if (selectedResources.contains(candidate.getUnderlyingResource().getProject())) {
			result.add(candidate);
		}
	}

	protected final void addWorkspaceModel(final IFeatureModel candidate, final List<IFeatureModel> result) {
		if (workspaceScope == SCOPE_WORKSPACE) {
			result.add(candidate);
		} else if (selectedResources.contains(candidate.getUnderlyingResource().getProject())) {
			result.add(candidate);
		}
	}

	public IPluginModelBase[] getMatchingModels() {
		return addRelevantModels(PluginRegistry.getAllModels());
	}

	protected final IPluginModelBase[] addRelevantModels(IPluginModelBase[] models) {
		ArrayList<IPluginModelBase> result = new ArrayList<>();
		for (IPluginModelBase model : models) {
			if (model.getUnderlyingResource() != null) {
				addWorkspaceModel(model, result);
			} else {
				addExternalModel(model, result);
			}
		}
		return result.toArray(new IPluginModelBase[result.size()]);
	}

	public IFeatureModel[] getMatchingFeatureModels() {
		return addRelevantModels(PDECore.getDefault().getFeatureModelManager().getModels());
	}

	protected final IFeatureModel[] addRelevantModels(IFeatureModel[] models) {
		final List<IFeatureModel> result = new ArrayList<>();
		for (IFeatureModel model : models) {
			if (model.getUnderlyingResource() != null) {
				addWorkspaceModel(model, result);
			} else {
				addExternalModel(model, result);
			}
		}
		return result.toArray(new IFeatureModel[result.size()]);
	}
}
