/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Wizard to edit a target definition, creates a working copy to edit.  Any changes are
 * not saved by this wizard and must be done by the client.  The edited target
 * can be accessed using {@link #getTargetDefinition()}
 */
public class EditTargetDefinitionWizard extends Wizard {

	/**
	 * The target definition being edited - a copy of the original
	 */
	private ITargetDefinition fDefinition;

	/**
	 * Constructs a wizard to edit the given definition.
	 *
	 * @param definition target definition being edited
	 * @param createWorkingCopy if true, a copy of the definition will be created to edit, if false the definition will be edited directly
	 */
	public EditTargetDefinitionWizard(ITargetDefinition definition, boolean createWorkingCopy) {
		setTargetDefinition(definition, createWorkingCopy);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		// update the cache to remove all other targets with same handle except
		// this.
		HashMap<ITargetHandle, List<TargetDefinition>> targetFlagMap = TargetPlatformHelper.getTargetDefinitionMap();
		for (Map.Entry<ITargetHandle, List<TargetDefinition>> entry : targetFlagMap.entrySet()) {
			if (entry.getKey().equals(fDefinition.getHandle())) {
				 List<TargetDefinition> targets = targetFlagMap.get(fDefinition.getHandle());
				for (Iterator<TargetDefinition> iterator = targets.iterator(); iterator.hasNext();) {
					TargetDefinition target = iterator.next();
					if (!target.equals(fDefinition)) {
						iterator.remove();
					}
				}
			}
		}
		return true;

	}

	@Override
	public boolean performCancel() {
		// update the cache to remove this target with this handle
		HashMap<ITargetHandle, List<TargetDefinition>> targetFlagMap = TargetPlatformHelper.getTargetDefinitionMap();
		for (Map.Entry<ITargetHandle, List<TargetDefinition>> entry : targetFlagMap.entrySet()) {
			if (entry.getKey().equals(fDefinition.getHandle())) {
				List<TargetDefinition> targets = targetFlagMap.get(fDefinition.getHandle());
				for (Iterator<TargetDefinition> iterator = targets.iterator(); iterator.hasNext();) {
					TargetDefinition target = iterator.next();
					if (target.equals(fDefinition)) {
						iterator.remove();
					}
				}
			}
		}
		return true;
	}

	@Override
	public void addPages() {
		addPage(new TargetDefinitionContentPage(fDefinition));
	}

	/**
	 * Sets the target definition to be edited. Will delegate to pages to
	 * refresh controls if already created.
	 *
	 * @param definition target definition
	 * @param createWorkingCopy if true, a copy of the definition will be created to edit, if false the definition will be edited directly
	 */
	public void setTargetDefinition(ITargetDefinition definition, boolean createWorkingCopy) {
		ITargetDefinition workingCopy = null;
		if (createWorkingCopy) {
			try {
				ITargetPlatformService service = TargetDefinitionPage.getTargetService();
				if (service != null) {
					if (definition.getHandle().exists()) {
						// Make a copy from the handle
						workingCopy = definition.getHandle().getTargetDefinition();
					} else {
						// If no handle use the service to create a new one
						workingCopy = service.newTarget();
					}
					service.copyTargetDefinition(definition, workingCopy);
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		} else {
			workingCopy = definition;
		}
		fDefinition = workingCopy;
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			((TargetDefinitionPage) page).targetChanged(workingCopy);
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
