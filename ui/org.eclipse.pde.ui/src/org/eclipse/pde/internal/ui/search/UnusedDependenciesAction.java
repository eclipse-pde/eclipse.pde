package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class UnusedDependenciesAction extends Action {
	
	private IPluginModelBase model;

	public UnusedDependenciesAction(IPluginModelBase model) {
		this.model = model;
		setText(PDEPlugin.getResourceString("UnusedDependencies.action"));
	}
	
	public void run() {
		try {
			UnusedDependenciesOperation op = new UnusedDependenciesOperation(model);
			ProgressMonitorDialog pmd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			pmd.run(true, true, op);
			IPluginImport[] unused = op.getUnusedDependencies();
			if (unused.length == 0)
				MessageDialog.openInformation(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString("UnusedDependencies.title"),
					PDEPlugin.getResourceString("UnusedDependencies.notFound"));
			else if (model.isEditable()) {
				UnusedImportsDialog dialog =
					new UnusedImportsDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						model,
						unused);
				dialog.create();
				dialog.getShell().setText(
					PDEPlugin.getResourceString("UnusedDependencies.title"));
				dialog.open();
			} else {
				String lineSeparator = System.getProperty("line.separator");
				StringBuffer buffer =
					new StringBuffer(
						PDEPlugin.getResourceString("UnusedDependencies.found"));
				for (int i = 0; i < unused.length; i++) {
					buffer.append(lineSeparator + unused[i].getId());
				}
				MessageDialog.openInformation(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString("UnusedDependencies.title"),
					buffer.toString());
			}
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}
	
}
