/*******************************************************************************
 * Copyright (c) 2010, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Gregory Amerson <gregory.amerson@liferay.com> - ongoing enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - ongoing enhancements
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.repo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.bnd.ui.plugins.EclipseWorkspaceRepository;
import org.eclipse.swt.widgets.Display;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.Builder;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.version.Version;

public class RepositoryTreeContentProvider implements ITreeContentProvider {

	private static final String									CACHE_REPOSITORY		= "cache";

	private final EnumSet<ResolutionPhase>						phases;

	private String												rawFilter				= null;
	private String												wildcardFilter			= null;
	/**
	 * Number of filter results to keep per repo. This is to avoid memory leaks
	 * if you search with lots of different filter strings.
	 */
	private static final int									MAX_CACHED_FILTER_RESULTS	= 10;
	private boolean												showRepos				= true;

	private Requirement											requirementFilter		= null;

	private final Map<RepositoryPlugin, Map<String, Object[]>>	repoPluginListResults	= new HashMap<>();
	private StructuredViewer									structuredViewer;

	public RepositoryTreeContentProvider() {
		this.phases = EnumSet.allOf(ResolutionPhase.class);
	}

	public RepositoryTreeContentProvider(ResolutionPhase mode) {
		this.phases = EnumSet.of(mode);
	}

	public RepositoryTreeContentProvider(EnumSet<ResolutionPhase> modes) {
		this.phases = modes;
	}

	public String getFilter() {
		return rawFilter;
	}

	public void setFilter(String filter) {
		this.rawFilter = filter;
		if (filter == null || filter.length() == 0 || filter.trim()
			.equals("*")) {
			wildcardFilter = null;
		} else {
			wildcardFilter = "*" + filter.trim() + "*";
		}
	}

	public void setRequirementFilter(Requirement requirement) {
		this.requirementFilter = requirement;
	}

	public void setShowRepos(boolean showRepos) {
		this.showRepos = showRepos;
	}

	public boolean isShowRepos() {
		return showRepos;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		Collection<Object> result;
		if (inputElement instanceof Workspace workspace) {
			result = new ArrayList<>();
			addRepositoryPlugins(result, workspace);
		} else if (inputElement instanceof Collection) {
			result = new ArrayList<>();
			addCollection(result, (Collection<Object>) inputElement);
		} else if (inputElement instanceof Object[]) {
			result = new ArrayList<>();
			addCollection(result, Arrays.asList(inputElement));
		} else {
			result = Collections.emptyList();
		}

		return result.toArray();
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof StructuredViewer) {
			this.structuredViewer = (StructuredViewer) viewer;

			// only clear during subsequent updates
			if (oldInput != null) {
				repoPluginListResults.clear();
			}
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] result = null;

		if (parentElement instanceof RepositoryPlugin repo) {
			result = getRepositoryBundles(repo);
		} else if (parentElement instanceof RepositoryBundle bundle) {
			result = getRepositoryBundleVersions(bundle);
		} else if (parentElement instanceof Project project) {
			result = getProjectBundles(project);
		}

		return result;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof RepositoryBundle) {
			return ((RepositoryBundle) element).getRepo();
		}
		if (element instanceof RepositoryBundleVersion) {
			return ((RepositoryBundleVersion) element).getParentBundle();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof RepositoryPlugin || element instanceof RepositoryBundle || element instanceof Project;
	}

	private void addRepositoryPlugins(Collection<Object> result, Workspace workspace) {
		workspace.getErrors()
			.clear();
		List<RepositoryPlugin> repoPlugins = workspace.getPlugins(RepositoryPlugin.class);
		ILog log = ILog.get();
		for (String error : workspace.getErrors()) {
			log.error(error);
		}
		for (RepositoryPlugin repoPlugin : repoPlugins) {
			if (CACHE_REPOSITORY.equals(repoPlugin.getName())) {
				continue;
			}
			if (repoPlugin instanceof IndexProvider indexProvider) {
				if (!supportsPhase(indexProvider)) {
					continue;
				}
			}
			if (showRepos) {
				result.add(repoPlugin);
			} else {
				result.addAll(Arrays.asList(getRepositoryBundles(repoPlugin)));
			}
		}
	}

	private void addCollection(Collection<Object> result, Collection<Object> inputs) {
		for (Object input : inputs) {
			if (input instanceof RepositoryPlugin repo) {
				if (repo instanceof IndexProvider) {
					if (!supportsPhase((IndexProvider) repo)) {
						continue;
					}
				}

				if (showRepos) {
					result.add(repo);
				} else {
					Object[] bundles = getRepositoryBundles(repo);
					if (bundles != null && bundles.length > 0) {
						result.addAll(Arrays.asList(bundles));
					}
				}
			}
		}
	}

	private boolean supportsPhase(IndexProvider provider) {
		Set<ResolutionPhase> supportedPhases = provider.getSupportedPhases();
		for (ResolutionPhase phase : phases) {
			if (supportedPhases.contains(phase)) {
				return true;
			}
		}
		return false;
	}

	Object[] getProjectBundles(Project project) {
		ProjectBundle[] result = null;
		try (ProjectBuilder pb = project.getBuilder(null)) {
			List<Builder> builders = pb.getSubBuilders();
			result = new ProjectBundle[builders.size()];

			int i = 0;
			for (Builder builder : builders) {
				ProjectBundle bundle = new ProjectBundle(project, builder.getBsn());
				result[i++] = bundle;
			}
		} catch (Exception e) {
			ILog.get().error(MessageFormat.format("Error querying sub-bundles for project {0}.", project.getName()), e);
		}
		return result;
	}

	Object[] getRepositoryBundleVersions(RepositoryBundle bundle) {
		SortedSet<Version> versions = null;
		try {
			versions = bundle.getRepo()
				.versions(bundle.getBsn());
		} catch (Exception e) {
			ILog.get().error(MessageFormat.format("Error querying versions for bundle {0} in repository {1}.",
				bundle.getBsn(), bundle.getRepo()
					.getName()),
				e);
		}
		if (versions != null) {
			Stream<RepositoryBundleVersion> resultStream = versions.stream()
				.map(version -> new RepositoryBundleVersion(bundle, version));
			// If the RepositoryBundle represents a pseudo-BSN of the form
			// group:artifact, then we don't want to display the true bundles
			// under this node.
			if (bundle.getBsn()
				.indexOf(":") != -1) {
				resultStream = resultStream.filter(rbv -> rbv.getText()
					.contains("Not a bundle"));
			}
			return resultStream.toArray(RepositoryBundleVersion[]::new);
		}
		return null;
	}

	Object[] getRepositoryBundles(final RepositoryPlugin repoPlugin) {
		Object[] result = null;

		if (requirementFilter != null) {
			if (repoPlugin instanceof Repository) {
				result = searchR5Repository(repoPlugin, (Repository) repoPlugin);
			} else if (repoPlugin instanceof WorkspaceRepository) {
				try {
					EclipseWorkspaceRepository workspaceRepo = EclipseWorkspaceRepository
							.get(ResourcesPlugin.getWorkspace());
					result = searchR5Repository(repoPlugin, workspaceRepo);
				} catch (Exception e) {
					ILog.get().error("Error querying workspace repository", e);
				}
			}
			return result;
		}

		/*
		 * We can't directly call repoPlugin.list() since we are on the UI
		 * thread so the plan is to first check to see if we have cached the
		 * list results already from a previous job, if so, return those results
		 * directly If not, then we need to create a background job that will
		 * call list() and once it is finished, we tell the Viewer to refresh
		 * this node and the next time this method gets called the 'results'
		 * will be available in the cache
		 */
		Map<String, Object[]> listResults = repoPluginListResults.computeIfAbsent(repoPlugin,
				p -> createLRUMap(MAX_CACHED_FILTER_RESULTS));

		result = listResults.get(wildcardFilter);

		if (result == null) {
			Job job = new Job("Loading " + repoPlugin.getName() + " content...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					IStatus status = Status.OK_STATUS;
					Object[] jobresult;
					List<String> bsns = null;

					try {
						bsns = repoPlugin.list(wildcardFilter);
					} catch (Exception e) {
						String message = MessageFormat.format("Error querying repository {0}.", repoPlugin.getName());
						ILog.get().error(message, e);
						status = Status.error(message, e);
					}
					if (bsns != null) {
						Collections.sort(bsns);
						jobresult = new RepositoryBundle[bsns.size()];
						int i = 0;
						for (String bsn : bsns) {
							jobresult[i++] = new RepositoryBundle(repoPlugin, bsn);
						}

						Map<String, Object[]> listResults = repoPluginListResults.computeIfAbsent(repoPlugin,
								p -> createLRUMap(MAX_CACHED_FILTER_RESULTS));
						listResults.put(wildcardFilter, jobresult);

						Display.getDefault()
							.asyncExec(() -> {
								if (!structuredViewer.getControl()
									.isDisposed()) {
									structuredViewer.refresh(repoPlugin, true);
								}
							});
					}

					return status;
				}
			};
			job.schedule();

			// wait 100 ms and see if the job will complete fast (likely already
			// cached)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}

			IStatus status = job.getResult();

			if (status != null && status.isOK()) {
				Map<String, Object[]> fastResults = repoPluginListResults.computeIfAbsent(repoPlugin,
						p -> createLRUMap(MAX_CACHED_FILTER_RESULTS));
				result = fastResults.get(wildcardFilter);
			} else {
				Object[] loading = new Object[] {
					new LoadingContentElement()
				};

				listResults.put(wildcardFilter, loading);
				result = loading;
			}
		}

		return result;
	}

	private Object[] searchR5Repository(RepositoryPlugin repoPlugin, Repository osgiRepo) {
		Object[] result;
		Set<RepositoryResourceElement> resultSet = new LinkedHashSet<>();
		Map<Requirement, Collection<Capability>> providers = osgiRepo
			.findProviders(Collections.singleton(requirementFilter));

		for (Entry<Requirement, Collection<Capability>> providersEntry : providers.entrySet()) {
			for (Capability providerCap : providersEntry.getValue()) {
				resultSet.add(new RepositoryResourceElement(repoPlugin, providerCap.getResource()));
			}
		}

		result = resultSet.toArray();
		return result;
	}
	
	
	// Define a LRU-like inner map (max n entries)
		private static Map<String, Object[]> createLRUMap(int n) {
			return new LinkedHashMap<String, Object[]>(n + 1, 1.0f, true) {
				private static final long serialVersionUID = 1L;

				@Override
		        protected boolean removeEldestEntry(Map.Entry<String, Object[]> eldest) {
					// Auto-remove oldest when size > n
					// but always keep the 'null' key which is '*'
					return size() > n && eldest.getKey() != null;
		        }
		    };
		}
}
