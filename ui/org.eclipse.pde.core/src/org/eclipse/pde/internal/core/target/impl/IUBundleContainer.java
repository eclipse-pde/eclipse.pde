/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.impl;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * A bundle container that references IU's in one or more repositories.
 * 
 * @since 3.5
 */
public class IUBundleContainer extends AbstractBundleContainer {

	/**
	 * Constant describing the type of bundle container 
	 */
	public static final String TYPE = "InstallableUnit"; //$NON-NLS-1$	

	/**
	 * IU identifiers.
	 */
	private String[] fIds;

	/**
	 * IU versions
	 */
	private Version[] fVersions;

	/**
	 * Cached IU's referenced by this bundle container, or <code>null</code> if not
	 * resolved.
	 */
	private IInstallableUnit[] fUnits;

	/**
	 * Repositories to consider, or <code>null</code> if default.
	 */
	private URI[] fRepos;

	/**
	 * Query for bundles in a profile. Every IU that ends up being installed as a bundle
	 * provides a capability in the name space "osgi.bundle".
	 */
	class BundleQuery extends MatchQuery {

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.query.MatchQuery#isMatch(java.lang.Object)
		 */
		public boolean isMatch(Object candidate) {
			if (candidate instanceof IInstallableUnit) {
				IInstallableUnit unit = (IInstallableUnit) candidate;
				IProvidedCapability[] provided = unit.getProvidedCapabilities();
				for (int i = 0; i < provided.length; i++) {
					if (provided[i].getNamespace().equals("osgi.bundle")) { //$NON-NLS-1$
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 * 
	 * @param ids IU identifiers
	 * @param versions IU versions
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> if
	 *   default set
	 */
	IUBundleContainer(String[] ids, String[] versions, URI[] repositories) {
		fIds = ids;
		fVersions = new Version[versions.length];
		for (int i = 0; i < versions.length; i++) {
			fVersions[i] = new Version(versions[i]);

		}
		fRepos = repositories;
	}

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 * 
	 * @param units IU's
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> if
	 *   default set
	 */
	IUBundleContainer(IInstallableUnit[] units, URI[] repositories) {
		fUnits = units;
		fIds = new String[units.length];
		fVersions = new Version[units.length];
		for (int i = 0; i < units.length; i++) {
			fIds[i] = units[i].getId();
			fVersions[i] = units[i].getVersion();
		}
		fRepos = repositories;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		return AbstractTargetHandle.LOCAL_BUNDLE_POOL.toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(Messages.IUBundleContainer_0, 10);

		// retrieve profile
		IProfile profile = ((TargetDefinition) definition).getProfile();
		subMonitor.worked(1);

		// resolve IUs
		IInstallableUnit[] units = getInstallableUnits(profile);

		// create the provisioning plan
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(fUnits);
		IPlanner planner = getPlanner();
		ProvisioningContext context = null;
		if (fRepos == null) {
			context = new ProvisioningContext();
		} else {
			context = new ProvisioningContext(fRepos);
			context.setArtifactRepositories(fRepos);
		}
		ProvisioningPlan plan = planner.getProvisioningPlan(request, context, subMonitor);
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			throw new CoreException(status);
		}
		subMonitor.worked(1);

		// execute the provisioning plan
		PhaseSet phases = DefaultPhaseSet.createDefaultPhaseSet(DefaultPhaseSet.PHASE_CHECK_TRUST & DefaultPhaseSet.PHASE_CONFIGURE & DefaultPhaseSet.PHASE_PROPERTY & DefaultPhaseSet.PHASE_UNCONFIGURE & DefaultPhaseSet.PHASE_UNINSTALL);
		IEngine engine = getEngine();
		IStatus result = engine.perform(profile, phases, plan.getOperands(), context, subMonitor);
		subMonitor.worked(6);

		if (!result.isOK()) {
			System.out.println(result.getMessage());
			throw new CoreException(result);
		}

		// query for bundles
		BundleQuery query = new BundleQuery();
		Collector collector = new Collector();
		profile.query(query, collector, subMonitor);
		subMonitor.worked(1);

		List bundles = new ArrayList();
		IFileArtifactRepository repo = getBundlePool(profile);
		Iterator iterator = collector.iterator();
		while (iterator.hasNext()) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			IArtifactKey[] artifacts = unit.getArtifacts();
			for (int i = 0; i < artifacts.length; i++) {
				IArtifactKey key = artifacts[i];
				File file = repo.getArtifactFile(key);
				if (file == null) {
					// TODO: missing bundle
				} else {
					IResolvedBundle bundle = generateBundle(file);
					if (bundle != null) {
						bundles.add(bundle);
					}
				}
			}
		}

		subMonitor.done();
		return (ResolvedBundle[]) bundles.toArray(new ResolvedBundle[bundles.size()]);
	}

	/**
	 * Returns the IU's this container references. Checks in the profile first to avoid
	 * going out to repositories.
	 * 
	 * @param profile profile to check first
	 * @return IU's
	 * @exception CoreException if unable to retrieve IU's
	 */
	private synchronized IInstallableUnit[] getInstallableUnits(IProfile profile) throws CoreException {
		if (fUnits == null) {
			fUnits = new IInstallableUnit[fIds.length];
			for (int i = 0; i < fIds.length; i++) {
				InstallableUnitQuery query = new InstallableUnitQuery(fIds[i], fVersions[i]);
				Collector collector = profile.query(query, new Collector(), null);
				if (collector.isEmpty()) {
					// try repositories
					if (fRepos != null) {
						for (int j = 0; j < fRepos.length; j++) {
							IMetadataRepository repository = getRepository(fRepos[j]);
							collector = repository.query(query, new Collector(), null);
							if (!collector.isEmpty()) {
								break;
							}
						}
					}
				}
				if (collector.isEmpty()) {
					// not found
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.IUBundleContainer_1, fIds[i])));
				}
				fUnits[i] = (IInstallableUnit) collector.iterator().next();
			}
		}
		return fUnits;
	}

