/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.update;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IProvisionerWizard;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.IOperation;
import org.eclipse.update.operations.IOperationListener;
import org.eclipse.update.operations.OperationsManager;

public class UpdateSiteProvisionerWizard extends Wizard implements
IProvisionerWizard {

	class UpdateSiteDownloader implements IRunnableWithProgress, IOperationListener {

		private IUpdateSiteProvisionerEntry[] entries;

		public UpdateSiteDownloader(IUpdateSiteProvisionerEntry[] entries) {
			this.entries = entries;
		}

		public void run(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException {

			try {
				monitor.beginTask(PDEUIMessages.UpdateSiteDownloader_message, entries.length);
				for(int i = 0; i < entries.length; i++) {
					IUpdateSiteProvisionerEntry entry = entries[i];
					File sitePath = new File(entry.getInstallLocation());
					URL remoteSiteURL = new URL(entry.getSiteLocation());
					ISite site = SiteManager.getSite(sitePath.toURL(), null);
					IConfiguredSite csite = site.getCurrentConfiguredSite();

					if(csite == null) {
						ILocalSite localSite = SiteManager.getLocalSite();
						IInstallConfiguration config = localSite.getCurrentConfiguration();
						csite = config.createConfiguredSite(sitePath);
						csite.verifyUpdatableStatus();
//						if (!status.isOK())
//						System.out.println("status is not good");

					}

					ISite remoteSite = SiteManager.getSite(remoteSiteURL, null);

					IFeatureReference[] references = 
						remoteSite.getFeatureReferences();
					for(int j = 0; j < references.length; j++) {
						IFeatureReference reference = references[j];
						IFeature feature = reference.getFeature(null);
						IInstallFeatureOperation operation = 
							OperationsManager.getOperationFactory().createInstallOperation(csite, feature, null, null, null);
						operation.execute(monitor, this);
					}
					monitor.worked(1);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			} finally {
				monitor.done();
			}
		}

		public boolean afterExecute(IOperation operation, Object data) {
			return true;
		}

		public boolean beforeExecute(IOperation operation, Object data) {
			return true;
		}

	}

	private File[] fDirs = null;
	private UpdateSiteProvisionerPage fPage;

	public UpdateSiteProvisionerWizard() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.UpdateSiteProvisionerWizard_title); 
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWSITEPRJ_WIZ);
	}

	public void addPages() {
		fPage = new UpdateSiteProvisionerPage("update site"); //$NON-NLS-1$
		addPage(fPage);
	}

	public boolean performFinish() {
		List dirs = new ArrayList();
		try {
			IUpdateSiteProvisionerEntry[] entries = fPage.getEntries();
			getContainer().run(false, false, new UpdateSiteDownloader(entries));

			for(int i = 0; i < entries.length; i++) {
				IUpdateSiteProvisionerEntry entry = entries[i];
				File file = new File(entry.getInstallLocation(), "eclipse"); //$NON-NLS-1$
				if(file.exists())
					dirs.add(file);
			}
			fDirs = (File[]) dirs.toArray(new File[dirs.size()]);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	public File[] getLocations() {
		return fDirs;
	}

}
