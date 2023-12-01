/*******************************************************************************
 * Copyright (c) 2023 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.metadata.expression.QueryResult;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * In-Memory representation of a artifact repository based on a non-IU target
 * location. This repository is used during the planner resolution of an IU
 * target location to supply artifacts from other (non-IU) locations.
 */
@SuppressWarnings("restriction")
public class FileArtifactRepository extends AbstractArtifactRepository {
	private static final String NAME = "Non-IU Artifact Repository";
	private static final String DESCRIPTION = """
			In-Memory repository created for a single Non-IU repository, used
			during the planner resolution of a real IU repository, in order to
			grant access to external, installable units.
			""";
	private static final String SCHEME = "memory://"; //$NON-NLS-1$
	private final Map<IArtifactDescriptor, File> artifacts = new HashMap<>();

	public FileArtifactRepository(IProvisioningAgent agent, ITargetLocation targetLocation) {
		super(agent, NAME, null, null, URI.create(SCHEME + UUID.randomUUID()), DESCRIPTION, null, null);
		Assert.isTrue(targetLocation.isResolved());
		for (TargetBundle targetBundle : targetLocation.getBundles()) {
			if (!targetBundle.getStatus().isOK()) {
				PDECore.log(Status.warning("Target bundle is not resolved: " + targetBundle));
				continue;
			}
			BundleInfo bundleInfo = targetBundle.getBundleInfo();
			URI bundleLocation = bundleInfo.getLocation();
			if (bundleLocation == null) {
				PDECore.log(Status.warning("Bundle location not found for: " + bundleInfo));
				continue;
			}
			File bundleFile = new File(bundleLocation);
			if (!bundleFile.exists()) {
				PDECore.log(Status.warning("Bundle file doesn't exist: " + bundleFile));
				continue;
			}
			IArtifactKey artifactKey = BundlesAction.createBundleArtifactKey(bundleInfo.getSymbolicName(),
					bundleInfo.getVersion());
			IArtifactDescriptor artifactDesriptor = new ArtifactDescriptor(artifactKey);
			if (artifacts.containsKey(artifactDesriptor)) {
				PDECore.log(Status.warning("Artifact already exists: " + artifactDesriptor));
				continue;
			}
			artifacts.put(artifactDesriptor, bundleFile);
		}
		for (TargetFeature targetFeature : targetLocation.getFeatures()) {
			String featureLocation = ((IFeatureModel) targetFeature.getFeatureModel()).getInstallLocation();
			if (featureLocation == null) {
				PDECore.log(Status.warning("Feature location not found for: " + targetFeature));
				continue;
			}
			File featureFile = new File(featureLocation);
			if (!featureFile.exists()) {
				PDECore.log(Status.warning("Feature file doesn't exist: " + featureFile));
				continue;
			}
			IArtifactKey artifactKey = FeaturesAction.createFeatureArtifactKey(targetFeature.getId(),
					targetFeature.getVersion());
			IArtifactDescriptor artifactDesriptor = new ArtifactDescriptor(artifactKey);
			if (artifacts.containsKey(artifactDesriptor)) {
				PDECore.log(Status.warning("Artifact already exists: " + artifactDesriptor));
				continue;
			}
			artifacts.put(artifactDesriptor, featureFile);
		}
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		File artifactFile = artifacts.get(descriptor);
		if (artifactFile == null) {
			return Status.error("Artifact not found: " + descriptor);
		}
		try (FileInputStream is = new FileInputStream(artifactFile)) {
			is.transferTo(destination);
			return Status.OK_STATUS;
		} catch (IOException e) {
			return Status.error(e.getLocalizedMessage(), e);
		}
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		return (query, monitor) -> query.perform(artifacts.keySet().iterator());
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		return new QueryResult<>(artifacts.keySet().stream().map(IArtifactDescriptor::getArtifactKey).iterator());
	}

	@Override
	public boolean contains(IArtifactDescriptor descriptor) {
		return artifacts.containsValue(descriptor);
	}

	@Override
	public boolean contains(IArtifactKey key) {
		return artifacts.keySet().stream().anyMatch(descriptor -> key.equals(descriptor.getArtifactKey()));
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return getRawArtifact(descriptor, destination, monitor);
	}

	@Override
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return artifacts.keySet().stream() //
				.filter(descriptor -> key.equals(descriptor.getArtifactKey())) //
				.toArray(IArtifactDescriptor[]::new);
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		MultiStatus multiStatus = new MultiStatus(getClass(), IStatus.INFO, "");
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		for (IArtifactRequest request : requests) {
			request.perform(this, subMonitor.split(1));
			multiStatus.add(request.getResult());
		}
		return Status.OK_STATUS;
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		throw new ProvisionException("Artifact repository must not be modified!");
	}

}
