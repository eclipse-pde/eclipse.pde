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
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.core.resources.IResource;

public class ShowInWorkspaceAction extends Action {
	private String viewId;
	private ISelectionProvider provider;

	/**
	 * Constructor for ShowInWorkspaceAction.
	 */
	public ShowInWorkspaceAction(String viewId, ISelectionProvider provider) {
		this.viewId = viewId;
		this.provider = provider;
	}

	/**
	 * Constructor for ShowInWorkspaceAction.
	 * @param text
	 */
	protected ShowInWorkspaceAction(String text) {
		super(text);
	}

	public boolean isApplicable() {
		IStructuredSelection selection = (IStructuredSelection) provider.getSelection();
		if (selection.isEmpty())
			return false;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof ModelEntry))
				return false;
			ModelEntry entry = (ModelEntry) obj;
			IPluginModelBase model = entry.getActiveModel();
			if (model.getUnderlyingResource() == null)
				return false;
		}
		return true;
	}

	public void run() {
		List v = collectResources();
		IWorkbenchPage page = PDEPlugin.getActivePage();
		try {
			IViewPart view = page.showView(viewId);
			if (view instanceof ISetSelectionTarget) {
				ISelection selection = new StructuredSelection(v);
				((ISetSelectionTarget) view).selectReveal(selection);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	private List collectResources() {
		ArrayList list = new ArrayList();
		IStructuredSelection selection = (IStructuredSelection)provider.getSelection();
		if (selection.isEmpty())
			return list;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) obj;
				IPluginModelBase model = entry.getActiveModel();
				IResource resource = model.getUnderlyingResource();
				if (resource != null)
					list.add(resource);
			}

		}
		return list;
	}
}