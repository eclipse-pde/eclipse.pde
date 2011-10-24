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
 * This interface represents a wizard which will be used to add target locations
 * to a target definition.  The wizard will be available from the Add... button
 * on the locations tab in the target wizard or editor.  Implementors must contribute
 * their wizards through the <code>org.eclipse.pde.ui.targetLocationProvisioners</code>
 * extension point.  Implementing classes must have a default constructor (zero
 * arguments) for the class to be created from the extension.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.7
 */
public interface ITargetLocationWizard extends IWizard {

	/**
	 * After the wizard is created this method will be called, providing this wizard with information
	 * from the target definition the new location(s) will be added to.  It is not recommended that 
	 * implementors modify the target as UI behaviour is not API (it may change between releases).
	 * 
	 * @param target the target definition this location is being added to
	 */
	public void setTarget(ITargetDefinition target);

	/**
	 * Returns an array of target locations to be added to the target definition. Will be
	 * called after the wizard is closed.
	 * 
	 * @return an array of target locations to add to the target definition
	 */
	public ITargetLocation[] getLocations();

}
