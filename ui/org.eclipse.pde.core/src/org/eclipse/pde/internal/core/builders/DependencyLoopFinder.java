/*******************************************************************************
 *  Copyright (c) 2000, 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.builders;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECoreMessages;

public class DependencyLoopFinder {

	/**
	 * Upper bound on the number of reported loops. The search runs
	 * synchronously on the UI thread and the number of distinct cycles through
	 * a plug-in can grow exponentially with the size of the tangle it sits in.
	 */
	private static final int MAX_LOOPS = 100;

	public static DependencyLoop[] findLoops(IPlugin root) {
		return findLoops(root, null);
	}

	public static DependencyLoop[] findLoops(IPlugin root, IPlugin[] candidates) {
		return findLoops(root, candidates, false);
	}

	public static DependencyLoop[] findLoops(IPlugin root, IPlugin[] candidates, boolean onlyCandidates) {
		LoopSearch search = new LoopSearch(root.getId());
		List<IPlugin> rootDependencies = new ArrayList<>();
		if (!onlyCandidates) {
			rootDependencies.addAll(search.dependenciesOf(root));
		}
		if (candidates != null) {
			rootDependencies.addAll(Arrays.asList(candidates));
		}
		search.restrictToLoopMembers(rootDependencies);
		search.collectLoops(root, rootDependencies);
		return search.loops();
	}

	/**
	 * Enumerates the cycles that pass through one root plug-in.
	 */
	private static final class LoopSearch {

		private final String rootId;
		private final List<DependencyLoop> loops = new ArrayList<>();
		private final List<IPlugin> path = new ArrayList<>();
		private final Map<String, List<IPlugin>> dependencies = new HashMap<>();
		private Set<String> loopMembers = Set.of();

		LoopSearch(String rootId) {
			this.rootId = rootId;
		}

		/**
		 * Narrows the search to the plug-ins that can actually sit on a cycle
		 * through the root, that is those reachable from the root that also
		 * lead back to it. Unlike a "this plug-in yielded no loop" blacklist,
		 * this property does not depend on the path taken to reach a plug-in,
		 * so pruning by it cannot hide a cycle.
		 */
		void restrictToLoopMembers(List<IPlugin> rootDependencies) {
			Map<String, List<String>> dependents = new HashMap<>();
			Set<String> reached = new HashSet<>();
			Deque<IPlugin> pending = new ArrayDeque<>();
			for (IPlugin dependency : rootDependencies) {
				dependents.computeIfAbsent(dependency.getId(), id -> new ArrayList<>()).add(rootId);
				if (reached.add(dependency.getId())) {
					pending.add(dependency);
				}
			}
			while (!pending.isEmpty()) {
				IPlugin plugin = pending.remove();
				for (IPlugin dependency : dependenciesOf(plugin)) {
					dependents.computeIfAbsent(dependency.getId(), id -> new ArrayList<>()).add(plugin.getId());
					if (reached.add(dependency.getId())) {
						pending.add(dependency);
					}
				}
			}
			// walking the collected edges backwards from the root yields the
			// plug-ins that lead back to it
			loopMembers = new HashSet<>();
			Deque<String> backwards = new ArrayDeque<>();
			backwards.add(rootId);
			while (!backwards.isEmpty()) {
				for (String dependent : dependents.getOrDefault(backwards.remove(), List.of())) {
					if (!dependent.equals(rootId) && loopMembers.add(dependent)) {
						backwards.add(dependent);
					}
				}
			}
		}

		void collectLoops(IPlugin plugin, List<IPlugin> pluginDependencies) {
			path.add(plugin);
			for (IPlugin dependency : pluginDependencies) {
				if (loops.size() >= MAX_LOOPS) {
					break;
				}
				String id = dependency.getId();
				if (rootId.equals(id)) {
					addLoop();
				} else if (loopMembers.contains(id) && !isOnPath(id)) {
					collectLoops(dependency, dependenciesOf(dependency));
				}
			}
			path.remove(path.size() - 1);
		}

		/**
		 * Returns the plug-ins required by the given one, resolved through the
		 * registry. Cached, as the search visits a plug-in once per path
		 * leading to it.
		 */
		List<IPlugin> dependenciesOf(IPlugin plugin) {
			return dependencies.computeIfAbsent(plugin.getId(), id -> {
				List<IPlugin> resolved = new ArrayList<>();
				for (IPluginImport iimport : plugin.getImports()) {
					String importedId = iimport.getId();
					//Be paranoid
					if (importedId == null) {
						continue;
					}
					IPlugin imported = findPlugin(importedId);
					if (imported != null) {
						resolved.add(imported);
					}
				}
				return resolved;
			});
		}

		private boolean isOnPath(String id) {
			return path.stream().anyMatch(plugin -> id.equals(plugin.getId()));
		}

		private void addLoop() {
			DependencyLoop loop = new DependencyLoop();
			loop.setMembers(path.toArray(new IPlugin[path.size()]));
			int no = loops.size() + 1;
			loop.setName(NLS.bind(PDECoreMessages.Builders_DependencyLoopFinder_loopName, ("" + no))); //$NON-NLS-1$
			loops.add(loop);
		}

		DependencyLoop[] loops() {
			return loops.toArray(new DependencyLoop[loops.size()]);
		}
	}

	private static IPlugin findPlugin(String id) {
		IPluginModelBase childModel = PluginRegistry.findModel(id);
		if (childModel == null || !(childModel instanceof IPluginModel)) {
			return null;
		}
		return (IPlugin) childModel.getPluginBase();
	}
}
