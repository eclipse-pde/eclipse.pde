/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.target.ITargetLocation;

/**
 * Interface for wizard pages used to edit target locations.
 * 
 * @see EditBundleContainerWizard
 */
public interface IEditBundleContainerPage extends IWizardPage {

	/**
	 * Returns a target location containing edited values taken from the wizard page.
	 * @return target location
	 */
	public ITargetLocation getBundleContainer();

	/**
	 * Informs the wizard page that the wizard is closing and any settings/preferences
	 * should be stored.
	 */
	public void storeSettings();

}
