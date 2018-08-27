/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 247265
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTreePart;
import org.eclipse.swt.widgets.Composite;

public class BasePluginListPage extends WizardPage {
	protected WizardCheckboxTreePart treePart;

	/**
	 * @param pageName
	 */
	public BasePluginListPage(String pageName) {
		super(pageName);
		treePart = new WizardCheckboxTreePart(null);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public BasePluginListPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		treePart = new WizardCheckboxTreePart(null);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	public void createControl(Composite parent) {

	}

	@Override
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			treePart.getControl().setFocus();
		}
	}

}
