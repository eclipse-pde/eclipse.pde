package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.SearchUI;


public class FindDeclarationsAction extends Action {
	
	private static final String KEY_DECLARATION = "SearchAction.Declaration";

	private Object object;

	public FindDeclarationsAction(Object object) {
		this.object = object;
		setText(PDEPlugin.getResourceString(KEY_DECLARATION));
	}
	public void run() {
		PluginSearchInput input = new PluginSearchInput();

		if (object instanceof IPluginImport) {
			input.setSearchString(((IPluginImport) object).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (object instanceof IPluginExtension)  {
			input.setSearchString(((IPluginExtension)object).getPoint());
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
		} else if (object instanceof IPlugin) {
			input.setSearchString(((IPlugin)object).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (object instanceof IFragment) {
			input.setSearchString(((IFragment)object).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_FRAGMENT);
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_DECLARATIONS);
		input.setSearchScope(new PluginSearchScope());
		try {
			SearchUI.activateSearchResultView();
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
