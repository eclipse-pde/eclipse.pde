/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.samples;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ShowSampleAction extends Action implements IIntroAction {
	private static final String SAMPLE_FEATURE_ID = "org.eclipse.sdk.samples"; //$NON-NLS-1$
	private static final String UPDATE_SITE = "http://www.eclipse.org/pde/samples/site.xml"; //$NON-NLS-1$
	private String sampleId;

	private ProvisioningUI provUI;

	/**
	 *
	 */
	public ShowSampleAction() {
		provUI = ProvisioningUI.getDefaultUI();
	}

	@Override
	public void run(IIntroSite site, Properties params) {
		sampleId = params.getProperty("id"); //$NON-NLS-1$
		if (sampleId == null)
			return;

		Runnable r = () -> {
			if (!ensureSampleFeaturePresent())
				return;

			SampleWizard wizard = new SampleWizard();
			try {
				wizard.setInitializationData(null, "class", sampleId); //$NON-NLS-1$
				wizard.setSampleEditorNeeded(false);
				wizard.setSwitchPerspective(false);
				wizard.setSelectRevealEnabled(false);
				wizard.setActivitiesEnabled(false);
				WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				if (dialog.open() == Window.OK) {
					switchToSampleStandby(wizard);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		};

		Shell currentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		currentShell.getDisplay().asyncExec(r);
	}

	private void switchToSampleStandby(SampleWizard wizard) {
		StringBuilder url = new StringBuilder();
		url.append("http://org.eclipse.ui.intro/showStandby?"); //$NON-NLS-1$
		url.append("pluginId=org.eclipse.pde.ui"); //$NON-NLS-1$
		url.append("&"); //$NON-NLS-1$
		url.append("partId=org.eclipse.pde.ui.sampleStandbyPart"); //$NON-NLS-1$
		url.append("&"); //$NON-NLS-1$
		url.append("input="); //$NON-NLS-1$
		url.append(sampleId);
		IIntroURL introURL = IntroURLFactory.createIntroURL(url.toString());
		if (introURL != null) {
			introURL.execute();
			ensureProperContext(wizard);
		}
	}

	private void ensureProperContext(SampleWizard wizard) {
		IConfigurationElement sample = wizard.getSelection();
		String perspId = sample.getAttribute("perspectiveId"); //$NON-NLS-1$
		if (perspId != null) {
			try {
				wizard.enableActivities();
				PlatformUI.getWorkbench().showPerspective(perspId, PDEPlugin.getActiveWorkbenchWindow());
				wizard.selectReveal(PDEPlugin.getActiveWorkbenchShell());
			} catch (WorkbenchException e) {
				PDEPlugin.logException(e);
			}
		}
		enableActivities(sample);
	}

	private void enableActivities(IConfigurationElement sample) {
	}

	/**
	 * Ensure the sample feature is present. If not present, attempt to install it.
	 *
	 * @return <code>true</code> if the sample features are present, and
	 * <code>false</code> otherwise.
	 */
	private boolean ensureSampleFeaturePresent() {
		IProfile profile = getProfile();
		if (profile == null)
			return false;
		if (checkFeature(profile))
			return true;
		// the feature is not present - ask to download
		if (MessageDialog.openQuestion(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.ShowSampleAction_msgTitle, PDEUIMessages.ShowSampleAction_msgDesc)) {
			return downloadFeature();
		}
		return false;
	}

	private boolean checkFeature(IProfile profile) {
		return !profile.query(getSampleFeatureQuery(), null).isEmpty();
	}

	/**
	 * Returns <code>true</code> if the sample feature is already installed, or if
	 * it won't be possible to install the feature (required services are missing). Returns
	 * <code>false</code> if the features are missing and should be installed.
	 */
	private IProfile getProfile() {
		IProvisioningAgent agent = provUI.getSession().getProvisioningAgent();
		if (agent == null)
			return null;
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null)
			return null;
		return registry.getProfile(provUI.getProfileId());
	}

	IQuery<IInstallableUnit> getSampleFeatureQuery() {
		return QueryUtil.createIUQuery(SAMPLE_FEATURE_ID);
	}

	/**
	 * Download the sample feature, returning <code>true</code> if the feature
	 * was installed successfully, and <code>false</code> otherwise.
	 */
	private boolean downloadFeature() {
		IRunnableWithProgress op = monitor -> {
			try {
				SubMonitor sub = SubMonitor.convert(monitor, PDEUIMessages.ShowSampleAction_installing, 100);
				InstallOperation operation = createInstallOperation(sub.split(10));
				operation.resolveModal(sub.split(20));
				IStatus status = operation.getResolutionResult();
				if (status.getSeverity() == IStatus.CANCEL) {
					throw new InterruptedException();
				} else if (!(status.isOK() || status.getSeverity() == IStatus.INFO)) {
					throw new CoreException(status);
				}
				ProvisioningJob job = operation.getProvisioningJob(null);
				status = job.runModal(sub.split(70));
				if (!(status.isOK() || status.getSeverity() == IStatus.INFO)) {
					throw new CoreException(status);
				}
				applyConfiguration();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	/**
	 * Returns a Collection<IInstallableUnit> of the installable units that contain the samples
	 * to be installed.
	 */
	protected Collection<IInstallableUnit> findSampleIUs(URI location, SubMonitor monitor) throws ProvisionException {
		IMetadataRepository repository = provUI.loadMetadataRepository(location, false, monitor.split(5));
		IQueryResult<IInstallableUnit> allSamples = repository.query(getSampleFeatureQuery(), monitor.split(5));
		if (allSamples.isEmpty()) {
			throw new ProvisionException(NLS.bind(PDEUIMessages.ShowSampleAction_NoSamplesFound, location.toString()));
		}
		IInstallableUnit toInstall = null;
		for (Iterator<IInstallableUnit> iterator = allSamples.iterator(); iterator.hasNext();) {
			IInstallableUnit current = iterator.next();
			if (toInstall == null || toInstall.getVersion().compareTo(current.getVersion()) < 0) {
				toInstall = current;
			}
		}
		Collection<IInstallableUnit> result = new ArrayList<>(1);
		result.add(toInstall);
		return result;
	}

	/**
	 * Creates the operation that will install the sample features in the running platform.
	 */
	InstallOperation createInstallOperation(SubMonitor monitor) throws URISyntaxException, ProvisionException {
		URI repositoryLocation = new URI(UPDATE_SITE);
		Collection<IInstallableUnit> sampleIUs = findSampleIUs(repositoryLocation, monitor);
		URI[] repos = new URI[] {repositoryLocation};
		InstallOperation operation = provUI.getInstallOperation(sampleIUs, repos);
		return operation;
	}

	/**
	 * Apply the profile changes to the currently running configuration.
	 */
	void applyConfiguration() throws CoreException {
		BundleContext context = PDEPlugin.getDefault().getBundle().getBundleContext();
		ServiceReference<Configurator> reference = context.getServiceReference(Configurator.class);
		Configurator configurator = context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, "Unexpected failure applying configuration", e)); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
	}

}
