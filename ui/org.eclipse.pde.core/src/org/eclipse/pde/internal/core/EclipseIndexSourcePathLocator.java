/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.index.P2Index;
import org.eclipse.pde.internal.core.index.P2Index.Repository;
import org.eclipse.pde.internal.core.index.P2IndexImpl;

/**
 * A plugin source path locator that queries the "Eclipse Index"
 * (<a href="https://download.eclipse.org/oomph/index/">https://download.eclipse.org/oomph/index/</a>)
 * for a matching source bundle if none can be found otherwise. If a source
 * bundle is found in one of the p2 repositories known to the index, it is
 * downloaded into the target bundle pool so subsequent lookups can use the
 * regular {@link EclipsePluginSourcePathLocator}.
 * <p>
 * This locator only becomes active if the user explicitly enabled the
 * {@link ICoreConstants#QUERY_ECLIPSE_INDEX_FOR_SOURCE_BUNDLES} preference as
 * it requires network access.
 * </p>
 */
public class EclipseIndexSourcePathLocator implements IPluginSourcePathLocator {

	private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$

	/**
	 * Caches the outcome (potentially empty) of a lookup for a given plugin id
	 * and version so that repeated requests for the same plugin don't cause
	 * repeated network round-trips.
	 */
	private final Map<String, Optional<IPath>> cache = new ConcurrentHashMap<>();

	private volatile P2Index index;

	@Override
	public IPath locateSource(IPluginBase plugin) {
		if (!PDECore.getDefault().getPreferencesManager()
				.getBoolean(ICoreConstants.QUERY_ECLIPSE_INDEX_FOR_SOURCE_BUNDLES)) {
			return null;
		}
		String id = plugin.getId();
		String version = plugin.getVersion();
		if (id == null || version == null) {
			return null;
		}
		String key = id + '_' + version;
		return cache.computeIfAbsent(key, k -> {
			try {
				return Optional.ofNullable(findAndDownloadSource(id, version));
			} catch (RuntimeException | LinkageError e) {
				PDECore.log(Status.warning(
						"Failed to query the Eclipse Index for a source bundle for " + id + '_' + version, e)); //$NON-NLS-1$
				return Optional.empty();
			}
		}).orElse(null);
	}

	private IPath findAndDownloadSource(String id, String hostVersion) {
		String sourceId = id + SourceBundleDownloader.SOURCE_SUFFIX;
		String hostBase = SourceBundleDownloader.baseVersion(hostVersion);
		Map<Repository, Set<Version>> capabilities = getIndex().lookupCapabilities(CAPABILITY_NS_OSGI_BUNDLE,
				sourceId);
		List<Candidate> candidates = new ArrayList<>();
		for (Map.Entry<Repository, Set<Version>> entry : capabilities.entrySet()) {
			for (Version v : entry.getValue()) {
				if (v.isOSGiCompatible() && SourceBundleDownloader.baseVersion(v.getOriginal()).equals(hostBase)) {
					candidates.add(new Candidate(entry.getKey(), v));
				}
			}
		}
		// Try the candidate(s) with the highest (most recent) qualifier first
		candidates.sort(Comparator.comparing((Candidate c) -> c.version).reversed());
		for (Candidate candidate : candidates) {
			IPath result = tryDownload(candidate, sourceId);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private IPath tryDownload(Candidate candidate, String sourceId) {
		try {
			URI location = new URI(candidate.repository.getLocation().toString());
			return SourceBundleDownloader.tryDownload(location, sourceId, candidate.version);
		} catch (Exception e) {
			PDECore.log(Status.warning(
					"Failed to download source bundle " + sourceId + " from " + candidate.repository.getLocation(), //$NON-NLS-1$ //$NON-NLS-2$
					e));
		}
		return null;
	}

	private P2Index getIndex() {
		P2Index result = index;
		if (result == null) {
			synchronized (this) {
				result = index;
				if (result == null) {
					File cacheDir = PDECore.getDefault().getStateLocation().append("eclipseIndex").toFile(); //$NON-NLS-1$
					index = result = new P2IndexImpl(cacheDir);
				}
			}
		}
		return result;
	}

	private static final class Candidate {
		final Repository repository;
		final Version version;

		Candidate(Repository repository, Version version) {
			this.repository = repository;
			this.version = version;
		}
	}

}
