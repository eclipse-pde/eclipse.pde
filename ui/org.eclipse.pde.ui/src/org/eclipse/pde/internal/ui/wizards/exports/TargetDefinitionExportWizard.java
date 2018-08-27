/*******************************************************************************
 * Copyright (c) 2010, 2017 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Ian Bull <irbull@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.ExportTargetJob;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard to export a target definition to a directory on the file system
 *
 * @see ExportTargetJob
 */
public class TargetDefinitionExportWizard extends Wizard implements IExportWizard {

	private TargetDefinitionExportWizardPage fPage = null;
	private ITargetDefinition fTarget;

	/**
	 * Section in the dialog settings for this wizard and the wizards created with selection
	 * Shared with the EditBundleContainerWizard
	 */
	static final String SETTINGS_SECTION = "exportTargetDefinitionWizard"; //$NON-NLS-1$

	/**
	 * Default constructor is required for the class to be instantiated through plugin extensions
	 */
	public TargetDefinitionExportWizard() {
		this(null);
	}

	public TargetDefinitionExportWizard(ITargetDefinition target) {
		fTarget = target;
		if (fTarget == null)
			try {
				ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
				if (service != null) {
					fTarget = service.getWorkspaceTargetDefinition();
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.ExportActiveTargetDefinition);
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	@Override
	public void addPages() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
		}
		setDialogSettings(settings);

		fPage = new TargetDefinitionExportWizardPage(fTarget);
		addPage(fPage);
	}

	@Override
	public boolean performFinish() {
		fPage.storeSettings();
		String destDir = fPage.getDestinationDirectory();
		boolean clearDestDir = fPage.isClearDestinationDirectory();
		File file = new File(destDir);

		Job job = new ExportTargetJob(fTarget, file.toURI(), clearDestDir);
		job.schedule(200);

		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

}
