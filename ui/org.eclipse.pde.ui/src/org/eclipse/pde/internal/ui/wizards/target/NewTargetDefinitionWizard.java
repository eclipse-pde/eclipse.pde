/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.core.target.ITargetDefinition;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.WorkspaceFileTargetHandle;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewTargetDefinitionWizard extends BasicNewResourceWizard {

	TargetDefinitionWizardPage fPage;
	TargetCreationPage ftargetCreationPage;
	IPath fInitialPath = null;
	IPath fFilePath = null;

	@Override
	public void addPages() {
		ftargetCreationPage = new TargetCreationPage("profile"); //$NON-NLS-1$
		fPage = new TargetDefinitionWizardPage("profile", getSelection()); //$NON-NLS-1$
		if (fInitialPath != null)
			fPage.setContainerFullPath(fInitialPath);
		addPage(fPage);
	}

	@Override
	public boolean performFinish() {
		try {
			int option = fPage.getInitializationOption();
			ftargetCreationPage.setTargetId(fPage.getTargetId());
			ITargetDefinition targetDef = ftargetCreationPage.createTarget(option);
			fFilePath = fPage.getContainerFullPath().append(fPage.getFileName());
			IFile targetFile = PDECore.getWorkspace().getRoot().getFile(fFilePath);
			if (option == TargetDefinitionWizardPage.USE_EMPTY) {
				//extract the file name
				String name = targetFile.getName();
				int index = name.lastIndexOf(targetFile.getFileExtension()) - 1;
				if (index > 0) {
					name = name.substring(0, index);
					targetDef.setName(name);
				}
			}
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

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(PDEUIMessages.NewTargetProfileWizard_title);
		setNeedsProgressMonitor(true);
	}

	@Override
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
