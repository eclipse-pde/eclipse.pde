package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.editor.manifest.ManifestEditor;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchGoToAction extends Action {
	
	public PluginSearchGoToAction() {
		super();
	}
	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		ISearchResultView view= SearchUI.getSearchResultView();		
		ISelection selection= view.getSelection();
		Object element= null;
		if (selection instanceof IStructuredSelection)
			element= ((IStructuredSelection)selection).getFirstElement();
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry= (ISearchResultViewEntry)element;
			IPluginObject object = (IPluginObject)entry.getGroupByKey();
			if (object instanceof IPluginBase)
				ManifestEditor.openPluginEditor((IPluginBase)object);
			ManifestEditor.openPluginEditor(object.getModel().getPluginBase(),object);
		}	
	}


}
