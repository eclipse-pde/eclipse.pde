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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * In-Memory representation of a artifact repository based on a non-IU target
 * location. This repository is used during the planner resolution of an IU
 * target location to supply artifacts from other (non-IU) locations.
 */
@SuppressWarnings("restriction")
public class VirtualArtifactRepository extends AbstractArtifactRepository {
	private static final String NAME = "Non-IU Artifact Repository @ "; //$NON-NLS-1$
	private static final String DESCRIPTION = """
			In-Memory repository created for a single Non-IU repository, used
			during the planner resolution of a real IU repository.
			"""; //$NON-NLS-1$
	private static final String MEMORY_PREFIX = "memory://"; //$NON-NLS-1$
	// BundleInfo or IFeatureModel
	private final Map<IArtifactDescriptor, Object> artifacts = new HashMap<>();

	public VirtualArtifactRepository(IProvisioningAgent agent, ITargetLocation targetLocation) {
		super(agent, NAME + getLocationSafe(targetLocation), targetLocation.getType(), null,
				URI.create(MEMORY_PREFIX + UUID.randomUUID()), DESCRIPTION + '\n' + targetLocation.serialize(), null,
				null);
		Assert.isTrue(targetLocation.isResolved());
		for (TargetBundle targetBundle : targetLocation.getBundles()) {
			if (!targetBundle.getStatus().isOK()) {
				PDECore.log(Status.warning(NLS.bind(Messages.VirtualArtifactRepository_0, targetBundle)));
				continue;
			}
			BundleInfo bundleInfo = targetBundle.getBundleInfo();
			IArtifactKey artifactKey = BundlesAction.createBundleArtifactKey(bundleInfo.getSymbolicName(),
					bundleInfo.getVersion());
			IArtifactDescriptor artifactDesriptor = new ArtifactDescriptor(artifactKey);
			artifacts.put(artifactDesriptor, bundleInfo);
		}
		for (TargetFeature targetFeature : targetLocation.getFeatures()) {
			IArtifactKey artifactKey = FeaturesAction.createFeatureArtifactKey(targetFeature.getId(),
					targetFeature.getVersion());
			IArtifactDescriptor artifactDesriptor = new ArtifactDescriptor(artifactKey);
			artifacts.put(artifactDesriptor, targetFeature.getFeatureModel());
		}
	}

	private static String getLocationSafe(ITargetLocation targetLocation) {
		try {
			return targetLocation.getLocation(false);
		} catch (CoreException e) {
			return "<unknown>"; //$NON-NLS-1$
		}
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		Object artifactModel = artifacts.get(descriptor);
		if (artifactModel == null) {
			return Status.error(NLS.bind(Messages.VirtualArtifactRepository_1, descriptor));
		}
		try {
			transferArtifact(artifactModel, destination);
			return Status.OK_STATUS;
		} catch (Exception e) {
			return Status.error(e.getLocalizedMessage(), e);
		}
	}

	private void transferArtifact(Object artifactModel, OutputStream destination) throws Exception {
		if (artifactModel instanceof BundleInfo bundleInfo) {
			URI location = bundleInfo.getLocation();
			if (location == null) {
				throw new FileNotFoundException(bundleInfo.getSymbolicName());
			}

			File bundleLocation = new File(location);
			if (bundleLocation.isFile()) {
				try (InputStream is = new FileInputStream(bundleLocation)) {
					is.transferTo(destination);
				}
			} else {
				ZipOutputStream zip = new ZipOutputStream(destination);
				FileUtils.zip(zip, bundleLocation, Set.of(), FileUtils.createRootPathComputer(bundleLocation));
				zip.finish();
				zip.flush();
			}
		} else if (artifactModel instanceof IFeatureModel featureModel) {
			String installLocation = featureModel.getInstallLocation();
			if (installLocation != null) {
				File featureJar = new File(installLocation);
				if (featureJar.isFile()) {
					Files.copy(featureJar.toPath(), destination);
					return;
				}
			}
			IFeature feature = featureModel.getFeature();
			// Generate in-memory feature jar (with only the feature.xml)
			JarOutputStream jos = new JarOutputStream(destination);
			jos.putNextEntry(new JarEntry("feature.xml")); //$NON-NLS-1$
			PrintWriter printWriter = new PrintWriter(jos);
			feature.write("", printWriter); //$NON-NLS-1$
			printWriter.flush();
			jos.finish();
		} else {
			throw new IllegalArgumentException(artifactModel.toString());
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
		return artifacts.containsKey(descriptor);
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
		MultiStatus multiStatus = new MultiStatus(getClass(), IStatus.INFO, "Perform Artifact Requests"); //$NON-NLS-1$
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		for (IArtifactRequest request : requests) {
			request.perform(this, subMonitor.split(1));
			multiStatus.add(request.getResult());
		}
		return multiStatus.isOK() ? Status.OK_STATUS : multiStatus;
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		throw new ProvisionException("Artifact repository must not be modified!"); //$NON-NLS-1$
	}

}
