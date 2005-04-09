/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
public class PropertiesAction extends Action {
	private PDEFormEditor editor;
	public PropertiesAction(PDEFormEditor editor) {
		this.editor = editor;
		setText(PDEUIMessages.Actions_properties_label);
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
