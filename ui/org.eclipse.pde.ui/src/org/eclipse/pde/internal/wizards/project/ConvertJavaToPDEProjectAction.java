package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.WizardDialog;
import java.util.Iterator;
import java.util.Vector;

public class ConvertJavaToPDEProjectAction implements IObjectActionDelegate {
	public static final String KEY_CONVERTING = "ConvertProjectAction.converting";
	public static final String KEY_UPDATING = "ConvertProjectAction.updating";
	private IWorkbenchPart targetPart;
	private Vector selected=new Vector();

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
					try {
						if (!project.isOpen() || project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
							enable = false;
							break;
						}
						else {
							selected.add(project);
						}
					} catch (CoreException e) {
						PDEPlugin.logException(e);
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