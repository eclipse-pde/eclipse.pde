/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ISetSelectionTarget;

public class ShowInWorkspaceAction extends Action {
	private String fViewId;
	private ISelectionProvider fProvider;

	/**
	 * Constructor for ShowInWorkspaceAction.
	 */
	public ShowInWorkspaceAction(String viewId, ISelectionProvider provider) {
		fViewId = viewId;
		fProvider = provider;
	}

	/**
	 * Constructor for ShowInWorkspaceAction.
	 * @param text
	 */
	protected ShowInWorkspaceAction(String text) {
		super(text);
	}

	public boolean isApplicable() {
		IStructuredSelection selection = (IStructuredSelection) fProvider.getSelection();
		if (selection.isEmpty())
			return false;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof IPluginModelBase))
				return false;
			if (((IPluginModelBase)obj).getUnderlyingResource() == null)
				return false;
		}
		return true;
	}

	public void run() {
		List v = collectResources();
		IWorkbenchPage page = PDEPlugin.getActivePage();
		try {
			IViewPart view = page.showView(fViewId);
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
		IStructuredSelection selection = (IStructuredSelection)fProvider.getSelection();
		if (selection.isEmpty())
			return list;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof IPluginModelBase) {
				list.add(((IPluginModelBase)obj).getUnderlyingResource());
			}

		}
		return list;
	}
}
