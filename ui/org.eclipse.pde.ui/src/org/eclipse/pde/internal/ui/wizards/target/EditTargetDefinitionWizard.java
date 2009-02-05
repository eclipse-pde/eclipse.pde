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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Wizard to edit a target definition
 */
public class EditTargetDefinitionWizard extends Wizard {

	/**
	 * The target definition being edited - a copy of the original
	 */
	private ITargetDefinition fDefinition;

	/**
	 * The original target definition that was to be edited. We create
	 * a copy in case the user cancels the operation.
	 */
	private ITargetDefinition fOriginal;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// TODO check if any changes first
		ITargetPlatformService service = TargetDefinitionPage.getTargetService();
		if (service != null) {
			try {
				service.copyTargetDefinition(fDefinition, fOriginal);
			} catch (CoreException e) {
				// TODO set error message
				return false;
			}
		}
		return true;
	}

	/**
	 * Constructs a wizard to edit the given definition.
	 * 
	 * @param definition
	 */
	public EditTargetDefinitionWizard(ITargetDefinition definition) {
		setTargetDefinition(definition);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new TargetDefinitionContentPage(fDefinition));
	}

	/**
	 * Sets the target definition to be edited. Will delegate to pages to
	 * refresh controls if already created.
	 * 
	 * @param definition target definition
	 */
	public void setTargetDefinition(ITargetDefinition definition) {
		fOriginal = definition;
		ITargetPlatformService service = TargetDefinitionPage.getTargetService();
		if (service != null) {
			fDefinition = service.newTarget();
			try {
				service.copyTargetDefinition(definition, fDefinition);
				fDefinition.resolve(null); // TODO: show progress
				IWizardPage[] pages = getPages();
				for (int i = 0; i < pages.length; i++) {
					((TargetDefinitionPage) pages[i]).targetChanged(fDefinition);
				}
			} catch (CoreException e) {
				// TODO: show error message
				PDEPlugin.log(e);
			}
		}
	}

	/**
	 * Returns the target definition being edited
	 * 
	 * @return target definition
	 */
	public ITargetDefinition getTargetDefinition() {
		return fDefinition;
	}
}
