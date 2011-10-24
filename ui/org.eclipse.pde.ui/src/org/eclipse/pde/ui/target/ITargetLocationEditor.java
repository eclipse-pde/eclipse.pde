/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.target;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;

/**
 * Contributed target locations that want to support editing in the target wizard and editor must adapt
 * their {@link ITargetLocation} to this interface. 
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.7
 */
public interface ITargetLocationEditor {

	/**
	 * Returns whether this target location can be edited by this editor. This method will be 
	 * called when a selection is made to determine if the edit button should be enabled.
	 * 
	 * @param target the target definition being edited
	 * @param targetLocation the target location to edit
	 * @return whether this editor can edit the target location
	 */
	public boolean canEdit(ITargetDefinition target, ITargetLocation targetLocation);

	/**
	 * Returns a wizard that will be opened to edit the given target location.  The wizard
	 * is responsible for modifying the target location and/or target. The target definition
	 * will be resolved if the wizard completes successfully.
	 *  
	 * @param target the target definition being edited
	 * @param targetLocation the target location to edit
	 * @return wizard to open for editing the target location
	 */
	public IWizard getEditWizard(ITargetDefinition target, ITargetLocation targetLocation);

}
