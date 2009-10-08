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
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;

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
	 * Whether this container must have all required IUs of the selected IUs available and included
	 * in the target to resolve successfully.  If this option is true, the planner will be used to resolve
	 * otherwise the slicer is used.  The planner can describe any missing requirements as errors.
	 * <p>
	 * <code>true</code> by default
	 * </p>
	 */
	private boolean fIncludeAllRequired = true;

	/**
	 * Whether this container should download and include environment (platform) specific units for all
	 * available platforms (vs only the current target definition's environment settings).  Only supported 
	 * by the slicer so {@link fIncludeAllRequired} must be turned off for this setting to be used.
	 * <p>
	 * <code>false</code> by default
	 * </p>
	 */
	private boolean fIncludeMultipleEnvironments = false;

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
			fVersions[i] = Version.create(versions[i]);

		}
		if (repositories == null || repositories.length == 0) {
			fRepos = null;
		} else {
			fRepos = repositories;
		}
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
		if (repositories == null || repositories.length == 0) {
			fRepos = null;
		} else {
			fRepos = repositories;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		return AbstractTargetHandle.BUNDLE_POOL.toOSString();
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
		if (fIncludeAllRequired) {
			return resolveWithPlanner(definition, monitor);
		}
		return resolveWithSlicer(definition, monitor);
	}

	/**
	 * Used to resolve the contents of this container if the user is including all required software.  The p2 planner is used
	 * to determine the complete set of IUs required to run the selected software.  If all requirements are met, the bundles
	 * are downloaded from the repository into the bundle pool and added to the target definition.
	 * 
	 * @param definition definition being resolved
	 * @param monitor for reporting progress
	 * @return set of bundles included in this container
	 * @throws CoreException if there is a problem with the requirements or there is a problem downloading
	 */
	private IResolvedBundle[] resolveWithPlanner(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(Messages.IUBundleContainer_0, 200);

		// retrieve profile
		IProfile profile = ((TargetDefinition) definition).getProfile();
		subMonitor.worked(10);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// resolve IUs
		IInstallableUnit[] units = getInstallableUnits(profile);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// create the provisioning plan
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(units);
		for (int i = 0; i < units.length; i++) {
			IInstallableUnit unit = units[i];
			request.setInstallableUnitProfileProperty(unit, AbstractTargetHandle.PROP_INSTALLED_IU, Boolean.toString(true));
		}
		IPlanner planner = getPlanner();
		URI[] repositories = resolveRepositories();
		ProvisioningContext context = new ProvisioningContext(repositories);
		context.setArtifactRepositories(repositories);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		ProvisioningPlan plan = planner.getProvisioningPlan(request, context, new SubProgressMonitor(subMonitor, 10));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		ProvisioningPlan installerPlan = plan.getInstallerPlan();
		if (installerPlan != null) {
			// this plan requires an update to the installer first, log the fact and attempt
			// to continue, we don't want to update the running SDK while provisioning a target
			PDECore.log(new Status(IStatus.INFO, PDECore.PLUGIN_ID, Messages.IUBundleContainer_6));
		}
		subMonitor.worked(10);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// execute the provisioning plan
		PhaseSet phases = DefaultPhaseSet.createDefaultPhaseSet(DefaultPhaseSet.PHASE_CHECK_TRUST | DefaultPhaseSet.PHASE_CONFIGURE | DefaultPhaseSet.PHASE_UNCONFIGURE | DefaultPhaseSet.PHASE_UNINSTALL);
		IEngine engine = getEngine();
		Operand[] operands = plan.getOperands();
		List allOps = new ArrayList(operands.length + 1);
		for (int i = 0; i < operands.length; i++) {
			allOps.add(operands[i]);
		}
		allOps.add(new PropertyOperand(AbstractTargetHandle.PROP_PROVISION_MODE, null, TargetDefinitionPersistenceHelper.MODE_PLANNER));
		allOps.add(new PropertyOperand(AbstractTargetHandle.PROP_ALL_ENVIRONMENTS, null, Boolean.toString(false)));
		IStatus result = engine.perform(profile, phases, (Operand[]) allOps.toArray(new Operand[allOps.size()]), context, new SubProgressMonitor(subMonitor, 140));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// slice IUs and all prerequisites
		PermissiveSlicer slicer = new PermissiveSlicer(profile, new Properties(), true, false, true, false, false);
		IQueryable slice = slicer.slice(units, new SubProgressMonitor(subMonitor, 10));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// query for bundles
		BundleQuery query = new BundleQuery();
		Collector collector = new Collector();
		slice.query(query, collector, new SubProgressMonitor(subMonitor, 10));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		Map bundles = new LinkedHashMap();
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
						bundles.put(bundle.getBundleInfo(), bundle);
					}
				}
			}
		}

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// remove all bundles from previous IU containers (so we don't get duplicates from multi-locations
		IBundleContainer[] containers = definition.getBundleContainers();
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			if (container == this) {
				break;
			}
			if (container instanceof IUBundleContainer) {
				IUBundleContainer bc = (IUBundleContainer) container;
				IResolvedBundle[] included = bc.getBundles();
				if (included != null) {
					for (int j = 0; j < included.length; j++) {
						bundles.remove(included[j].getBundleInfo());
					}
				}
			}
		}
		subMonitor.worked(10);
		subMonitor.done();
		return (ResolvedBundle[]) bundles.values().toArray(new ResolvedBundle[bundles.size()]);
	}

	/**
	 * Used to resolve the contents of this container when the user has chosen to manage the dependencies in the target
	 * themselves.  The selected IUs and any required software that can be found will be retrieved from the repositories 
	 * and added to the target.  Any missing required software will be ignored.
	 * 
	 * @param definition definition being resolved
	 * @param monitor for reporting progress
	 * @return set of resolved bundles included in this container
	 * @throws CoreException if there is a problem interacting with the repositories
	 */
	private IResolvedBundle[] resolveWithSlicer(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(Messages.IUBundleContainer_0, 200);

		// retrieve profile
		IProfile profile = ((TargetDefinition) definition).getProfile();
		subMonitor.worked(10);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// resolve IUs
		IInstallableUnit[] units = getInstallableUnits(profile);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		URI[] repositories = resolveRepositories();
		int repoCount = repositories.length;
		if (repoCount == 0) {
			return new IResolvedBundle[0];
		}

		IProgressMonitor loadMonitor = new SubProgressMonitor(subMonitor, 10);
		loadMonitor.beginTask(null, repoCount * 10);
		IMetadataRepository[] metadataRepos = new IMetadataRepository[repoCount];
		IMetadataRepositoryManager manager = getRepoManager();
		for (int i = 0; i < repoCount; ++i)
			metadataRepos[i] = manager.loadRepository(repositories[i], new SubProgressMonitor(loadMonitor, 10));
		loadMonitor.done();

		IQueryable allMetadata;
		if (repoCount == 1) {
			allMetadata = metadataRepos[0];
		} else {
			allMetadata = new CompoundQueryable(metadataRepos);
		}

		// slice IUs and all prerequisites
		PermissiveSlicer slicer = null;
		if (getIncludeAllEnvironments()) {
			slicer = new PermissiveSlicer(allMetadata, new Properties(), true, false, true, true, false);
		} else {
			Properties props = new Properties();
			props.setProperty("osgi.os", definition.getOS() != null ? definition.getOS() : Platform.getOS()); //$NON-NLS-1$
			props.setProperty("osgi.ws", definition.getWS() != null ? definition.getWS() : Platform.getWS()); //$NON-NLS-1$
			props.setProperty("osgi.arch", definition.getArch() != null ? definition.getArch() : Platform.getOSArch()); //$NON-NLS-1$
			props.setProperty("osgi.nl", definition.getNL() != null ? definition.getNL() : Platform.getNL()); //$NON-NLS-1$
			slicer = new PermissiveSlicer(allMetadata, props, true, false, false, true, false);
		}
		IQueryable slice = slicer.slice(units, new SubProgressMonitor(subMonitor, 10));
		Collector collector = slice.query(new InstallableUnitQuery(null), new Collector(), new SubProgressMonitor(subMonitor, 10));

		if (subMonitor.isCanceled() || collector.isEmpty()) {
			return new IResolvedBundle[0];
		}

		ArrayList operands = new ArrayList(collector.size());
		Iterator itor = collector.iterator();
		while (itor.hasNext()) {
			operands.add(new InstallableUnitOperand(null, (IInstallableUnit) itor.next()));
		}
		for (int i = 0; i < units.length; i++) {
			IInstallableUnit unit = units[i];
			operands.add(new InstallableUnitPropertyOperand(unit, AbstractTargetHandle.PROP_INSTALLED_IU, null, Boolean.toString(true)));
		}
		operands.add(new PropertyOperand(AbstractTargetHandle.PROP_PROVISION_MODE, null, TargetDefinitionPersistenceHelper.MODE_SLICER));
		operands.add(new PropertyOperand(AbstractTargetHandle.PROP_ALL_ENVIRONMENTS, null, Boolean.toString(getIncludeAllEnvironments())));

		// execute the provisioning plan
		PhaseSet phases = DefaultPhaseSet.createDefaultPhaseSet(DefaultPhaseSet.PHASE_CHECK_TRUST | DefaultPhaseSet.PHASE_CONFIGURE | DefaultPhaseSet.PHASE_UNCONFIGURE | DefaultPhaseSet.PHASE_UNINSTALL);
		IEngine engine = getEngine();
		ProvisioningContext context = new ProvisioningContext(repositories);
		context.setArtifactRepositories(repositories);
		IStatus result = engine.perform(profile, phases, (Operand[]) operands.toArray(new Operand[operands.size()]), context, new SubProgressMonitor(subMonitor, 140));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// slice IUs and all prerequisites
		slicer = new PermissiveSlicer(profile, new Properties(), true, false, true, false, false);
		slice = slicer.slice(units, new SubProgressMonitor(subMonitor, 10));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// query for bundles
		collector = slice.query(new BundleQuery(), new Collector(), new SubProgressMonitor(subMonitor, 10));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		Map bundles = new LinkedHashMap();
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
						bundles.put(bundle.getBundleInfo(), bundle);
					}
				}
			}
		}

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// remove all bundles from previous IU containers (so we don't get duplicates from multi-locations
		IBundleContainer[] containers = definition.getBundleContainers();
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			if (container == this) {
				break;
			}
			if (container instanceof IUBundleContainer) {
				IUBundleContainer bc = (IUBundleContainer) container;
				IResolvedBundle[] included = bc.getBundles();
				if (included != null) {
					for (int j = 0; j < included.length; j++) {
						bundles.remove(included[j].getBundleInfo());
					}
				}
			}
		}
		subMonitor.worked(10);
		subMonitor.done();
		return (ResolvedBundle[]) bundles.values().toArray(new ResolvedBundle[bundles.size()]);
	}

	/**
	 * Returns the IU's this container references. Checks in the profile first to avoid
	 * going out to repositories.
	 * 
	 * @param profile profile to check first
	 * @return IU's
	 * @exception CoreException if unable to retrieve IU's
	 */
	public synchronized IInstallableUnit[] getInstallableUnits(IProfile profile) throws CoreException {
		if (fUnits == null) {
			fUnits = new IInstallableUnit[fIds.length];
			for (int i = 0; i < fIds.length; i++) {
				InstallableUnitQuery query = new InstallableUnitQuery(fIds[i], fVersions[i]);
				Collector collector = profile.query(query, new Collector(), null);
				if (collector.isEmpty()) {
					// try repositories
					URI[] repositories = resolveRepositories();
					for (int j = 0; j < repositories.length; j++) {
						IMetadataRepository repository = getRepository(repositories[j]);
						collector = repository.query(query, new Collector(), null);
						if (!collector.isEmpty()) {
							break;
						}
					}
				}
				if (collector.isEmpty()) {
					// not found
					fUnits = null;
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
		IMetadataRepositoryManager manager = getRepoManager();
		IMetadataRepository repo = manager.loadRepository(uri, null);
		return repo;
	}

	/**
	 * Returns the metadata repository manager.
	 * 
	 * @return metadata repository manager
	 * @throws CoreException if none
	 */
	private IMetadataRepositoryManager getRepoManager() throws CoreException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) PDECore.getDefault().acquireService(IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_2));
		}
		return manager;
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
			if (iuContainer.getIncludeAllRequired() == getIncludeAllRequired()) {
				// include all targets only matters if include all required is turned off
				if (getIncludeAllRequired() || iuContainer.getIncludeAllEnvironments() == getIncludeAllEnvironments()) {
					if (isEqualOrNull(fIds, iuContainer.fIds) && isEqualOrNull(fVersions, iuContainer.fVersions) && isEqualOrNull(fRepos, iuContainer.fRepos)) {
						return super.isContentEqual(container);
					}
				}
			}
		}
		return false;
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
	 * Returns the repositories to consider when resolving IU's (will return default set of
	 * repositories if current repository settings are <code>null</code>).
	 *  
	 * @return URI's of repositories to use when resolving bundles
	 * @exception CoreException
	 */
	private URI[] resolveRepositories() throws CoreException {
		if (fRepos == null) {
			IMetadataRepositoryManager manager = getRepoManager();
			return manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		}
		return fRepos;
	}

	/**
	 * Sets whether all required units must be available to resolve this container.  When <code>true</code>
	 * the resolve operation will use the planner to determine the complete set of IUs required to
	 * make the selected IUs runnable.  If any dependencies are missing, the resolve operation will return an
	 * error explaining what problems exist.  When <code>false</code> the resolve operation will use the slicer
	 * to determine what units to include.  Any required units that are not available in the repositories will
	 * be ignored.
	 * <p>
	 * Since there is only one profile per target and the planner and slicer resolve methods are incompatible
	 * it is highly recommended that the parent target be passed to this method so all other IUBundleContainers
	 * in the target can be updated with the new setting. 
	 * </p>
	 * @param include whether all required units must be available to resolve this container
	 * @param definition parent target, used to update other IUBundleContainers with this setting, can be <code>null</code>
	 */
	public void setIncludeAllRequired(boolean include, ITargetDefinition definition) {
		fIncludeAllRequired = include;
		if (definition != null) {
			IBundleContainer[] containers = definition.getBundleContainers();
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i] instanceof IUBundleContainer && containers[i] != this) {
						((IUBundleContainer) containers[i]).setIncludeAllRequired(include, null);
					}
				}
			}
		}
	}

	/**
	 * Returns whether all required units must be available to resolve this container.  When <code>true</code>
	 * the resolve operation will use the planner to determine the complete set of IUs required to
	 * make the selected IUs runnable.  If any dependencies are missing, the resolve operation will return an
	 * error explaining what problems exist.  When <code>false</code> the resolve operation will use the slicer
	 * to determine what units to include.  Any required units that are not available in the repositories will
	 * be ignored.
	 *  
	 * @return whether all required units must be available to resolve this container
	 */
	public boolean getIncludeAllRequired() {
		return fIncludeAllRequired;
	}

	/**
	 * Sets whether all environment (platform) specific installable units should
	 * be included in this container when it is resolved.  This feature is not supported
	 * by the planner so will only have an effect if the include all required setting
	 * is turned off ({@link #getIncludeAllRequired()}).
	 * <p>
	 * There is only one profile per target and this setting can only be set for the
	 * entire target definition.  It is highly recommended that the parent target be passed
	 * to this method so all other IUBundleContainers in the target can be updated with the 
	 * new setting. 
	 * </p>
	 * @param include whether environment specific units should be included
	 * @param definition parent target, used to update other IUBundleContainers with this setting, can be <code>null</code>
	 */
	public void setIncludeAllEnvironments(boolean include, ITargetDefinition definition) {
		fIncludeMultipleEnvironments = include;
		if (definition != null) {
			IBundleContainer[] containers = definition.getBundleContainers();
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i] instanceof IUBundleContainer && containers[i] != this) {
						((IUBundleContainer) containers[i]).setIncludeAllEnvironments(include, null);
					}
				}
			}
		}
	}

	/**
	 * Returns whether all environment (platform) specific installable units should
	 * be included in this container when it is resolved.  This feature is not supported
	 * by the planner so will only have an effect if the include all required setting
	 * is turned off ({@link #getIncludeAllRequired()}).
	 * 
	 * @return whether environment specific units should be included
	 */
	public boolean getIncludeAllEnvironments() {
		return fIncludeMultipleEnvironments;
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
