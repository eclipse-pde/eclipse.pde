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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
//import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.plugin.PluginDocumentNode;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class PluginSearchActionGroup extends ActionGroup {

	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		ActionContext context = getContext();
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() == 1) {
				Object object = sSelection.getFirstElement();
				if (object instanceof PluginDocumentNode) {
					object = ((PluginDocumentNode)object).getPluginObjectNode();
				}
				addFindDeclarationsAction(object, menu);
				addFindReferencesAction(object, menu);
				addShowDescriptionAction(object, menu);
				addDependencyExtentAction(object, menu);
			}
		}
	}

	private void addDependencyExtentAction(Object object, IMenuManager menu) {
		if (object instanceof ISearchResultViewEntry) {
			object = ((ISearchResultViewEntry) object).getGroupByKey();
		} else if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}
		
		if (object instanceof IPluginImport
			&& ((IPluginImport) object).getModel().getUnderlyingResource()
				!= null) {
			menu.add(new Separator());
			menu.add(new DependencyExtentAction((IPluginImport) object));
		}
	}
	

	private void addFindDeclarationsAction(Object object, IMenuManager menu) {
		if (object instanceof ISearchResultViewEntry) {
			object = ((ISearchResultViewEntry) object).getGroupByKey();
		} else if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}
		if (object instanceof IPluginImport
			|| object instanceof IPluginExtension) {
			menu.add(new FindDeclarationsAction(object));
		}
	}

	private void addFindReferencesAction(Object object, IMenuManager menu) {
		if (object instanceof ISearchResultViewEntry) {
			object = ((ISearchResultViewEntry) object).getGroupByKey();
		} else if (object instanceof ModelEntry) {
			object = ((ModelEntry) object).getActiveModel().getPluginBase();
		} else if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}
		if (object instanceof IPluginExtensionPoint
			|| object instanceof IPluginImport
			|| (object instanceof IPlugin))
			menu.add(new FindReferencesAction(object));
	}

	private void addShowDescriptionAction(Object object, IMenuManager menu) {
		if (object instanceof ISearchResultViewEntry)
			object = ((ISearchResultViewEntry) object).getGroupByKey();
		if (object instanceof IPluginExtensionPoint) {
			menu.add(new ShowDescriptionAction((IPluginExtensionPoint) object));
		} else if (object instanceof IPluginExtension) {
			String pointId = ((IPluginExtension) object).getPoint();
			IPluginExtensionPoint extPoint =
				PDECore.getDefault().findExtensionPoint(pointId);
			if (extPoint != null)
				menu.add(new ShowDescriptionAction(extPoint));
		}
	}

}
