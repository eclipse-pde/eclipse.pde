/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * This job attempts to install a set of exported plug-ins or
 * features into the current runtime.
 */
public class RuntimeInstallJob extends Job {

	private FeatureExportInfo fInfo;

	/**
	 * Creates a new job that will install exported plug-ins.  For a 
	 * successful install, specific option in the feature export info
	 * object need to be set before the export operation see 
	 * {@link #modifyInfoForInstall(FeatureExportInfo)}
	 * 
	 * @param jobName the name to use for this job
	 * @param info the info object describing what is being exported
	 */
	public RuntimeInstallJob(String jobName, FeatureExportInfo info) {
		super(jobName);
		fInfo = info;
	}

	/**
	 * Sets the export options required to make the export installable.
	 * This method should be called before the export operation takes
	 * place.
	 * 
	 * @param info the feature info object that will be modified
	 */
	public static void modifyInfoForInstall(FeatureExportInfo info) {
		info.exportSource = false;
		info.useJarFormat = true;
		info.exportMetadata = true;
		info.qualifier = QualifierReplacer.getDateQualifier();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		try {
			monitor.beginTask(PDEUIMessages.RuntimeInstallJob_Job_name_installing, 12 + (2 * fInfo.items.length));

			// p2 needs to know about the generated repos
			URI destination = new File(fInfo.destinationDirectory).toURI();
			ProvisioningUtil.loadArtifactRepository(destination, new SubProgressMonitor(monitor, 1));

			IMetadataRepository metaRepo = ProvisioningUtil.loadMetadataRepository(destination, new SubProgressMonitor(monitor, 1));

			IProfile profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
			if (profile == null) {
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), PDEUIMessages.RuntimeInstallJob_ErrorCouldntOpenProfile);
			}

			List toInstall = new ArrayList();
			for (int i = 0; i < fInfo.items.length; i++) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				monitor.subTask(NLS.bind(PDEUIMessages.RuntimeInstallJob_Creating_installable_unit, fInfo.items[i].toString()));

				//Get the installable unit from the repo
				String id = null;
				String version = null;
				if (fInfo.items[i] instanceof IPluginModelBase) {
					id = ((IPluginModelBase) fInfo.items[i]).getPluginBase().getId();
					version = ((IPluginModelBase) fInfo.items[i]).getPluginBase().getVersion();
				} else if (fInfo.items[i] instanceof IFeatureModel) {
					id = ((IFeatureModel) fInfo.items[i]).getFeature().getId() + ".feature.group"; //$NON-NLS-1$
					version = ((IFeatureModel) fInfo.items[i]).getFeature().getVersion();
				}

