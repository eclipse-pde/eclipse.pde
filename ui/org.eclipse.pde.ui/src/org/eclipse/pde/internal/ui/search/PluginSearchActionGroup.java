package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.IMenuManager;
//import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.search.internal.ui.SearchResultViewEntry;
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
				addFindDeclarationsAction(object, menu);
				addFindReferencesAction(object, menu);
				addShowDescriptionAction(object, menu);
			}
		}
	}

	private void addFindDeclarationsAction(Object object, IMenuManager menu) {
		if (object instanceof ISearchResultViewEntry) {
			object = ((SearchResultViewEntry) object).getGroupByKey();
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
			object = ((SearchResultViewEntry) object).getGroupByKey();
		} else if (object instanceof ModelEntry) {
			object = ((ModelEntry) object).getActiveModel().getPluginBase();
		} else if (object instanceof ImportObject) {
			object = ((ImportObject) object).getImport();
		}
		if (object instanceof IPluginExtensionPoint
			|| object instanceof IPluginImport
			|| (object instanceof IPlugin))
			menu.add(new FindReferencesAction(object));

		//if (object instanceof IPluginExtensionPoint)
		//menu.add(new Separator());
	}

	private void addShowDescriptionAction(Object object, IMenuManager menu) {
		if (object instanceof ISearchResultViewEntry)
			object = ((SearchResultViewEntry) object).getGroupByKey();
		if (object instanceof IPluginExtensionPoint)
			menu.add(
				new ShowDescriptionAction((IPluginExtensionPoint) object));
	}

}
