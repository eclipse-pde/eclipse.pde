package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.search.dependencies.*;
import org.eclipse.ui.actions.*;


public class PluginSearchActionGroup extends ActionGroup {
	
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
				addFindDeclarationsAction(object, menu);
				addFindReferencesAction(object, menu);
				addShowDescriptionAction(object, menu);
				addDependencyExtentAction(object, menu);
			}
		}
	}

	private void addFindDeclarationsAction(Object object, IMenuManager menu) {
		if (object instanceof ImportObject)
			object = ((ImportObject)object).getImport();
		
		if (object instanceof IPluginBase 
				|| object instanceof IPluginExtension
				|| object instanceof IPluginImport) {
			menu.add(new FindDeclarationsAction(object));
		}
	}

	private void addShowDescriptionAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginExtensionPoint) {
			menu.add(new ShowDescriptionAction((IPluginExtensionPoint)object));
		} else if (object instanceof IPluginExtension) {
			String point = ((IPluginExtension)object).getPoint();
			IPluginExtensionPoint extPoint = PDECore.getDefault().findExtensionPoint(point);
			if (extPoint != null)
				menu.add(new ShowDescriptionAction(extPoint));
		}
	}

	private void addFindReferencesAction(Object object, IMenuManager menu) {
		if (object instanceof ModelEntry) {
			object = ((ModelEntry) object).getActiveModel().getPluginBase();
		} else if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}
		if (object instanceof IPluginExtensionPoint
			|| object instanceof IPluginImport
			|| (object instanceof IPlugin))
			menu.add(new FindReferencesAction(object));
	}
	
	private void addDependencyExtentAction(Object object, IMenuManager menu) {
		if (object instanceof ImportObject) {
			object = ((ImportObject)object).getImport();
		}
		
		if (object instanceof IPluginImport) {
			String id = ((IPluginImport)object).getId();
			IResource resource = ((IPluginImport)object).getModel().getUnderlyingResource();
			if (resource != null) {
				menu.add(new DependencyExtentAction(resource.getProject(), id));
			}
		}
	}

}