				if (id == null && version == null) {
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), NLS.bind(PDEUIMessages.RuntimeInstallJob_ErrorCouldNotGetIdOrVersion, fInfo.items[i].toString()));
				}

				// Use the same qualifier replacement as the export operation used
				version = QualifierReplacer.replaceQualifierInVersion(version, id, null, null);

				// Check if the right version exists in the new meta repo
				Version newVersion = new Version(version);
				Collector queryMatches = metaRepo.query(new InstallableUnitQuery(id, newVersion), new Collector(), monitor);
				if (queryMatches.size() == 0) {
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), NLS.bind(PDEUIMessages.RuntimeInstallJob_ErrorCouldNotFindUnitInRepo, new String[] {id, version}));
				}

				IInstallableUnit iuToInstall = (IInstallableUnit) queryMatches.toArray(IInstallableUnit.class)[0];

				// Find out if the profile already has that iu installed												
				queryMatches = profile.query(new InstallableUnitQuery(id), new Collector(), new SubProgressMonitor(monitor, 0));
				if (queryMatches.size() == 0) {
					// Just install the new iu into the profile
					toInstall.add(iuToInstall);
				} else {
					// There is an existing iu that we need to replace using an installable unit patch
					Version existingVersion = ((IInstallableUnit) queryMatches.toArray(IInstallableUnit.class)[0]).getVersion();
					toInstall.add(createInstallableUnitPatch(id, newVersion, existingVersion, profile, monitor));
				}
				monitor.worked(2);

			}

			if (toInstall.size() > 0) {
				MultiStatus accumulatedStatus = new MultiStatus(PDEPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$
				ProfileChangeRequest request = InstallAction.computeProfileChangeRequest((IInstallableUnit[]) toInstall.toArray(new IInstallableUnit[toInstall.size()]), IProfileRegistry.SELF, accumulatedStatus, monitor);
				if (request == null || accumulatedStatus.getSeverity() == IStatus.CANCEL || !(accumulatedStatus.isOK() || accumulatedStatus.getSeverity() == IStatus.INFO)) {
					return accumulatedStatus;
				}

				ProvisioningPlan thePlan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(new URI[] {destination}), new SubProgressMonitor(monitor, 5));
				IStatus status = thePlan.getStatus();
				if (status.getSeverity() == IStatus.CANCEL || !(status.isOK() || status.getSeverity() == IStatus.INFO)) {
					return status;
				}

				status = ProvisioningUtil.performProvisioningPlan(thePlan, new DefaultPhaseSet(), profile, new SubProgressMonitor(monitor, 5));

				return status;
			}

			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;

		} catch (ProvisionException e) {
			return e.getStatus();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates an installable unit patch that will change the version of
	 * existing requirements with the given version.
	 * 
	 * @param id id of the installable unit that is having a version change
	 * @param version the new version to require
	 * @param existingVersion an existing version of the plug-in that this patch will replaced, used to generate lifecycle
	 * @param profile the profile we are installing in
	 * @param monitor progress monitor
	 * @return an installable unit patch
	 */
	private IInstallableUnitPatch createInstallableUnitPatch(final String id, final Version version, final Version existingVersion, IProfile profile, IProgressMonitor monitor) {
		InstallableUnitPatchDescription iuPatchDescription = new MetadataFactory.InstallableUnitPatchDescription();
		iuPatchDescription.setId(id + ".patch"); //$NON-NLS-1$
		iuPatchDescription.setProperty(IInstallableUnit.PROP_NAME, NLS.bind(PDEUIMessages.RuntimeInstallJob_installPatchName, id));
		iuPatchDescription.setProperty(IInstallableUnit.PROP_DESCRIPTION, PDEUIMessages.RuntimeInstallJob_installPatchDescription);
		Version patchVersion = new Version("1.0.0." + QualifierReplacer.getDateQualifier()); //$NON-NLS-1$
		iuPatchDescription.setVersion(patchVersion);
		iuPatchDescription.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(iuPatchDescription.getId(), new VersionRange(new Version(0, 0, 0), true, patchVersion, false), 0, null));

		ArrayList list = new ArrayList(1);
		list.add(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iuPatchDescription.getId(), iuPatchDescription.getVersion()));
		iuPatchDescription.addProvidedCapabilities(list);

		IRequiredCapability applyTo = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, id, null, null, false, false);
		IRequiredCapability newValue = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(version, true, version, true), null, false, false);
		iuPatchDescription.setRequirementChanges(new IRequirementChange[] {MetadataFactory.createRequirementChange(applyTo, newValue)});

		iuPatchDescription.setApplicabilityScope(new IRequiredCapability[0][0]);

		// Add lifecycle requirement on a changed bundle, if it gets updated, then we should uninstall the patch
		Collector queryMatches = profile.query(new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate instanceof IInstallableUnit) {
					IRequiredCapability[] reqs = ((IInstallableUnit) candidate).getRequiredCapabilities();
					for (int i = 0; i < reqs.length; i++) {
						if (reqs[i].getName().equals(id)) {
							if (new VersionRange(existingVersion, true, existingVersion, true).equals(reqs[i].getRange())) {
								return true;
							}
						}
					}
				}
				return false;
			}
		}, new Collector(), monitor);
		if (!queryMatches.isEmpty()) {
			IInstallableUnit lifecycleUnit = (IInstallableUnit) queryMatches.toArray(IInstallableUnit.class)[0];
			iuPatchDescription.setLifeCycle(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, lifecycleUnit.getId(), new VersionRange(lifecycleUnit.getVersion(), true, lifecycleUnit.getVersion(), true), null, false, false, false));
		}

		iuPatchDescription.setProperty(IInstallableUnit.PROP_TYPE_PATCH, Boolean.TRUE.toString());

		return MetadataFactory.createInstallableUnitPatch(iuPatchDescription);
	}
}
