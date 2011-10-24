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

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.graphics.Point;

/**
 * Embedded wizard to edit a target
 */
public class EditTargetNode implements IWizardNode {

	private EditTargetDefinitionWizard fWizard;
	private ITargetDefinition fDefinition;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#dispose()
	 */
	public void dispose() {
		if (fWizard != null) {
			fWizard.dispose();
			fWizard = null;
			fDefinition = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#getExtent()
	 */
	public Point getExtent() {
		return new Point(-1, -1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#getWizard()
	 */
	public IWizard getWizard() {
		if (fWizard == null) {
			fWizard = new EditTargetDefinitionWizard(fDefinition, false);
			fWizard.setWindowTitle(PDEUIMessages.EditTargetNode_0);
		}
		return fWizard;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#isContentCreated()
	 */
	public boolean isContentCreated() {
		return fWizard != null;
	}

	/**
	 * Sets the target being edited.
	 * 
	 * @param definition
	 */
	public void setTargetDefinition(ITargetDefinition definition) {
		fDefinition = definition;
		if (fWizard != null) {
			fWizard.setTargetDefinition(definition, false);
		}
	}

}
