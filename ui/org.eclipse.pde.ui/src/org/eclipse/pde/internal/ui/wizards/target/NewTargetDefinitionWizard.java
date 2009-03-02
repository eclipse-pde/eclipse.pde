/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.impl.WorkspaceFileTargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewTargetDefinitionWizard extends BasicNewResourceWizard {

	TargetDefinitionWizardPage fPage;
	TargetCreationPage ftargetCreationPage;
	IPath fInitialPath = null;
	IPath fFilePath = null;

	public void addPages() {
		ftargetCreationPage = new TargetCreationPage("profile"); //$NON-NLS-1$
		fPage = new TargetDefinitionWizardPage("profile", getSelection()); //$NON-NLS-1$
		if (fInitialPath != null)
			fPage.setContainerFullPath(fInitialPath);
		addPage(fPage);
	}

	public boolean performFinish() {
		try {
			int option = fPage.getInitializationOption();
			ftargetCreationPage.setTargetId(fPage.getTargetId());
			ITargetDefinition targetDef = ftargetCreationPage.createTarget(option);
			fFilePath = fPage.getContainerFullPath().append(fPage.getFileName());
			IFile targetFile = PDECore.getWorkspace().getRoot().getFile(fFilePath);
			WorkspaceFileTargetHandle wrkspcTargetHandle = new WorkspaceFileTargetHandle(targetFile);
			wrkspcTargetHandle.save(targetDef);

			// Open the editor
			IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
			if (ww != null) {
				IWorkbenchPage page = ww.getActivePage();
				IFile file = wrkspcTargetHandle.getTargetFile();
				if (page != null && file.exists())
					try {
						IDE.openEditor(page, file);
					} catch (PartInitException e) {
					}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(PDEUIMessages.NewTargetProfileWizard_title);
		setNeedsProgressMonitor(true);
	}

	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	public void setInitialPath(IPath path) {
		fInitialPath = path;
	}

	public IPath getFilePath() {
		return fFilePath;
	}

}
