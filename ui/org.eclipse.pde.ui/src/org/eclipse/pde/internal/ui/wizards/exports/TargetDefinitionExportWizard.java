/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Ian Bull <irbull@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class TargetDefinitionExportWizard extends Wizard implements IExportWizard {

	private TargetDefinitionExportWizardPage fPage = null;

	public TargetDefinitionExportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.ExportActiveTargetDefinition);
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	public void addPages() {
		fPage = new TargetDefinitionExportWizardPage();
		addPage(fPage);
	}

	public boolean performFinish() {
		String destDir = fPage.getDestinationDirectory();
		boolean clearDestDir = fPage.isClearDestinationDirectory();
		File file = new File(destDir);

		Job job = new ExportActiveTargetJob(file.toURI(), clearDestDir);
		job.schedule(200);

		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// do nothing atm
	}

}
