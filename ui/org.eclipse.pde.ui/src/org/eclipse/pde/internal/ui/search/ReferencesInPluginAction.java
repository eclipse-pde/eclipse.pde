package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

/**
 * @author wassimm
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ReferencesInPluginAction extends Action {
	
	private static final String KEY_REFERENCES = "DependencyExtent.references";
	
	ISearchResultViewEntry entry;
	
	
	public ReferencesInPluginAction(ISearchResultViewEntry entry) {
		this.entry = entry;
		setText(PDEPlugin.getResourceString(KEY_REFERENCES) + " " + entry.getResource().getName());
	}
	
	public void run() {
		try {
			SearchUI.activateSearchResultView();
			IRunnableWithProgress operation = null;
			Object object = entry.getGroupByKey();
			if (object instanceof IJavaElement) {
				operation = new JavaSearchOperation((IJavaElement)object, (IProject)entry.getResource());
			} else {
				operation =
					new PluginSearchUIOperation(
						getPluginSearchInput((IPluginExtensionPoint) object),
						new PluginSearchResultCollector());
			}
			ProgressMonitorDialog pmd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			pmd.run(true, true, operation);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}
	
	private PluginSearchInput getPluginSearchInput(IPluginExtensionPoint object) {
		PluginSearchInput input = new PluginSearchInput();
		input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
		input.setSearchString(
			((IPluginExtensionPoint) object).getPluginBase().getId()
				+ "."
				+ ((IPluginExtensionPoint) object).getId());
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		HashSet set = new HashSet();
		IResource resource = ((IProject)entry.getResource()).getFile("plugin.xml");
		if (!resource.exists())
			resource = ((IProject)entry.getResource()).getFile("fragment.xml");
			
		set.add(resource);
		input.setSearchScope(
			new PluginSearchScope(
				PluginSearchScope.SCOPE_SELECTION,
				PluginSearchScope.EXTERNAL_SCOPE_NONE,
				set));
		return input;
	}


}
