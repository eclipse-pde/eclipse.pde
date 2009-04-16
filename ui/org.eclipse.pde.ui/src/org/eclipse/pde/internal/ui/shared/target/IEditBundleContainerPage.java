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
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * Interface for wizard pages used to edit bundle containers.
 * 
 * @see EditBundleContainerWizard
 */
public interface IEditBundleContainerPage extends IWizardPage {

	/**
	 * Returns a bundle container containing edited values taken from the wizard page.
	 * @return bundle container
	 */
	public IBundleContainer getBundleContainer();

	/**
	 * Informs the wizard page that the wizard is closing and any settings/preferences
	 * should be stored.
	 */
	public void storeSettings();

}
