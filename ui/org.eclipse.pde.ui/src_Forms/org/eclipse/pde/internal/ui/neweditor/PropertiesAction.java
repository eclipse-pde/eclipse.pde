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
package org.eclipse.pde.internal.ui.neweditor;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
public class PropertiesAction extends Action {
	public static final String LABEL = "Actions.properties.label";
	private PDEFormEditor editor;
	public PropertiesAction(PDEFormEditor editor) {
		this.editor = editor;
		setText(PDEPlugin.getResourceString(LABEL));
		setImageDescriptor(PDEPluginImages.DESC_PROPERTIES);
		setDisabledImageDescriptor(PDEPluginImages.DESC_PROPERTIES_DISABLED);
	}
	public void run() {
		try {
			String viewId = IPageLayout.ID_PROP_SHEET;
			IWorkbenchPage perspective = PDEPlugin.getActivePage();
			IViewPart view = perspective.showView(viewId);
			editor.updatePropertySheet(editor.getActivePageInstance());
			perspective.activate(editor);
			perspective.bringToTop(view);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}