	/**
	 * Returns the metadata repository with the given URI.
	 * 
	 * @param uri location
	 * @return repository
	 * @throws CoreException
	 */
	private IMetadataRepository getRepository(URI uri) throws CoreException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) PDECore.getDefault().acquireService(IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_2));
		}
		IMetadataRepository repo = manager.loadRepository(uri, null);
		return repo;
	}

	/**
	 * Returns the local bundle pool (repository) where bundles are stored for the
	 * given profile.
	 * 
	 * @param profile profile bundles are stored
	 * @return local file artifact repository
	 * @throws CoreException
	 */
	private IFileArtifactRepository getBundlePool(IProfile profile) throws CoreException {
		String path = profile.getProperty(IProfile.PROP_CACHE);
		if (path != null) {
			URI uri = new File(path).toURI();
			IArtifactRepositoryManager manager = getArtifactRepositoryManager();
			try {
				return (IFileArtifactRepository) manager.loadRepository(uri, null);
			} catch (ProvisionException e) {
				//the repository doesn't exist, so fall through and create a new one
			}
		}
		return null;
	}

	/**
	 * Returns the provisioning engine service.
	 * 
	 * @return provisioning engine
	 * @throws CoreException if none
	 */
	private IArtifactRepositoryManager getArtifactRepositoryManager() throws CoreException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) PDECore.getDefault().acquireService(IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_3));
		}
		return manager;
	}

	/**
	 * Returns the provisioning engine service.
	 * 
	 * @return provisioning engine
	 * @throws CoreException if none
	 */
	private IEngine getEngine() throws CoreException {
		IEngine engine = (IEngine) PDECore.getDefault().acquireService(IEngine.SERVICE_NAME);
		if (engine == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_4));
		}
		return engine;
	}

	/**
	 * Returns the provisioning planner.
	 * 
	 * @return provisioning planner
	 * @throws CoreException if none
	 */
	private IPlanner getPlanner() throws CoreException {
		IPlanner planner = (IPlanner) PDECore.getDefault().acquireService(IPlanner.class.getName());
		if (planner == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_5));
		}
		return planner;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof IUBundleContainer) {
			IUBundleContainer iuContainer = (IUBundleContainer) container;
			if (isEqualOrNull(fIds, iuContainer.fIds) && isEqualOrNull(fVersions, iuContainer.fVersions) && isEqualOrNull(fRepos, iuContainer.fRepos)) {
				return super.isContentEqual(container);
			}
		}
		return false;
	}

	/**
	 * Returns the IU's referenced by this container.
	 * 
	 * @return IU's
	 */
	public IInstallableUnit[] getInstallableUnits() {
		return fUnits; // TODO:
	}

	/**
	 * Returns the URI's identifying the metadata repositories to consider when resolving
	 * IU's or <code>null</code> if the default set should be used.
	 * 
	 * @return metadata repository URI's or <code>null</code>
	 */
	public URI[] getRepositories() {
		return fRepos;
	}

	/**
	 * Returns installable unit identifiers.
	 * 
	 * @return IU id's
	 */
	String[] getIds() {
		return fIds;
	}

	/**
	 * Returns installable unit versions.
	 * 
	 * @return IU versions
	 */
	Version[] getVersions() {
		return fVersions;
	}
}
