/*******************************************************************************
 * Copyright (c) 2010, 2023 bndtools project and others.
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
 *     PK Søreide <per.kristian.soreide@gmail.com> - ongoing enhancements
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.views.resolution;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.bnd.ui.model.resolution.CapReq;
import org.eclipse.pde.bnd.ui.model.resolution.CapReqLoader;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.resource.ResourceUtils;

class AnalyseBundleResolutionJob extends Job {

	private final Set<? extends CapReqLoader>		loaders;

	private Map<String, Collection<RequirementWrapper>> requirements;
	private Map<String, Collection<Capability>> capabilities;
	private final EE										ee;

	public AnalyseBundleResolutionJob(String name, Set<? extends CapReqLoader> loaders) {
		this(name, loaders, null);
	}

	public AnalyseBundleResolutionJob(String name, Set<? extends CapReqLoader> loaders, EE ee) {
		super(name);
		this.loaders = loaders;
		this.ee = ee;
	}

	private static <K, V> void mergeMaps(Map<K, Collection<V>> from, Map<K, Collection<V>> into) {
		for (Entry<K, Collection<V>> entry : from.entrySet()) {
			K key = entry.getKey();
			into.merge(key, entry.getValue(), (a, b) -> Stream.concat(a.stream(), b.stream()).distinct().toList());
		}
	}

	private static <K, V, M> void mergeMapsWithMapping(Map<K, Collection<V>> from, Map<K, Collection<M>> into,
			Function<V, M> mapper) {
		for (Entry<K, Collection<V>> entry : from.entrySet()) {
			K key = entry.getKey();
			into.merge(key, entry.getValue().stream().map(mapper).toList(),
					(a, b) -> Stream.concat(a.stream(), b.stream()).distinct().toList());
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			// Load all the capabilities and requirements
			Map<String, Collection<Capability>> allCaps = new HashMap<>();
			Map<String, Collection<RequirementWrapper>> allReqs = new HashMap<>();
			for (CapReqLoader loader : loaders) {
				try (loader){
					CapReq loaded = loader.loadCapReq();
					mergeMaps(loaded.capabilities(), allCaps);
					mergeMapsWithMapping(loaded.requirements(), allReqs, req -> new RequirementWrapper(req));
				} catch (Exception e) {
					ILog.get().error("Error in Bnd resolution analysis.", e);
				}
			}

			// Check for resolved requirements
			for (String namespace : allReqs.keySet()) {
				Collection<RequirementWrapper> rws = allReqs.getOrDefault(namespace, emptyList());
				Collection<Capability> candidates = allCaps.getOrDefault(namespace, emptyList());

				List<Capability> javaCandidates = ee == null ? emptyList()
					: ee.getResource()
						.getCapabilities(namespace);

				outer: for (RequirementWrapper rw : rws) {
					String filterDirective = rw.requirement.getDirectives()
						.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					if (filterDirective == null) {
						continue;
					}
					Predicate<Capability> predicate = ResourceUtils.filterMatcher(rw.requirement);
					for (Capability cand : candidates) {
						if (predicate.test(cand)) {
							rw.resolved = true;
							continue outer;
						}
					}
					for (Capability cand : javaCandidates) {
						if (predicate.test(cand)) {
							rw.java = true;
							continue outer;
						}
					}
				}
			}

			// Generate the final results
			this.requirements = allReqs;
			this.capabilities = allCaps;
			return Status.OK_STATUS;
		} catch (RuntimeException e) {
		     throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, Collection<RequirementWrapper>> getRequirements() {
		return Collections.unmodifiableMap(requirements);
	}

	public Map<String, Collection<Capability>> getCapabilities() {
		return Collections.unmodifiableMap(capabilities);
	}
}
