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

import java.util.Collection;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Defines the Page to get the location where the new target file has to be created
 * 
 * @since 3.5
 */
public class MoveTargetDefinitionPage extends PDEWizardNewFileCreationPage {

	private static String EXTENSION = "target"; //$NON-NLS-1$
	private Collection fFilterList;

	public MoveTargetDefinitionPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.MoveTargetDefinitionPage_0);
		setDescription(PDEUIMessages.MoveTargetDefinitionPage_1);
		// Force the file extension to be 'target'
		setFileExtension(EXTENSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAdvancedControls(Composite parent) {
		//Hide the advanced control buttons
	}

	/**
	 * The list of filenames that are not allowed
	 * 
	 * @param fFilterList <code>Collection</code> of filenames as <code>IPath</code>
	 */
	protected void setFilter(Collection filterFileList) {
		fFilterList = filterFileList;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		IPath path = getContainerFullPath();
		if (fFilterList != null && path != null) {
			path = path.append(getFileName());
			if (fFilterList.contains(path)) {
				setErrorMessage(NLS.bind(PDEUIMessages.NewTargetDefnitionFileWizardPage_0, getFileName()));
				return false;
			}
			setErrorMessage(null);
		}
		return super.validatePage();
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.MOVE_TARGET_WIZARD);
	}
}
