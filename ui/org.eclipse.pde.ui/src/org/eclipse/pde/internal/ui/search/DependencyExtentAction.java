package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.SearchUI;

public class DependencyExtentAction extends Action {
	
	private static final String KEY_COMPUTE = "DependencyExtentAction.label";
	
	DependencyExtentSearchOperation op;
	
	public DependencyExtentAction(IPluginImport object) {
		op = new DependencyExtentSearchOperation(object);
		setText(PDEPlugin.getResourceString(KEY_COMPUTE));
	}
		
	
	public void run() {
		try {
			SearchUI.activateSearchResultView();
			ProgressMonitorDialog pmd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}
	
	
	
}
