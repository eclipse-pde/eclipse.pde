package org.eclipse.pde.internal.wizards;

import org.eclipse.ui.part.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.navigator.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;


public class NewWizard extends Wizard implements INewWizard {
	private org.eclipse.ui.IWorkbench workbench;
	private org.eclipse.jface.viewers.IStructuredSelection selection;

public NewWizard() {
	super();
}
public org.eclipse.jface.viewers.IStructuredSelection getSelection() {
	return selection;
}
public IWorkbench getWorkbench() {
	return workbench;
}
public void init(IWorkbench workbench, IStructuredSelection selection) {
	this.workbench = workbench;
	this.selection = selection;
}
public boolean performFinish() {
	return true;
}
protected void revealSelection(final Object toSelect) {
	IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
	final IWorkbenchPart focusPart = ww.getActivePage().getActivePart();

	if (focusPart instanceof ISetSelectionTarget) {
		Display d = getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				ISelection selection = new StructuredSelection(toSelect);
				((ISetSelectionTarget) focusPart).selectReveal(selection);
			}
		});
	}
}
}
