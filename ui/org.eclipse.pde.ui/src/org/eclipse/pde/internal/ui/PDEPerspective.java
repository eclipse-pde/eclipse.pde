/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.ui.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.debug.ui.IDebugUIConstants;

public class PDEPerspective implements IPerspectiveFactory {
	
	private IPageLayout factory;

	public PDEPerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout factory) {
		this.factory = factory;
		addViews();
		addActionSets();
		addNewWizardShortcuts();
		addPerspectiveShortcuts();
		addViewShortcuts();
	}
	
	private void addPerspectiveShortcuts() {
		factory.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
		factory.addPerspectiveShortcut(JavaUI.ID_PERSPECTIVE);
		factory.addPerspectiveShortcut(IDebugUIConstants.ID_DEBUG_PERSPECTIVE);		
	}
	
	private void addViews() {
		IFolderLayout topLeft =
			factory.createFolder(
				"topLeft", //$NON-NLS-1$
				IPageLayout.LEFT,
				0.25f,
				factory.getEditorArea());
		topLeft.addPlaceholder(IPageLayout.ID_RES_NAV);
		topLeft.addView(JavaUI.ID_PACKAGES);
		topLeft.addPlaceholder(JavaUI.ID_TYPE_HIERARCHY);
		topLeft.addView(PDEPlugin.PLUGINS_VIEW_ID);

		IFolderLayout bottom =
			factory.createFolder(
				"bottomRight", //$NON-NLS-1$
				IPageLayout.BOTTOM,
				0.75f,
				factory.getEditorArea());
		bottom.addView("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
		bottom.addView(IPageLayout.ID_TASK_LIST);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addView(IPageLayout.ID_PROP_SHEET);
		
		factory.addView(
			IPageLayout.ID_OUTLINE,
			IPageLayout.RIGHT,
			0.75f,
			factory.getEditorArea());		
	}
	
	private void addActionSets() {
		factory.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		factory.addActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
		factory.addActionSet(JavaUI.ID_ACTION_SET);
		factory.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);		
	}
	
	private void addNewWizardShortcuts() {
		factory.addNewWizardShortcut("org.eclipse.pde.ui.NewProductConfigurationWizard"); //$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard"); //$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewClassCreationWizard"); //$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSourceFolderCreationWizard");	 //$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
	}
	
	private void addViewShortcuts() {
		factory.addShowViewShortcut(JavaUI.ID_PACKAGES);
		factory.addShowViewShortcut("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
		factory.addShowViewShortcut(PDEPlugin.PLUGINS_VIEW_ID);
		factory.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		factory.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		factory.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		factory.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	}
}
