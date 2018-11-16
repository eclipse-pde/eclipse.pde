/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
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
	private ProvisioningUI ui;

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
		// This provisioning UI manages the currently running profile.
		ui = ProvisioningUI.getDefaultUI();
		ui.manageJob(this, ProvisioningJob.RESTART_OR_APPLY);
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

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			ProvisioningSession session = ui.getSession();
			SubMonitor subMonitor = SubMonitor.convert(monitor, PDEUIMessages.RuntimeInstallJob_Job_name_installing,
					12 + (2 * fInfo.items.length));

			// p2 needs to know about the generated repos
			URI destination = new File(fInfo.destinationDirectory).toURI();
			ui.loadArtifactRepository(destination, false, subMonitor.split(1));

			IMetadataRepository metaRepo = ui.loadMetadataRepository(destination, false, subMonitor.split(1));

			IProfileRegistry profileRegistry = (IProfileRegistry) session.getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME);
			if (profileRegistry == null) {
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), PDEUIMessages.RuntimeInstallJob_ErrorCouldntOpenProfile);
			}
			IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
			if (profile == null) {
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), PDEUIMessages.RuntimeInstallJob_ErrorCouldntOpenProfile);
			}

			List<IInstallableUnit> toInstall = new ArrayList<>();
			for (Object item : fInfo.items) {
				subMonitor.subTask(
						NLS.bind(PDEUIMessages.RuntimeInstallJob_Creating_installable_unit, item.toString()));

				//Get the installable unit from the repo
				String id = null;
				String version = null;
				if (item instanceof IPluginModelBase) {
					id = ((IPluginModelBase) item).getPluginBase().getId();
					version = ((IPluginModelBase) item).getPluginBase().getVersion();
				} else if (item instanceof IFeatureModel) {
					id = ((IFeatureModel) item).getFeature().getId() + ".feature.group"; //$NON-NLS-1$
					version = ((IFeatureModel) item).getFeature().getVersion();
				}

				if (id == null && version == null) {
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), NLS.bind(PDEUIMessages.RuntimeInstallJob_ErrorCouldNotGetIdOrVersion, item.toString()));
				}

				// Use the same qualifier replacement as the export operation used
				version = QualifierReplacer.replaceQualifierInVersion(version, id, null, null);

				// Check if the right version exists in the new meta repo
				Version newVersion = Version.parseVersion(version);
				IQueryResult<?> queryMatches = metaRepo.query(QueryUtil.createIUQuery(id, newVersion), monitor);
				if (queryMatches.isEmpty()) {
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), NLS.bind(PDEUIMessages.RuntimeInstallJob_ErrorCouldNotFindUnitInRepo, new String[] {id, version}));
				}

				IInstallableUnit iuToInstall = (IInstallableUnit) queryMatches.iterator().next();

				// Find out if the profile already has that iu installed
				queryMatches = profile.query(QueryUtil.createIUQuery(id), subMonitor.split(1));
				if (queryMatches.isEmpty()) {
					// Just install the new iu into the profile
					toInstall.add(iuToInstall);
				} else {
					// There is an existing iu that we need to replace using an installable unit patch
					IInstallableUnit existingIU = (IInstallableUnit) queryMatches.iterator().next();
					toInstall.add(createInstallableUnitPatch(existingIU, newVersion, profile, subMonitor.split(1)));
				}
				subMonitor.split(2);
			}

			if (!toInstall.isEmpty()) {
				InstallOperation operation = ui.getInstallOperation(toInstall, new URI[] {destination});
				operation.resolveModal(subMonitor.split(5));
				IStatus status = operation.getResolutionResult();
				if (status.getSeverity() == IStatus.CANCEL || !(status.isOK() || status.getSeverity() == IStatus.INFO)) {
					return status;
				}
				ProvisioningJob job = operation.getProvisioningJob(null);
				status = job.runModal(subMonitor.split(5));
				return status;
			}

			return Status.OK_STATUS;

		} catch (ProvisionException e) {
			return e.getStatus();
		}
	}

	/**
	 * Creates an installable unit patch that will change the version of
	 * existing requirements with the given version.
	 *
	 * @param existingIU an existing plug-in that this patch will replace, used to generate lifecycle
	 * @param newVersion the new version to require
	 * @param profile the profile we are installing in
	 * @param monitor progress monitor
	 * @return an installable unit patch
	 */
	private IInstallableUnitPatch createInstallableUnitPatch(IInstallableUnit existingIU, Version newVersion, IProfile profile, IProgressMonitor monitor) {
		InstallableUnitPatchDescription iuPatchDescription = new MetadataFactory.InstallableUnitPatchDescription();
		String id = existingIU.getId();
		iuPatchDescription.setId(id + ".patch"); //$NON-NLS-1$
		iuPatchDescription.setProperty(IInstallableUnit.PROP_NAME, NLS.bind(PDEUIMessages.RuntimeInstallJob_installPatchName, id));
		iuPatchDescription.setProperty(IInstallableUnit.PROP_DESCRIPTION, PDEUIMessages.RuntimeInstallJob_installPatchDescription);
		Version patchVersion = Version.createOSGi(1, 0, 0, QualifierReplacer.getDateQualifier());
		iuPatchDescription.setVersion(patchVersion);
		iuPatchDescription.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(iuPatchDescription.getId(), new VersionRange(Version.createOSGi(0, 0, 0), true, patchVersion, false), 0, null));

		ArrayList<IProvidedCapability> list = new ArrayList<>(1);
		list.add(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iuPatchDescription.getId(), iuPatchDescription.getVersion()));
		iuPatchDescription.addProvidedCapabilities(list);

		IRequirement applyTo = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, null, null, false, false);
		IRequirement newValue = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(newVersion, true, newVersion, true), null, false, false);
		iuPatchDescription.setRequirementChanges(new IRequirementChange[] {MetadataFactory.createRequirementChange(applyTo, newValue)});

		iuPatchDescription.setApplicabilityScope(new IRequirement[0][0]);

		// Locate IU's that appoint the existing version of the IU that we are patching.
		// Add lifecycle requirement on a changed bundle, if it gets updated, then we should uninstall the patch
		IQueryResult<?> queryMatches = profile.query(QueryUtil.createMatchQuery("requirements.exists(rc | $0 ~= rc)", new Object[] {existingIU}), monitor); //$NON-NLS-1$
		if (!queryMatches.isEmpty()) {
			IInstallableUnit lifecycleUnit = (IInstallableUnit) queryMatches.iterator().next();
			iuPatchDescription.setLifeCycle(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, lifecycleUnit.getId(), new VersionRange(lifecycleUnit.getVersion(), true, lifecycleUnit.getVersion(), true), null, false, false, false));
		}

		iuPatchDescription.setProperty(InstallableUnitDescription.PROP_TYPE_PATCH, Boolean.TRUE.toString());

		return MetadataFactory.createInstallableUnitPatch(iuPatchDescription);
	}
}
