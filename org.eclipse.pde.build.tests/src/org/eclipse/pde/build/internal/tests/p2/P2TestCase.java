/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.internal.tests.p2;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryUtilities;
import org.eclipse.pde.build.tests.PDETestCase;
import org.osgi.framework.*;

public class P2TestCase extends PDETestCase {
	static final private String IU_NAMESPACE = "org.eclipse.equinox.p2.iu";

	static private BundleContext context = null;
	static private IMetadataRepositoryManager metadataManager = null;
	static private IArtifactRepositoryManager artifactManager = null;

	static private void initialize() {
		if (context == null) {
			Bundle bundle = Platform.getBundle("org.eclipse.pde.build.tests");
			if (bundle == null)
				throw new IllegalStateException();
			context = bundle.getBundleContext();
		}
		if (context == null)
			throw new IllegalStateException();

		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException();

		Object result = context.getService(reference);
		context.ungetService(reference);
		metadataManager = (IMetadataRepositoryManager) result;

		reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException();

		result = context.getService(reference);
		context.ungetService(reference);
		artifactManager = (IArtifactRepositoryManager) result;
	}

	public IMetadataRepository loadMetadataRepository(String metadataLocation) throws Exception {
		if (metadataLocation == null)
			return null;

		if (metadataManager == null)
			initialize();

		URI location = URIUtil.fromString(metadataLocation);
		IMetadataRepository repository = metadataManager.loadRepository(location, null);
		assertNotNull(repository);
		return repository;
	}

	public IArtifactRepository loadArtifactRepository(String artifactLocation) throws Exception {
		if (artifactLocation == null)
			return null;
		if (artifactManager == null)
			initialize();

		URI location = URIUtil.fromString(artifactLocation);
		IArtifactRepository repository = artifactManager.loadRepository(location, null);
		assertNotNull(repository);
		return repository;
	}

	public void assertMD5(IFolder repository, IArtifactDescriptor descriptor) throws Exception {
		String md5 = descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);
		if (md5 == null)
			return;

		IFile artifact = repository.getFile(getArtifactLocation(descriptor));
		String actualMD5 = RepositoryUtilities.computeMD5(artifact.getLocation().toFile());
		assertEquals(md5, actualMD5);
	}

	public String getArtifactLocation(IArtifactDescriptor descriptor) {
		IArtifactKey key = descriptor.getArtifactKey();
		String name = key.getId() + '_' + key.getVersion();
		if (key.getClassifier().equals("osgi.bundle"))
			name = "plugins/" + name + ".jar";
		else if (key.getClassifier().equals("org.eclipse.update.feature"))
			name = "features/" + name + ".jar";
		else if (key.getClassifier().equals("binary"))
			name = "binary/" + name;
		return name;
	}

	public IInstallableUnit getIU(IMetadataRepository repository, String name) {
		Collector collector = repository.query(new InstallableUnitQuery(name), new Collector(), null);
		assertEquals(collector.size(), 1);
		IInstallableUnit unit = (IInstallableUnit) collector.iterator().next();
		assertNotNull(unit);
		return unit;
	}

	public void assertTouchpoint(IInstallableUnit iu, String phase, String action) {
		ITouchpointData[] data = iu.getTouchpointData();
		for (int i = 0; i < data.length; i++) {
			if (data[i].getInstruction(phase).getBody().indexOf(action) > -1)
				return;
		}
		fail("Action not found:" + action);
	}

	public void assertProvides(IInstallableUnit iu, String namespace, String name) {
		IProvidedCapability[] caps = iu.getProvidedCapabilities();
		for (int i = 0; i < caps.length; i++) {
			if (caps[i].getNamespace().equals(namespace) && caps[i].getName().equals(name))
				return;

		}
		assertTrue(false);
	}

	public void assertRequires(IInstallableUnit iu, String namespace, String name) {
		IRequiredCapability[] reqs = iu.getRequiredCapabilities();
		for (int i = 0; i < reqs.length; i++) {
			if (reqs[i].getNamespace().equals(namespace) && reqs[i].getName().equals(name))
				return;

		}
		assertTrue(false);
	}

	public ArrayList assertRequires(IInstallableUnit iu, ArrayList requiredIUs, boolean requireAll) {
		outer: for (Iterator iterator = requiredIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit reqIU = (IInstallableUnit) iterator.next();

			IRequiredCapability[] reqs = iu.getRequiredCapabilities();
			for (int i = 0; i < reqs.length; i++) {
				if (reqs[i].getNamespace().equals(IU_NAMESPACE) && reqs[i].getName().equals(reqIU.getId()) && reqs[i].getRange().isIncluded(reqIU.getVersion())) {
					iterator.remove();
					continue outer;
				}
			}
		}

		if (requireAll)
			assertTrue(requiredIUs.size() == 0);
		return requiredIUs;
	}
}
