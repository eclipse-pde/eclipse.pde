/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.internal.ui.PDEUIMessages;

import java.util.Collection;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * New target definition file wizard used to create a new target definition file from
 * the new target platform preference page.
 */
public class MoveTargetDefinitionWizard extends BasicNewResourceWizard {

	MoveTargetDefinitionPage fPage;
	IPath fPath;
	Collection fFilter;

	public MoveTargetDefinitionWizard(Collection movedTargetDefinitions) {
		super();
		fFilter = movedTargetDefinitions;
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
		setWindowTitle(PDEUIMessages.MoveTargetDefinitionWizard_0);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		fPage = new MoveTargetDefinitionPage("New Target Definition", StructuredSelection.EMPTY); //$NON-NLS-1$
		addPage(fPage);
		fPage.setFilter(fFilter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		fPath = fPage.getContainerFullPath().append(fPage.getFileName());
		return true;
	}

	/**
	 * @return Path of the new file
	 */
	public IPath getTargetFileLocation() {
		return fPath;
	}
}
