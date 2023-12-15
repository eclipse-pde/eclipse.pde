/*******************************************************************************
 * Copyright (c) 2016, 2021 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com>  - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Raymond Auge <raymond.auge@liferay.com> - ongoing enhancements
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.templating;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateEngine;
import org.bndtools.templating.TemplateLoader;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.pde.bnd.ui.preferences.ReposPreference;
import org.eclipse.pde.bnd.ui.preferences.ReposPreferencePage;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.repository.Repository;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.service.reporter.Reporter;

@Component(name = "org.bndtools.templating.repos", property = {
	"source=workspace", Constants.SERVICE_DESCRIPTION + "=Load templates from the Workspace and Repositories",
	Constants.SERVICE_RANKING + ":Integer=" + ReposTemplateLoader.RANKING
})
public class ReposTemplateLoader implements TemplateLoader {

	static final int									RANKING			= Integer.MAX_VALUE;

	private static final String							NS_TEMPLATE		= "org.bndtools.template";

	private final ConcurrentMap<String, TemplateEngine>	engines			= new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private final List<Workspace> workspaces = new CopyOnWriteArrayList<>();

	private PromiseFactory								promiseFactory;
	private ExecutorService								localExecutor	= null;

	@Reference(target = IScopeContext.BUNDLE_SCOPE_FILTER)
	private IScopeContext bundleScope;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	void setExecutorService(ExecutorService executor) {
		this.promiseFactory = new PromiseFactory(Objects.requireNonNull(executor));
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addTemplateEngine(TemplateEngine engine, Map<String, Object> svcProps) {
		String name = (String) svcProps.get("name");
		engines.put(name, engine);
	}

	void removeTemplateEngine(@SuppressWarnings("unused") TemplateEngine engine, Map<String, Object> svcProps) {
		String name = (String) svcProps.get("name");
		engines.remove(name);
	}

	@Activate
	void activate() {
		if (promiseFactory == null) {
			localExecutor = Executors.newCachedThreadPool();
			promiseFactory = new PromiseFactory(localExecutor);
		}
	}

	@Deactivate
	void dectivate() {
		if (localExecutor != null) {
			localExecutor.shutdown();
		}
	}

	@Override
	public Promise<List<Template>> findTemplates(String templateType, final Reporter reporter) {
		final Requirement requirement = CapReqBuilder.createSimpleRequirement(NS_TEMPLATE, templateType, null)
			.buildSyntheticRequirement();

		// Try to get the repositories and BundleLocator from the workspace
		List<Repository> workspaceRepos = new ArrayList<>();
		BundleLocator tmpLocator;
		try {
			List<RepositoryPlugin> wsRepo = new ArrayList<>();
			for (Workspace workspace : workspaces) {
				workspaceRepos.addAll(workspace.getPlugins(Repository.class));
				wsRepo.addAll(workspace.getRepositories());
			}
			tmpLocator = new RepoPluginsBundleLocator(wsRepo);
		} catch (Exception e) {
			workspaceRepos = Collections.emptyList();
			tmpLocator = new DirectDownloadBundleLocator();
		}
		final BundleLocator locator = tmpLocator;

		// Setup the repos
		List<Repository> repos = new ArrayList<>(workspaceRepos.size() + 1);
		repos.addAll(workspaceRepos);
		addPreferenceConfiguredRepos(repos, reporter);

		// Map List<Repository> to Promise<List<Template>>
		Promise<List<Template>> promise = repos.stream()
			.map(repo -> promiseFactory.submit(() -> {
				Map<Requirement, Collection<Capability>> providerMap = repo
					.findProviders(Collections.singleton(requirement));
				return providerMap.get(requirement)
					.stream()
					.map(cap -> {
						IdentityCapability idcap = ResourceUtils.getIdentityCapability(cap.getResource());
						Object id = idcap.getAttributes()
							.get(IdentityNamespace.IDENTITY_NAMESPACE);
						Object ver = idcap.getAttributes()
							.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
						try {
							String engineName = (String) cap.getAttributes()
								.get("engine");
							if (engineName == null)
								engineName = "stringtemplate";
							TemplateEngine engine = engines.get(engineName);
							if (engine != null)
								return new CapabilityBasedTemplate(cap, locator, engine);
							reporter.error(
								"Error loading template from resource '%s' version %s: no Template Engine available matching '%s'",
								id, ver, engineName);
						} catch (Exception e) {
							reporter.error("Error loading template from resource '%s' version %s: %s", id, ver,
								e.getMessage());
						}
						return null;
					})
					.filter(Objects::nonNull)
					.collect(toList());
			}))
			.collect(promiseFactory.toPromise())
			.map(ll -> ll.stream()
				.flatMap(List::stream)
				.collect(toList()));

		return promise;
	}

	private void addPreferenceConfiguredRepos(List<Repository> repos, Reporter reporter) {

		IEclipsePreferences preferences = bundleScope.getNode(ReposPreference.TEMPLATE_LOADER_NODE);
		if (preferences.getBoolean(ReposPreferencePage.KEY_ENABLE_TEMPLATE_REPOSITORIES,
				ReposPreference.DEF_ENABLE_TEMPLATE_REPOSITORIES)) {
			List<String> list = ReposPreference.TEMPLATE_REPOSITORIES_PARSER
					.apply(preferences.get(ReposPreference.KEY_TEMPLATE_REPO_URI_LIST, ""));
			if (!list.isEmpty()) {
				try {
					OSGiRepository prefsRepo = loadRepo(list, reporter);
					repos.add(prefsRepo);
				} catch (Exception ex) {
					reporter.exception(ex, "Error loading preference repository: %s", list);
				}
			}
		}
	}

	private OSGiRepository loadRepo(List<String> uris, Reporter reporter) throws Exception {
		OSGiRepository repo = new OSGiRepository();
		repo.setReporter(reporter);
		Workspace workspace = workspaces.stream().findFirst().orElse(null);
		if (workspace != null) {
			repo.setRegistry(workspace);
		} else {
			Processor p = new Processor();
			p.addBasicPlugin(new HttpClient());
			repo.setRegistry(p);
		}
		Map<String, String> map = new HashMap<>();
		map.put("locations", uris.stream().collect(Collectors.joining(",")));
		repo.setProperties(map);
		return repo;
	}
}