/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.p2;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.DownloadPhaseSet;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.IProvisionerWizard;

/**
 * Wizard to provision a target using a p2 metadata repository such as an update site.
 * The user will select a download location and a list of installable units from known
 * sites.  When the user is finished, the IUs will be downloaded to a specific directory
 * and the directory will be added to the 'additional locations' of the target platform.
 * 
 * @since 3.4
 * @see IProvisionerWizard
 * @see P2TargetProvisionerWizardPage
 */
public class P2TargetProvisionerWizard extends Wizard implements IProvisionerWizard {

	private P2TargetProvisionerWizardPage fSelectIUPage;
	private File[] fLocations;

	/**
	 * Name of the temporary profile that will be created to execute the download.
	 */
	private static final String PROFILE_ID = "TEMP_TARGET_PROVISIONER_PROFILE"; //$NON-NLS-1$

	/**
	 * Identifier for dialog settings section 
	 */
	private static final String DIALOG_SETTINGS_SECTION = "P2TargetProvisionerWizardSettings"; //$NON-NLS-1$

	public P2TargetProvisionerWizard() {
		setWindowTitle(ProvisionerMessages.P2TargetProvisionerWizard_1);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
		setNeedsProgressMonitor(true);
		IDialogSettings workbenchSettings = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null)
			section = workbenchSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		setDialogSettings(section);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fSelectIUPage = new P2TargetProvisionerWizardPage("Select IU Page"); //$NON-NLS-1$
		fSelectIUPage.setTitle(ProvisionerMessages.P2TargetProvisionerWizard_1);
		fSelectIUPage.setDescription(ProvisionerMessages.P2TargetProvisionerWizard_2);
		addPage(fSelectIUPage);
		super.addPages();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		return fSelectIUPage.isPageComplete();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		fSelectIUPage.saveWidgetState();
		DownloadIUOperation op = new DownloadIUOperation(fSelectIUPage.getDownloadLocation(true), fSelectIUPage.isClearContentsBeforeDownloading(), fSelectIUPage.getUnits());
		try {
			fLocations = new File[0];
			getContainer().run(true, true, op);
			return true;
		} catch (InvocationTargetException e) {
			if (e.getTargetException() != null) {
				ErrorDialog.openError(getShell(), ProvisionerMessages.P2TargetProvisionerWizard_3, e.getTargetException().getMessage(), e.getTargetException() instanceof CoreException ? ((CoreException) e.getTargetException()).getStatus() : new Status(IStatus.ERROR, PDEPlugin.getPluginId(), e.getTargetException().getMessage()));
			} else {
				ErrorDialog.openError(getShell(), ProvisionerMessages.P2TargetProvisionerWizard_3, e.getMessage(), new Status(IStatus.ERROR, PDEPlugin.getPluginId(), e.getMessage()));
			}
			return false;
		} catch (InterruptedException e) {
			ErrorDialog.openError(getShell(), ProvisionerMessages.P2TargetProvisionerWizard_7, e.getMessage(), new Status(IStatus.ERROR, PDEPlugin.getPluginId(), e.getMessage()));
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IProvisionerWizard#getLocations()
	 */
	public File[] getLocations() {
		return fLocations;
	}

	/**
	 * Job to perform the download of the installable units, reports progress.
	 * @since 3.4
	 */
	private class DownloadIUOperation implements IRunnableWithProgress {

		private File fInstallDir;
		private boolean fClearContents;
		private IInstallableUnit[] fUnits;

		public DownloadIUOperation(File installDir, boolean clearContentsBeforeDownload, IInstallableUnit[] units) {
			fInstallDir = installDir;
			fClearContents = clearContentsBeforeDownload;
			fUnits = units;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) throws InvocationTargetException {
			monitor.beginTask(ProvisionerMessages.P2TargetProvisionerWizard_9, 4);
			if (fInstallDir != null && fInstallDir.isDirectory()) {

				// Clear the contents of the directory if required
				if (fClearContents) {
					File[] contents = fInstallDir.listFiles();
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, contents.length, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
					subMonitor.setTaskName(ProvisionerMessages.P2TargetProvisionerWizard_10);
					for (int i = 0; i < contents.length; i++) {
						deleteDir(contents[i]);
						subMonitor.worked(1);
					}
					subMonitor.done();
				} else {
					monitor.worked(1);
				}

				try {
					// Create the temporary profile
					Map properties = new HashMap();
					properties.put(IProfile.PROP_INSTALL_FOLDER, fInstallDir.toString());
					properties.put(IProfile.PROP_CACHE, fInstallDir.toString());
					ProvisioningUtil.removeProfile(PROFILE_ID, monitor);
					IProfile newProfile = ProvisioningUtil.addProfile(PROFILE_ID, properties, monitor);
					monitor.worked(1);

					// Create the provisioning plan
					ProfileChangeRequest request = new ProfileChangeRequest(newProfile);
					request.addInstallableUnits(fUnits);
					ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
					monitor.worked(1);

					// Execute the provisioning plan
					IStatus result = ProvisioningUtil.performProvisioningPlan(plan, new DownloadPhaseSet(), newProfile, monitor);

					if (result.isOK()) {
						fLocations = new File[] {fInstallDir};
					} else {
						throw new InvocationTargetException(new CoreException(result));
					}

				} catch (ProvisionException e) {
					throw new InvocationTargetException(e);
				} finally {
					// Make sure to remove the temporary profile
					try {
						ProvisioningUtil.removeProfile(PROFILE_ID, monitor);
					} catch (ProvisionException e) {
					}
				}
			}
			monitor.done();
		}

		/**
		 * Deletes a directory
		 * @param dir the directory to delete
		 */
		private void deleteDir(File dir) {
			if (dir.exists()) {
				if (dir.isDirectory()) {
					File[] children = dir.listFiles();
					if (children != null) {
						for (int i = 0; i < children.length; i++) {
							deleteDir(children[i]);
						}
					}
				}
				dir.delete();
			}
		}
	}

}
