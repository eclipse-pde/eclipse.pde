package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;

public class PropertiesAction extends Action {
	public static final String LABEL = "Actions.properties.label";
	private PDEMultiPageEditor editor;

public PropertiesAction(PDEMultiPageEditor editor) {
	this.editor = editor;
	setText(PDEPlugin.getResourceString(LABEL));
	setImageDescriptor(PDEPluginImages.DESC_PROPERTIES);
	setHoverImageDescriptor(PDEPluginImages.DESC_PROPERTIES_HOVER);
	setDisabledImageDescriptor(PDEPluginImages.DESC_PROPERTIES_DISABLED);
}
public void run() {
	try {
		String viewId = IPageLayout.ID_PROP_SHEET;
		IWorkbenchPage perspective = PDEPlugin.getActivePage();
		IViewPart view = perspective.showView(viewId);
		editor.updateSynchronizedViews(editor.getCurrentPage());
		perspective.activate(editor);
		perspective.bringToTop(view);
	} catch (PartInitException e) {
		PDEPlugin.logException(e);
	}
}
}
