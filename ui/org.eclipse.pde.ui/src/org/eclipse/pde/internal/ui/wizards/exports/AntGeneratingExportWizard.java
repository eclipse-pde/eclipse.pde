/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.build.BaseBuildAction;
import org.w3c.dom.Document;

public abstract class AntGeneratingExportWizard extends BaseExportWizard {
	
	protected BaseExportWizardPage fPage;

	public void addPages() {
		fPage = createPage1();
		addPage(fPage);
	}
	
	protected abstract BaseExportWizardPage createPage1();

	protected boolean performPreliminaryChecks() {
		if (fPage.doGenerateAntFile())
			generateAntBuildFile(fPage.getAntBuildFileName());
		return true;
	}
	
	protected boolean confirmDelete() {
		if (!fPage.doExportToDirectory()) {
			File zipFile = new File(fPage.getDestination(), fPage.getFileName());
			if (zipFile.exists()) {
				if (!MessageDialog.openQuestion(getContainer().getShell(),
						PDEUIMessages.BaseExportWizard_confirmReplace_title,  
						NLS.bind(PDEUIMessages.BaseExportWizard_confirmReplace_desc, zipFile.getAbsolutePath())))
					return false;
				zipFile.delete();
			}
		}
		return true;
	}

	protected abstract Document generateAntTask();
	
	protected void generateAntBuildFile(String filename) {
		String parent = new Path(filename).removeLastSegments(1).toOSString();
		String buildFilename = new Path(filename).lastSegment();
		if (!buildFilename.endsWith(".xml")) //$NON-NLS-1$
			buildFilename += ".xml"; //$NON-NLS-1$
		File dir = new File(new File(parent).getAbsolutePath());
		if (!dir.exists())
			dir.mkdirs();

		try {
			Document task = generateAntTask();
			if (task != null) {
				File buildFile = new File(dir, buildFilename);
				XMLPrintHandler.writeFile(task, buildFile);
				generateAntTask();
				setDefaultValues(dir, buildFilename);
			}
		} catch (IOException e) {
		}
	}
	
	private void setDefaultValues(File dir, String buildFilename) {
		try {
			IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(dir.toString()));
			if (container != null && container.exists()) {
				IProject project = container.getProject();
				if (project != null) {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
					IFile file = container.getFile(new Path(buildFilename));
					if (file.exists())
						BaseBuildAction.setDefaultValues(file);
				}
			}
		} catch (CoreException e) {
		}
	}
	
	protected String getExportOperation() {
		return fPage.doExportToDirectory() ? "directory" : "zip"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
