package org.eclipse.pde.internal.ui.search;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.editor.manifest.ManifestEditor;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.ui.PartInitException;


public class DependencyExtentGoToAction extends Action {
	public void run() {
		try {
			ISearchResultView view = SearchUI.getSearchResultView();
			ISelection selection = view.getSelection();
			Object element = null;
			if (selection instanceof IStructuredSelection)
				element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof ISearchResultViewEntry) {
				ISearchResultViewEntry entry = (ISearchResultViewEntry) element;
				element = entry.getGroupByKey();
				if (element instanceof IJavaElement) {
					JavaUI.openInEditor((IJavaElement) element);
				} else if (element instanceof IPluginObject) {
					ManifestEditor.openPluginEditor(
						((IPluginObject) element).getModel().getPluginBase(),
						(IPluginObject) element,
						entry.getSelectedMarker());
				}
			}
		} catch (PartInitException e) {
		} catch (JavaModelException e) {
		}
	}

}
