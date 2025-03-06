/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.internal.tests.p2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.build.tests.PDETestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class P2TestCase extends PDETestCase {
	static final private String IU_NAMESPACE = "org.eclipse.equinox.p2.iu";

	static private BundleContext context = null;
	static private IMetadataRepositoryManager metadataManager = null;
	static private IArtifactRepositoryManager artifactManager = null;

	static private void initialize() {
		if (context == null) {
			Bundle bundle = Platform.getBundle("org.eclipse.pde.build.tests");
			if (bundle == null) {
				throw new IllegalStateException();
			}
			context = bundle.getBundleContext();
		}
		if (context == null) {
			throw new IllegalStateException();
		}

		ServiceReference<IMetadataRepositoryManager> reference = context
				.getServiceReference(IMetadataRepositoryManager.class);
		if (reference == null) {
			IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(context,
					IProvisioningAgent.SERVICE_NAME);
			if (agent == null) {
				throw new IllegalStateException();
			}

			metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			return;
		}

		Object result = context.getService(reference);
		context.ungetService(reference);
		metadataManager = (IMetadataRepositoryManager) result;

		ServiceReference<IArtifactRepositoryManager> reference2 = context
				.getServiceReference(IArtifactRepositoryManager.class);
		if (reference2 == null) {
			throw new IllegalStateException();
		}

		result = context.getService(reference2);
		context.ungetService(reference2);
		artifactManager = (IArtifactRepositoryManager) result;
	}

	public void removeMetadataRepository(URI location) throws Exception {
		if (metadataManager == null) {
			initialize();
		}
		metadataManager.removeRepository(location);
	}

	public void removeArtifactRepository(URI location) throws Exception {
		if (artifactManager == null) {
			initialize();
		}
		artifactManager.removeRepository(location);
	}

	public IMetadataRepository loadMetadataRepository(String metadataLocation) throws Exception {
		if (metadataLocation == null) {
			return null;
		}

		URI location = URIUtil.fromString(metadataLocation);
		return loadMetadataRepository(location);
	}

	public IMetadataRepository loadMetadataRepository(URI location) throws Exception {
		if (location == null) {
			return null;
		}
		if (metadataManager == null) {
			initialize();
		}
		IMetadataRepository repository = metadataManager.loadRepository(location, null);
		assertNotNull(repository);
		return repository;
	}

	public IArtifactRepository loadArtifactRepository(String artifactLocation) throws Exception {
		if (artifactLocation == null) {
			return null;
		}
		if (artifactManager == null) {
			initialize();
		}

		URI location = URIUtil.fromString(artifactLocation);
		IArtifactRepository repository = artifactManager.loadRepository(location, null);
		assertNotNull(repository);
		return repository;
	}

	public URI createCompositeFromBase(IFolder repository) throws Exception {
		if (metadataManager == null) {
			initialize();
		}

		URI baseURI = repository.getLocationURI();

		File base = new File(Platform.getInstallLocation().getURL().getPath());
		assertTrue("Install location " + base.getAbsolutePath() + " does not exists", base.exists());
		base = new File(base, "p2/org.eclipse.equinox.p2.engine/profileRegistry/SDKProfile.profile");
		assertTrue("SDKProfile " + base.getAbsolutePath() + " does not exists", base.exists());
		File[] profiles = base.listFiles();
		assertNotNull("can't read folder " + base.getAbsolutePath(), profiles);
		Arrays.sort(profiles);
		assertTrue("No profiles found in " + base.getAbsolutePath(), profiles.length > 0);
		File profile = profiles[profiles.length - 1];

		CompositeMetadataRepository repo = (CompositeMetadataRepository) metadataManager.createRepository(baseURI,
				"base composite", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		repo.addChild(profile.toURI());

		CompositeArtifactRepository artifact = (CompositeArtifactRepository) artifactManager.createRepository(baseURI,
				"base composite", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		artifact.addChild(URIUtil.toURI(Platform.getInstallLocation().getURL()));

		return baseURI;
	}

	public String getArtifactLocation(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		String name = key.getId() + '_' + key.getVersion();
		if (key.getClassifier().equals("osgi.bundle")) {
			name = "plugins/" + name + ".jar";
		} else if (key.getClassifier().equals("org.eclipse.update.feature")) {
			name = "features/" + name + ".jar";
		} else if (key.getClassifier().equals("binary")) {
			name = "binary/" + name;
		}
		return name;
	}

	public IInstallableUnit getIU(IMetadataRepository repository, String name) {
		return getIU(repository, name, true);
	}

	public IInstallableUnit getIU(IMetadataRepository repository, String name, boolean assertNotNull) {
		IQueryResult<IInstallableUnit> queryResult = repository.query(QueryUtil.createIUQuery(name), null);

		IInstallableUnit unit = null;
		if (!queryResult.isEmpty()) {
			unit = queryResult.iterator().next();
		}
		if (assertNotNull) {
			assertEquals(1, queryResult.toUnmodifiableSet().size());
			assertNotNull(unit);
		}
		return unit;
	}

	public void assertManagerDoesntContain(URI repo) {
		if (metadataManager == null) {
			initialize();
		}
		assertFalse(metadataManager.contains(repo));

		if (artifactManager == null) {
			initialize();
		}
		assertFalse(artifactManager.contains(repo));
	}

	public void assertTouchpoint(IInstallableUnit iu, String phase, String action) {
		Collection<ITouchpointData> data = iu.getTouchpointData();
		for (ITouchpointData iTouchpointData : data) {
			ITouchpointInstruction instruction = iTouchpointData.getInstruction(phase);
			if (instruction != null && instruction.getBody().indexOf(action) > -1) {
				return;
			}
		}
		fail("Action not found:" + action);
	}

	public void assertProvides(IInstallableUnit iu, String namespace, String name) {
		Collection<IProvidedCapability> caps = iu.getProvidedCapabilities();
		for (IProvidedCapability cap : caps) {
			if (cap.getNamespace().equals(namespace) && cap.getName().equals(name)) {
				return;
			}

		}
		assertTrue(false);
	}

	public void assertRequires(IInstallableUnit iu, String namespace, String name) {
		Collection<IRequirement> reqs = iu.getRequirements();
		for (IRequirement iRequirement : reqs) {
			IRequiredCapability reqCap = (IRequiredCapability) iRequirement;
			if (reqCap.getNamespace().equals(namespace) && reqCap.getName().equals(name)) {
				return;
			}

		}
		assertTrue(false);
	}

	public ArrayList<IInstallableUnit> assertRequires(IInstallableUnit iu, ArrayList<IInstallableUnit> requiredIUs,
			boolean requireAll) {
		outer: for (Iterator<IInstallableUnit> iterator = requiredIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit reqIU = iterator.next();

			Collection<IRequirement> reqs = iu.getRequirements();
			for (IRequirement iRequirement : reqs) {
				IRequiredCapability reqCap = (IRequiredCapability) iRequirement;
				if (reqCap.getNamespace().equals(IU_NAMESPACE) && reqCap.getName().equals(reqIU.getId())
						&& reqCap.getRange().isIncluded(reqIU.getVersion())) {
					iterator.remove();
					continue outer;
				}
			}
		}

		if (requireAll) {
			assertTrue(requiredIUs.size() == 0);
		}
		return requiredIUs;
	}
}
