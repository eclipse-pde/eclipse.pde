package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.pde.internal.PDE;

public class ConvertJavaToPDEProjectAction implements IObjectActionDelegate {
	public static final String KEY_CONVERTING = "ConvertProjectAction.converting";
	public static final String KEY_UPDATING = "ConvertProjectAction.updating";
	private IWorkbenchPart targetPart;
	private Vector selected = new Vector();

	public void run(IAction action) {
		ConvertedProjectWizard wizard = new ConvertedProjectWizard(selected);
		wizard.init(PlatformUI.getWorkbench(), null);
		WizardDialog wdialog =
			new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		wdialog.open();
	}

	public void selectionChanged(IAction action, ISelection sel) {
		selected.clear();
		boolean enable = true;
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				Object object = iter.next();
				if (object instanceof IJavaProject)
					object = ((IJavaProject) object).getProject();
				if (object instanceof IProject) {
					IProject project = (IProject) object;
					if (!project.isOpen() || !PDE.hasPluginNature(project)) {
						enable = false;
						break;
					} else {
						selected.add(project);
					}
				} else {
					enable = false;
					break;
				}
			}
			action.setEnabled(enable);
		}
	}
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
}