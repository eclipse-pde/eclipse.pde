/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.*;

public class PDEPerspective implements IPerspectiveFactory {

	private IPageLayout factory;

	public PDEPerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout factory) {
		this.factory = factory;
		addViews();
	}

	private void addViews() {
		IFolderLayout topLeft = factory.createFolder("topLeft", //$NON-NLS-1$
				IPageLayout.LEFT, 0.25f, factory.getEditorArea());
		topLeft.addPlaceholder(IPageLayout.ID_RES_NAV);
		topLeft.addView(JavaUI.ID_PACKAGES);
		topLeft.addPlaceholder(JavaUI.ID_TYPE_HIERARCHY);
		topLeft.addView(IPDEUIConstants.PLUGINS_VIEW_ID);

		IFolderLayout bottom = factory.createFolder("bottomRight", //$NON-NLS-1$
				IPageLayout.BOTTOM, 0.75f, factory.getEditorArea());
		bottom.addView("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
		bottom.addView(IPageLayout.ID_TASK_LIST);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);

		factory.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.75f, factory.getEditorArea());

		factory.addNewWizardShortcut("org.eclipse.pde.ui.NewProjectWizard"); //$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.pde.ui.NewFeatureProjectWizard"); //$NON-NLS-1$

	}

}
