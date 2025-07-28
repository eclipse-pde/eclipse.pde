/*******************************************************************************
 * Copyright (c) 2019 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.internal.core.ClasspathContainerState;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;

@SuppressWarnings("restriction")
public class DSLibPluginModelListener implements IPluginModelListener {

	private static DSLibPluginModelListener INSTANCE;

	private final HashMap<IJavaProject, String> projects = new HashMap<>();

	private final HashMap<String, Integer> counts = new HashMap<>();

	private DSLibPluginModelListener() {
		PluginModelManager.getInstance().addPluginModelListener(this);
	}

	private synchronized static DSLibPluginModelListener getInstance(boolean create) {
		if (create && INSTANCE == null) {
			INSTANCE = new DSLibPluginModelListener();
		}

		return INSTANCE;
	}

	private void decrementCount(String modelId) {
		Integer oldCount = counts.get(modelId);
		if (oldCount != null) {
			if (oldCount.intValue() <= 1) {
				counts.remove(modelId);
			} else {
				counts.put(modelId, oldCount.intValue() - 1);
			}
		}
	}

	public static void addProject(IJavaProject project, String modelId) {
		DSLibPluginModelListener instance = getInstance(true);
		synchronized (instance.projects) {
			String oldModelId = instance.projects.put(project, modelId);
			Integer count = instance.counts.getOrDefault(modelId, Integer.valueOf(0));
			instance.counts.put(modelId, count.intValue() + 1);
			if (oldModelId != null) {
				instance.decrementCount(oldModelId);
			}
		}
	}

	public static void removeProject(IJavaProject project) {
		DSLibPluginModelListener instance = getInstance(false);
		if (instance != null) {
			synchronized (instance.projects) {
				String oldModelId = instance.projects.remove(project);
				if (oldModelId != null) {
					instance.decrementCount(oldModelId);
				}
			}
		}
	}

	private boolean containsModel(ModelEntry[] entries, String id) {
		for (ModelEntry entry : entries) {
			if ("org.eclipse.pde.ds.lib".equals(entry.getId())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		synchronized (projects) {
			HashSet<String> modelIds = new HashSet<>(2);
		for (String modelId : counts.keySet()) {
			if (((delta.getKind() & PluginModelDelta.ADDED) != 0 && containsModel(delta.getAddedEntries(), modelId))
					|| ((delta.getKind() & PluginModelDelta.CHANGED) != 0
							&& containsModel(delta.getChangedEntries(), modelId))
					|| ((delta.getKind() & PluginModelDelta.REMOVED) != 0
							&& containsModel(delta.getRemovedEntries(), modelId))) {
				modelIds.add(modelId);
			}
		}

		ArrayList<IProject> toUpdate = new ArrayList<>(projects.size());
			if (!modelIds.isEmpty()) {
				for (Map.Entry<IJavaProject, String> entry : projects.entrySet()) {
					IJavaProject project = entry.getKey();
					String modelId = entry.getValue();
					if (modelIds.contains(modelId)) {
						toUpdate.add(project.getProject());
					}
				}
			}
			ClasspathContainerState.requestClasspathUpdate(toUpdate);
		}
	}


	public static void dispose() {
		DSLibPluginModelListener instance = getInstance(false);
		if (instance != null) {
			PluginModelManager.getInstance().removePluginModelListener(instance);
		}
	}
}
