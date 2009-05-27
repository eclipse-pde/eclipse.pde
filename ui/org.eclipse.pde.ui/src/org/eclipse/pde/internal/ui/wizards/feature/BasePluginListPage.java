/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.swt.widgets.Composite;

public class BasePluginListPage extends WizardPage {
	protected WizardCheckboxTablePart tablePart;

	/**
	 * @param pageName
	 */
	public BasePluginListPage(String pageName) {
		super(pageName);
		tablePart = new WizardCheckboxTablePart(null);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public BasePluginListPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		tablePart = new WizardCheckboxTablePart(null);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {

	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			tablePart.getControl().setFocus();
		}
	}

}
