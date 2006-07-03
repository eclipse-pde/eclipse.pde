package org.eclipse.pde.internal.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class FormatManifestAction extends Action {

	private IEditorInput fInput;
	
	public FormatManifestAction() {
		setText(PDEUIMessages.FormatManifestAction_actionText);
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
		if (fInput == null)
			return;
		
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
					new FormatManifestOperation(new Object[] {fInput}));
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
			PDEPlugin.log(e);
		}
	}

	public void setSourceInput(IEditorInput input) {
		fInput = input;
	}
}
