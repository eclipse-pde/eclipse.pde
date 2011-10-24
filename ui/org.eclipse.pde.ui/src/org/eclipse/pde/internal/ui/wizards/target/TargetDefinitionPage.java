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
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECore;

/**
 * Common function for target definition wizard pages.
 */
public abstract class TargetDefinitionPage extends WizardPage {

	private ITargetDefinition fDefinition;

	/**
	 * @param pageName
	 */
	protected TargetDefinitionPage(String pageName, ITargetDefinition definition) {
		super(pageName);
		fDefinition = definition;
	}

	/**
	 * Returns the target being edited.
	 * 
	 * @return target definition or <code>null</code>
	 */
	public ITargetDefinition getTargetDefinition() {
		return fDefinition;
	}

	/**
	 * Notification the target being edited has changed to a new model. Subclasses
	 * should override.
	 */
	protected void targetChanged(ITargetDefinition definition) {
		fDefinition = definition;
	}

	/**
	 * Returns the target service or <code>null</code> if none.
	 * 
	 * @return target service or <code>null</code>
	 */
	protected static ITargetPlatformService getTargetService() {
		return (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
	}
}
