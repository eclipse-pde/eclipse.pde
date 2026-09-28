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

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.target.P2TargetUtils;

/**
 * A plugin source path locator that queries the p2 repositories already
 * known to the running IDE (e.g. sites added under <b>Available Software
 * Sites</b> or previously resolved as part of a target definition) for a
 * matching source bundle if none can be found otherwise. If a source bundle
 * is found, it is downloaded into the target bundle pool so subsequent
 * lookups can use the regular {@link EclipsePluginSourcePathLocator}.
 * <p>
 * This locator only becomes active if the user explicitly enabled the
 * {@link ICoreConstants#QUERY_KNOWN_P2_REPOSITORIES_FOR_SOURCE_BUNDLES}
 * preference as it can involve network access.
 * </p>
 */
public class KnownP2RepositoriesSourcePathLocator implements IPluginSourcePathLocator {

	/**
	 * Caches the outcome (potentially empty) of a lookup for a given plugin id
	 * and version so that repeated requests for the same plugin don't cause
	 * repeated round-trips to every known repository.
	 */
	private final Map<String, Optional<IPath>> cache = new ConcurrentHashMap<>();

	@Override
	public IPath locateSource(IPluginBase plugin) {
		if (!PDECore.getDefault().getPreferencesManager()
				.getBoolean(ICoreConstants.QUERY_KNOWN_P2_REPOSITORIES_FOR_SOURCE_BUNDLES)) {
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
			} catch (Exception | LinkageError e) {
				PDECore.log(Status.warning("Failed to query known p2 repositories for a source bundle for " + id //$NON-NLS-1$
						+ '_' + version, e));
				return Optional.empty();
			}
		}).orElse(null);
	}

	private IPath findAndDownloadSource(String id, String hostVersion) throws Exception {
		String sourceId = id + SourceBundleDownloader.SOURCE_SUFFIX;
		String hostBase = SourceBundleDownloader.baseVersion(hostVersion);
		IMetadataRepositoryManager manager = P2TargetUtils.getRepoManager();
		URI[] repositories = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		for (URI location : repositories) {
			try {
				IMetadataRepository repository = manager.loadRepository(location, new NullProgressMonitor());
				Version best = null;
				for (IInstallableUnit unit : repository.query(QueryUtil.createIUQuery(sourceId), null).toSet()) {
					Version v = unit.getVersion();
					if (SourceBundleDownloader.baseVersion(v.getOriginal()).equals(hostBase)
							&& (best == null || v.compareTo(best) > 0)) {
						best = v;
					}
				}
				if (best != null) {
					IPath result = tryDownload(location, sourceId, best);
					if (result != null) {
						// Found and downloaded a match, no need to query the remaining repositories
						return result;
					}
				}
			} catch (Exception e) {
				// This repository could not be read (e.g. not reachable), try the next one
				PDECore.log(Status.warning("Failed to query p2 repository " + location + " for source bundle " //$NON-NLS-1$ //$NON-NLS-2$
						+ sourceId, e));
			}
		}
		return null;
	}

	private IPath tryDownload(URI location, String sourceId, Version version) {
		try {
			return SourceBundleDownloader.tryDownload(location, sourceId, version);
		} catch (Exception e) {
			PDECore.log(Status.warning("Failed to download source bundle " + sourceId + " from " + location, e)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

}
