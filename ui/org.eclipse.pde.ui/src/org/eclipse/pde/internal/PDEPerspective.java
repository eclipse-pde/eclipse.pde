package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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

	IFolderLayout bottomRight =
		factory.createFolder(
			"bottomRight",
			IPageLayout.BOTTOM,
			(float) 0.72,
			factory.getEditorArea());

	bottomRight.addView(IPageLayout.ID_TASK_LIST);
	bottomRight.addView("org.eclipse.pde.runtime.LogView");

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
}
}
