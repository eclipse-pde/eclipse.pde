/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.editor.actions.OpenSchemaAction;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyExtentAction;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class PluginSearchActionGroup extends ActionGroup {

	private IBaseModel fModel;

	public void setBaseModel(IBaseModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager menu) {
		ActionContext context = getContext();
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() == 1) {
				Object object = sSelection.getFirstElement();
				addShowDescriptionAction(object, menu);
				addOpenSchemaAction(object, menu);
				addFindDeclarationsAction(object, menu);
				addFindReferencesAction(object, menu);
				addDependencyExtentAction(object, menu);
			}
		}
	}

	/**
	 * @param object
	 * @param menu
	 */
	private void addOpenSchemaAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginExtension) {
			// From PDEOutlinePage
			OpenSchemaAction action = new OpenSchemaAction();
			action.setInput((IPluginExtension) object);
			action.setEnabled(true);
			menu.add(action);
		} else if (object instanceof IPluginExtensionPoint) {
			// From PluginSearchResultPage
			// From ExtensionPointsSection
			OpenSchemaAction action = new OpenSchemaAction();
			IPluginExtensionPoint point = (IPluginExtensionPoint) object;
			String pointID = point.getFullId();
			// Ensure the extension point ID is fully qualified
			if (pointID.indexOf('.') == -1) {
				// Point ID is not fully qualified
				action.setInput(fullyQualifyPointID(pointID));
			} else {
				action.setInput(point);
			}
			action.setEnabled(true);
			menu.add(action);
		}
	}

	private void addFindDeclarationsAction(Object object, IMenuManager menu) {
		if (object instanceof ImportObject)
			object = ((ImportObject) object).getImport();

		if (object instanceof IPluginBase || object instanceof IPluginExtension || object instanceof IPluginImport) {
			menu.add(new FindDeclarationsAction(object));
		}
	}

	private void addShowDescriptionAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint extPoint = (IPluginExtensionPoint) object;
			String pointID = extPoint.getFullId();
			if (pointID.indexOf('.') == -1) {
				// Point ID is not fully qualified
				pointID = fullyQualifyPointID(pointID);
			}
			menu.add(new ShowDescriptionAction(extPoint, pointID));
		} else if (object instanceof IPluginExtension) {
			String point = ((IPluginExtension) object).getPoint();
			IPluginExtensionPoint extPoint = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(point);
			if (extPoint != null)
				menu.add(new ShowDescriptionAction(extPoint));
		}
	}

	private String fullyQualifyPointID(String pointID) {
		if (fModel instanceof IPluginModelBase) {
			String basePointID = ((IPluginModelBase) fModel).getPluginBase().getId();
			pointID = basePointID + '.' + pointID;
		}
		return pointID;
	}

	private void addFindReferencesAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginModelBase) {
			object = ((IPluginModelBase) object).getPluginBase();
		} else if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}
		if (object instanceof IPluginExtensionPoint || object instanceof IPluginImport || (object instanceof IPlugin) || (object instanceof IPluginExtension))
			menu.add(new FindReferencesAction(object));
	}

	private void addDependencyExtentAction(Object object, IMenuManager menu) {
		if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}

		if (object instanceof IPluginImport) {
			String id = ((IPluginImport) object).getId();
			IResource resource = ((IPluginImport) object).getModel().getUnderlyingResource();
			if (resource != null) {
				menu.add(new DependencyExtentAction(resource.getProject(), id));
			}
		}
	}

}
