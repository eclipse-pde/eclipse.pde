/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.ui.IWorkbench;

public class NewTocFileWizard extends TocHTMLWizard {

	private TocWizardPage fPage;
	private IPath fInitialPath = null;

	@Override
	public void addPages() {
		fPage = new TocWizardPage("tocfile", getSelection()); //$NON-NLS-1$
		if (fInitialPath != null) {
			fPage.setContainerFullPath(fInitialPath);
		}
		addPage(fPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(TocWizardMessages.NewTocFileWizard_title);
		setNeedsProgressMonitor(true);
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		// setDefaultPageImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_TARGET_WIZ);
	}

	@Override
	public boolean performFinish() {
		try {
			fNewFile = fPage.createNewFile();
			getContainer().run(false, true, getOperation());
		} catch (InvocationTargetException e) {
			PDEUserAssistanceUIPlugin.logException(e);
			fNewFile = null;
			return false;
		} catch (InterruptedException e) {
			fNewFile = null;
			return false;
		}
		return true;
	}

	private TocOperation getOperation() {
		return new TocOperation(fPage.createNewFile(), fPage.getTocName());
	}

	public void setInitialPath(IPath path) {
		fInitialPath = path;
	}

}
