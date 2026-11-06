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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.internal.core.target.P2TargetUtils;

/**
 * Shared helper used by {@link IPluginSourcePathLocator} implementations that
 * locate a source bundle in some p2 repository and download it into the
 * target bundle pool (e.g. {@link EclipseIndexSourcePathLocator} and
 * {@link KnownP2RepositoriesSourcePathLocator}).
 */
class SourceBundleDownloader {

	static final String SOURCE_SUFFIX = ".source"; //$NON-NLS-1$

	private SourceBundleDownloader() {
	}

	/**
	 * @return the {@code major.minor.micro} part of the given version string,
	 *         ignoring the qualifier.
	 */
	static String baseVersion(String version) {
		org.osgi.framework.Version v = org.osgi.framework.Version.parseVersion(version);
		return v.getMajor() + "." + v.getMinor() + "." + v.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Attempts to find the installable unit with the given id/version in the
	 * metadata repository at the given location and, if found, mirrors its
	 * artifact into the target bundle pool.
	 *
	 * @return the path to the (possibly already cached) source bundle in the
	 *         bundle pool, or <code>null</code> if it could not be found or
	 *         downloaded
	 */
	static IPath tryDownload(URI location, String sourceId, Version version) throws Exception {
		IMetadataRepositoryManager metadataManager = P2TargetUtils.getRepoManager();
		IMetadataRepository metadataRepository = metadataManager.loadRepository(location, new NullProgressMonitor());
		IInstallableUnit unit = metadataRepository.query(QueryUtil.createIUQuery(sourceId, version), null).stream()
				.findFirst().orElse(null);
		if (unit == null) {
			return null;
		}
		IArtifactKey artifactKey = unit.getArtifacts().stream().findFirst().orElse(null);
		if (artifactKey == null) {
			return null;
		}
		IFileArtifactRepository bundlePool = P2TargetUtils.getBundlePool();
		File existing = bundlePool.getArtifactFile(artifactKey);
		if (existing != null && existing.isFile()) {
			return IPath.fromOSString(existing.getAbsolutePath());
		}
		IArtifactRepositoryManager artifactManager = P2TargetUtils.getArtifactRepositoryManager();
		IArtifactRepository sourceRepository = artifactManager.loadRepository(location, new NullProgressMonitor());
		IArtifactRequest request = artifactManager.createMirrorRequest(artifactKey, bundlePool, null, null);
		request.perform(sourceRepository, new NullProgressMonitor());
		if (request.getResult() == null || !request.getResult().isOK()) {
			return null;
		}
		File downloaded = bundlePool.getArtifactFile(artifactKey);
		if (downloaded != null && downloaded.isFile()) {
			return IPath.fromOSString(downloaded.getAbsolutePath());
		}
		return null;
	}

}
