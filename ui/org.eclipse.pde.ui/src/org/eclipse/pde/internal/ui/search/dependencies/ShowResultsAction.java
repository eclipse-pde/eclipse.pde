package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;

public class ShowResultsAction extends Action {
	
	IPluginImport[] fUnusedImports;

	public ShowResultsAction(IPluginImport[] unused) {
		fUnusedImports = unused;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fUnusedImports.length == 0) {
			MessageDialog.openInformation(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString("UnusedDependencies.title"), //$NON-NLS-1$
				PDEPlugin.getResourceString("UnusedDependencies.notFound")); //$NON-NLS-1$
		} else {
			UnusedImportsDialog dialog =
				new UnusedImportsDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					(IPluginModelBase)fUnusedImports[0].getModel(),
					fUnusedImports);
			dialog.create();
			dialog.getShell().setText(
				PDEPlugin.getResourceString("UnusedDependencies.title")); //$NON-NLS-1$
			dialog.open();
		} 
	}
}


