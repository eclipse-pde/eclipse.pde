/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeWizard;

public class NewClassCreationWizard extends JavaAttributeWizard {

	private boolean fIsInterface;
	
	public NewClassCreationWizard(IProject project, boolean isInterface) {
		super(project, null, null, null);
		fIsInterface = isInterface;
	}

	public void addPages() {
		if (fIsInterface)
			mainPage = new NewInterfaceWizardPage();
		else
			mainPage = new NewClassWizardPage();
		addPage(mainPage);
		if (fIsInterface)
			((NewInterfaceWizardPage)mainPage).init(StructuredSelection.EMPTY);
		else
			((NewClassWizardPage)mainPage).init(StructuredSelection.EMPTY);
	}

}
