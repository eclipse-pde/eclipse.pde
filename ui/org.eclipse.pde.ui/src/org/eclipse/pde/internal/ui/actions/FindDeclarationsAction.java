package org.eclipse.pde.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.search.PluginSearchResultCollector;
import org.eclipse.pde.internal.ui.search.PluginSearchUIOperation;


public class FindDeclarationsAction extends Action {
	
	private static final String KEY_DECLARATIONS = "SearchAction.declarations";

	private Object object;

	public FindDeclarationsAction(Object object) {
		this.object = object;
		setText(PDEPlugin.getResourceString(KEY_DECLARATIONS));
	}
	public void run() {
		if (object instanceof IPluginImport) {
			PluginSearchInput input = new PluginSearchInput();
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPluginImport) object).getId());
			input.setSearchLimit(PluginSearchInput.LIMIT_DECLARATIONS);
			input.setSearchScope(new PluginSearchScope());
			try {
				ProgressMonitorDialog pmd =
					new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
				PluginSearchUIOperation op =
					new PluginSearchUIOperation(
						input,
						new PluginSearchResultCollector());
				pmd.run(true, true, op);
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
		}
	}

}
