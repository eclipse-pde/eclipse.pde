package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.ui.actions.FindDeclarationsAction;
import org.eclipse.pde.internal.ui.actions.FindReferencesAction;
import org.eclipse.pde.internal.ui.actions.ShowDescriptionAction;
import org.eclipse.search.internal.ui.SearchResultViewEntry;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchActionGroup extends ActionGroup {
	/**
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		ActionContext context = getContext();
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection)selection;
			if (sSelection.size() == 1) {
				Object object = sSelection.getFirstElement();
				if (object instanceof ISearchResultViewEntry) {
					object = ((SearchResultViewEntry)object).getGroupByKey();
				}
				addFindDeclarationsAction(object, menu);
				addFindReferencesAction(object, menu);
				addShowDescriptionAction(object, menu);
			}	
		}
	}
	
	private void addFindDeclarationsAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginImport)
			menu.add(new FindDeclarationsAction(object));
	}

	private void addFindReferencesAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginExtensionPoint
			|| object instanceof IPluginImport
			|| (object instanceof IPlugin))
			menu.add(new FindReferencesAction(object));
	}
	
	private void addShowDescriptionAction(Object object, IMenuManager menu) {
		if (object instanceof IPluginExtensionPoint)
			menu.add(new ShowDescriptionAction((IPluginExtensionPoint)object));
	}

}
