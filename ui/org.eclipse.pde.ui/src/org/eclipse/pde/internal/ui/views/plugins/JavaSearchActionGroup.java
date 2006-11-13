/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class JavaSearchActionGroup extends ActionGroup {

	class JavaSearchAction extends Action {

		private boolean add;

		public JavaSearchAction(boolean add) {
			this.add = add;
			if(add)
				setText(PDEUIMessages.PluginsView_addToJavaSearch);
			else
				setText(PDEUIMessages.PluginsView_removeFromJavaSearch);

		}

		public void run() {
			handleJavaSearch(add);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		ActionContext context = getContext();
		ISelection selection = context.getSelection();
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;

			boolean addSeparator = false;

			if (canDoJavaSearchOperation(sSelection, true)) {
				menu.add(new JavaSearchAction(true));
				addSeparator = true;
			}
			if (canDoJavaSearchOperation(sSelection, false)) {
				menu.add(new JavaSearchAction(false));
				addSeparator = true;
			}
			if (addSeparator) {
				menu.add(new Separator());
			}
		}

	}

	private boolean canDoJavaSearchOperation(
			IStructuredSelection selection,
			boolean add) {
		int nhits = 0;
		ModelEntry entry = null;

		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			entry = getModelEntry(iter.next());
			
			if (entry == null)
				return false;
			
			if (entry.getWorkspaceModel() == null) {
				if (add && entry.isInJavaSearch() == false)
					nhits++;
				if (!add && entry.isInJavaSearch())
					nhits++;
			}

		}
		return nhits > 0;
	}
	
	private ModelEntry getModelEntry(Object object) {
		ModelEntry entry = null;
		if (object instanceof IAdaptable) {
			entry = (ModelEntry) ((IAdaptable) object).getAdapter(ModelEntry.class);
		}
		if (object instanceof ModelEntry) {
			entry = (ModelEntry) object;
		}
		return entry;
	}

	private void handleJavaSearch(final boolean add) {
		IStructuredSelection selection =
			(IStructuredSelection) getContext().getSelection();
		if (selection.size() == 0)
			return;

		ArrayList result = new ArrayList();

		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			ModelEntry entry = getModelEntry(iter.next());

				if (entry.getWorkspaceModel() != null)
					continue;
				if (entry.isInJavaSearch() == !add)
					result.add(entry);
			}
		if (result.size() == 0)
			return;
		final ModelEntry[] array =
			(ModelEntry[]) result.toArray(new ModelEntry[result.size()]);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
			throws InvocationTargetException {
				PluginModelManager manager =
					PDECore.getDefault().getModelManager();
				try {
					manager.setInJavaSearch(array, add, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}

}
