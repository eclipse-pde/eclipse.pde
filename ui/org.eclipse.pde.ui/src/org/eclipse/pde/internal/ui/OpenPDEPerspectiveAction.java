package org.eclipse.pde.internal.ui;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.*;

public class OpenPDEPerspectiveAction extends Action {
	public OpenPDEPerspectiveAction() {
	}

	public void run() {
		IWorkbenchWindow window = PDEPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IAdaptable input;
		if (page != null)
			input = page.getInput();
		else
			input = ResourcesPlugin.getWorkspace().getRoot();
		try {
			PlatformUI.getWorkbench().showPerspective(
				"org.eclipse.pde.ui.PDEPerspective",
				window,
				input);
		} catch (WorkbenchException e) {
			PDEPlugin.logException(e);
		}
	}
}
