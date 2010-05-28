/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class JavaSearchActionGroup extends ActionGroup {

	class JavaSearchAction extends Action {

		private boolean add;

		public JavaSearchAction(boolean add) {
			this.add = add;
			if (add)
				setText(PDEUIMessages.PluginsView_addToJavaSearch);
			else
				setText(PDEUIMessages.PluginsView_removeFromJavaSearch);
		}

		public void run() {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			if (!add && store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH)) {
				boolean confirm = MessageDialog.openConfirm(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.JavaSearchActionGroup_RemoveJavaSearchTitle, PDEUIMessages.JavaSearchActionGroup_RemoveJavaSearchMessage);
				if (confirm) {
					store.setValue(IPreferenceConstants.ADD_TO_JAVA_SEARCH, false);
					handleJavaSearch(add);
				}
			} else {
				handleJavaSearch(add);
			}
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

	private boolean canDoJavaSearchOperation(IStructuredSelection selection, boolean add) {
		int nhits = 0;
		IPluginModelBase model = null;
		SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			model = getModel(iter.next());
			if (model == null)
				return false;

			if (model.getUnderlyingResource() == null) {
				if (add == !manager.isInJavaSearch(model.getPluginBase().getId()))
					nhits++;
			}
		}
		return nhits > 0;
	}

	private IPluginModelBase getModel(Object object) {
		IPluginModelBase model = null;
		if (object instanceof IAdaptable) {
			model = (IPluginModelBase) ((IAdaptable) object).getAdapter(IPluginModelBase.class);
		} else if (object instanceof IPluginModelBase) {
			model = (IPluginModelBase) object;
		}
		return model;
	}

	private void handleJavaSearch(final boolean add) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		if (selection.size() == 0)
			return;

		ArrayList result = new ArrayList();
		SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IPluginModelBase model = getModel(iter.next());
			if (model != null && model.getUnderlyingResource() == null && manager.isInJavaSearch(model.getPluginBase().getId()) != add) {
				result.add(model);
			}
		}
		if (result.size() == 0)
			return;
		final IPluginModelBase[] array = (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);

		IRunnableWithProgress op = new JavaSearchOperation(array, add);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}

}
