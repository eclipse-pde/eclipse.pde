/*******************************************************************************
 * Copyright (c) 2015, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Peter Kriens <peter.kriens@aqute.biz> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Neil Bartlett <njbartlett@gmail.com> - ongoing enhancements
 *     Gregory Amerson <gregory.amerson@liferay.com> - ongoing enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - ongoing enhancements
 *     Jürgen Albert <j.albert@data-in-motion.biz> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.plugins;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;

@Component(service = RepositoryListenerPlugin.class)
public class RepositoriesViewRefresher implements RepositoryListenerPlugin {

	public interface RefreshModel {
		List<RepositoryPlugin> getRepositories();

		Workspace getWorkspace();
	}

	enum State {
		IDLE,
		BUSY,
		REDO;
	}

	private static final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
	private static final Map<TreeViewer, RefreshModel> viewers = new ConcurrentHashMap<>();

	public static void refreshRepositories(final RepositoryPlugin target) {
		if (state.updateAndGet(current -> (current == State.IDLE) ? State.BUSY : State.REDO) == State.REDO) {
			return;
		}

		//
		// Since this can delay, we move this to the background
		//
			new WorkspaceJob("Updating repositories content") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					if (monitor == null)
						monitor = new NullProgressMonitor();

					Map<Workspace, List<WorkspaceTreeViewerRefreshModel>> map = viewers.entrySet().stream()
							.map(entry -> {
								Workspace workspace = entry.getValue().getWorkspace();
								return new WorkspaceTreeViewerRefreshModel(workspace, entry.getKey(), entry.getValue());
							}).filter(m -> m.workspace() != null).collect(Collectors.groupingBy(m -> m.workspace()));
					for (Entry<Workspace, List<WorkspaceTreeViewerRefreshModel>> wsentry : map.entrySet()) {

						Set<RepositoryPlugin> repos = new HashSet<>();
						if (target != null)
							repos.add(target);
						else {
							for (WorkspaceTreeViewerRefreshModel m : wsentry.getValue()) {
								repos.addAll(m.model().getRepositories());
							}
						}
						ensureLoaded(monitor, repos, wsentry.getKey());

						final Map<WorkspaceTreeViewerRefreshModel, List<RepositoryPlugin>> entryRepos = new HashMap<>();

						for (WorkspaceTreeViewerRefreshModel entry : wsentry.getValue()) {
							entryRepos.put(entry, entry.model().getRepositories());
						}

						state.set(State.BUSY);

						for (WorkspaceTreeViewerRefreshModel entry : wsentry.getValue()) {

							TreeViewer viewer = entry.viewer();
							viewer.getControl()
							.getDisplay()
							.asyncExec(() -> {
								TreePath[] expandedTreePaths = viewer.getExpandedTreePaths();

								viewer.setInput(entryRepos.get(entry));
										if (expandedTreePaths != null && expandedTreePaths.length > 0)
									viewer.setExpandedTreePaths(expandedTreePaths);
							});

						}
						if (state.getAndSet(State.IDLE) == State.REDO) {
							refreshRepositories(null);
						}
					}
					return Status.OK_STATUS;
				}
			}.schedule(1000);
	}

	private static IStatus ensureLoaded(IProgressMonitor monitor, Collection<RepositoryPlugin> repos, Workspace ws) {
		int n = 0;
		try {
			final RepositoryPlugin workspaceRepo = ws.getWorkspaceRepository();
			for (RepositoryPlugin repo : repos) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				monitor.beginTask(repo.getName(), n++);
				if (repo != workspaceRepo) {
					repo.list(null); // looks silly but is here to incur any
										// download time
					continue;
				}
				// We must safely call bnd to list workspace repo
				try {
					ws.readLocked(() -> workspaceRepo.list(null), monitor::isCanceled);
				} catch (TimeoutException | InterruptedException e) {
					return Status.error("Unable to acquire lock to refresh repository " + repo.getName(), e);
				}
			}
		} catch (Exception e) {
			return Status.error("Exception refreshing repositories", e);
		}
		return Status.OK_STATUS;
	}

	public static void addViewer(TreeViewer viewer, RefreshModel model) {
		viewers.put(viewer, model);
		refreshViewer(viewer, model);
	}

	public static void refreshViewer(TreeViewer viewer, RefreshModel model) {
		new Job("Updating repositories") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<RepositoryPlugin> repositories = model.getRepositories();
				Display.getDefault()
					.asyncExec(() -> viewer.setInput(repositories));
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	public static void removeViewer(TreeViewer viewer) {
		viewers.remove(viewer);
	}

	@Override
	public void bundleAdded(final RepositoryPlugin repository, Jar jar, File file) {
		refreshRepositories(repository);
	}

	@Override
	public void bundleRemoved(final RepositoryPlugin repository, Jar jar, File file) {
		refreshRepositories(repository);
	}

	@Override
	public void repositoryRefreshed(final RepositoryPlugin repository) {
		refreshRepositories(repository);
	}

	@Override
	public void repositoriesRefreshed() {
		refreshRepositories(null);
	}

	private static final record WorkspaceTreeViewerRefreshModel(Workspace workspace, TreeViewer viewer,
			RefreshModel model) {

	}

}
