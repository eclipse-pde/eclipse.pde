/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

public PDEPerspective() {
	super();
}

public void createInitialLayout(IPageLayout factory) {

	// Top left folder.
	IFolderLayout topLeft =
		factory.createFolder(
			"topLeft",
			IPageLayout.LEFT,
			(float) 0.25,
			factory.getEditorArea());
	//topLeft.addView(IPageLayout.ID_RES_NAV);
	topLeft.addPlaceholder(IPageLayout.ID_RES_NAV);
	topLeft.addView(JavaUI.ID_PACKAGES);
	topLeft.addView(JavaUI.ID_TYPE_HIERARCHY);
	topLeft.addView(PDEPlugin.PLUGINS_VIEW_ID);

	IFolderLayout bottomRight =
		factory.createFolder(
			"bottomRight",
			IPageLayout.BOTTOM,
			(float) 0.72,
			factory.getEditorArea());

	bottomRight.addView(IPageLayout.ID_TASK_LIST);
	bottomRight.addView("org.eclipse.pde.runtime.LogView");
	bottomRight.addView(IDebugUIConstants.ID_CONSOLE_VIEW);

	factory.addView(
		IPageLayout.ID_PROP_SHEET, 
		IPageLayout.LEFT, 
		(float) 0.35,
		"bottomRight");

	factory.addView(
		IPageLayout.ID_OUTLINE,
		IPageLayout.RIGHT,
		(float) 0.75,
		factory.getEditorArea());

	// Add action sets
	factory.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
	factory.addActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
	factory.addActionSet(JavaUI.ID_ACTION_SET);
	factory.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
;
	
	// new actions 
	factory.addNewWizardShortcut("org.eclipse.pde.internal.ui.wizards.project.NewProjectWizard");
	factory.addNewWizardShortcut("org.eclipse.pde.internal.ui.wizards.project.NewFragmentWizard");
	factory.addNewWizardShortcut("org.eclipse.pde.internal.ui.wizards.extension.NewSchemaFileWizard");
	factory.addNewWizardShortcut("org.eclipse.pde.internal.ui.feature.NewFeatureProjectWizard");
	
	factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard"); //$NON-NLS-1$
	factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewClassCreationWizard"); //$NON-NLS-1$
	factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
	factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSourceFolderCreationWizard");	 //$NON-NLS-1$
	factory.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSnippetFileCreationWizard"); //$NON-NLS-1$
	factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
	factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
}
}
