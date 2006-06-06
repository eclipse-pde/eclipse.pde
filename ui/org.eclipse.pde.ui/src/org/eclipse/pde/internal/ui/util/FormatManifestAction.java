package org.eclipse.pde.internal.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class FormatManifestAction extends Action implements IObjectActionDelegate, IWorkbenchWindowActionDelegate {

	private IStructuredSelection fSelection;
	
	public FormatManifestAction() {
		setText(PDEUIMessages.FormatManifestAction_actionText);
		setActionDefinitionId("org.eclipse.pde.ui.manifestEditor.FormatManifest"); //$NON-NLS-1$
		setEnabled(true);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		targetPart.toString();
	}

	public void runWithEvent(Event event) {
		run(this);
	}
	
	public void run() {
		run(this);
	}
	
	public void run(IAction action) {
		if (fSelection == null)
			return;
		
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
					new FormatManifestOperation(fSelection.toArray()));
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
			PDEPlugin.log(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (!selection.isEmpty() && selection instanceof IStructuredSelection)
			fSelection = (IStructuredSelection)selection;
		else
			fSelection = null;
	}

	public void dispose() {
		
	}

	public void init(IWorkbenchWindow window) {
		
	}
	
}
