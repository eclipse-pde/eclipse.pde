/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions.SimpleCSPreviewAction;
import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;

public class SimpleCSEditorContributor extends PDEFormTextEditorContributor {

	private SimpleCSPreviewAction fPreviewAction;

	public SimpleCSEditorContributor() {
		super("&Simple Cheat Sheet"); //$NON-NLS-1$
		fPreviewAction = null;
	}

	@Override
	protected void makeActions() {
		super.makeActions();
		// Make the preview action
		fPreviewAction = new SimpleCSPreviewAction();
	}

	@Override
	public void contextMenuAboutToShow(IMenuManager manager, boolean addClipboard) {
		// Get the model
		ISimpleCSModel model = (ISimpleCSModel) getEditor().getAggregateModel();
		// Set the cheat sheet object
		fPreviewAction.setDataModelObject(model.getSimpleCS());
		// Set the editor input
		fPreviewAction.setEditorInput(getEditor().getEditorInput());
		// Add the preview action to the context menu
		manager.add(fPreviewAction);
		manager.add(new Separator());
		super.contextMenuAboutToShow(manager, addClipboard);
	}

	public SimpleCSPreviewAction getPreviewAction() {
		return fPreviewAction;
	}

	@Override
	public boolean supportsHyperlinking() {
		return true;
	}

}
