/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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

import java.util.Vector;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECoreMessages;

public class DependencyLoopFinder {

	public static DependencyLoop[] findLoops(IPlugin root) {
		return findLoops(root, null);
	}

	public static DependencyLoop[] findLoops(IPlugin root, IPlugin[] candidates) {
		return findLoops(root, candidates, false);
	}

	public static DependencyLoop[] findLoops(IPlugin root, IPlugin[] candidates, boolean onlyCandidates) {
		Vector<DependencyLoop> loops = new Vector<>();

		Vector<IPlugin> path = new Vector<>();
		findLoops(loops, path, root, candidates, onlyCandidates, new Vector<String>());
		return loops.toArray(new DependencyLoop[loops.size()]);
	}

	private static void findLoops(Vector<DependencyLoop> loops, Vector<IPlugin> path, IPlugin subroot, IPlugin[] candidates, boolean onlyCandidates, Vector<String> exploredPlugins) {
		if (!path.isEmpty()) {
			// test the path so far
			// is the subroot the same as root - if yes, that's it

			IPlugin root = path.elementAt(0);
			if (isEquivalent(root, subroot)) {
				// our loop!!
				DependencyLoop loop = new DependencyLoop();
				loop.setMembers(path.toArray(new IPlugin[path.size()]));
				int no = loops.size() + 1;
				loop.setName(NLS.bind(PDECoreMessages.Builders_DependencyLoopFinder_loopName, ("" + no))); //$NON-NLS-1$
				loops.add(loop);
				return;
			}
			// is the subroot the same as any other node?
			// if yes, abort - local loop that is not ours
			for (int i = 1; i < path.size(); i++) {
				IPlugin node = path.elementAt(i);
				if (isEquivalent(subroot, node)) {
					// local loop
					return;
				}
			}
		}
		@SuppressWarnings("unchecked")
		Vector<IPlugin> newPath = !path.isEmpty() ? ((Vector<IPlugin>) path.clone()) : path;
		newPath.add(subroot);

		if (!onlyCandidates) {
			IPluginImport[] iimports = subroot.getImports();
			for (IPluginImport iimport : iimports) {
				String id = iimport.getId();
				//Be paranoid
				if (id == null) {
					continue;
				}
				if (!exploredPlugins.contains(id)) {
					// is plugin in list of non loop yielding plugins
					//Commenting linear lookup - was very slow
					//when called from here. We will use
					//model manager instead because it
					//has a hash table lookup that is much faster.
					//IPlugin child = PDECore.getDefault().findPlugin(id);
					IPlugin child = findPlugin(id);
					if (child != null) {
						// number of loops before traversing plugin
						int oldLoopSize = loops.size();

						findLoops(loops, newPath, child, null, false, exploredPlugins);

						// number of loops after traversing plugin
						int newLoopsSize = loops.size();

						if (oldLoopSize == newLoopsSize) {// no change in number of loops
							// no loops from going to this node, skip next time
							exploredPlugins.add(id);
						}
					}
				}

			}

		}
		if (candidates != null) {
			for (IPlugin candidate : candidates) {
				// number of loops before traversing plugin
				int oldLoopSize = loops.size();

				findLoops(loops, newPath, candidate, null, false, exploredPlugins);

				// number of loops after traversing plugin
				int newLoopsSize = loops.size();

				if (oldLoopSize == newLoopsSize) { // no change in number of loops
					// no loops from going to this node, skip next time
					exploredPlugins.add(candidate.getId());
				}
			}
		}

	}

	private static IPlugin findPlugin(String id) {
		IPluginModelBase childModel = PluginRegistry.findModel(id);
		if (childModel == null || !(childModel instanceof IPluginModel)) {
			return null;
		}
		return (IPlugin) childModel.getPluginBase();
	}

	private static boolean isEquivalent(IPlugin left, IPlugin right) {
		return left.getId().equals(right.getId());
	}
}
