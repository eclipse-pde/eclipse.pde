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

public class ConvertJavaToPDEProjectAction implements IObjectActionDelegate {
	public static final String KEY_CONVERTING = "ConvertProjectAction.converting";
	public static final String KEY_UPDATING = "ConvertProjectAction.updating";
	private IWorkbenchPart targetPart;
	private IProject project;

public void run(IAction action) {
	if (project == null)
		return;
	if (project.isOpen()==false) 
	    return;
	ProgressMonitorDialog dialog =
		new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				monitor.beginTask(PDEPlugin.getResourceString(KEY_CONVERTING), 2);
				monitor.worked(1);
				ConvertedProjectsPage.convertProject(project, monitor);
				monitor.subTask(PDEPlugin.getResourceString(KEY_UPDATING));
				monitor.worked(1);
				ConvertedProjectsPage.updateBuildPath(project, monitor);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} finally {
				monitor.done();
			}
		}
	};
	try {
		dialog.run(false, true, operation);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
	} catch (InterruptedException e) {
	}
}
public void selectionChanged(IAction action, ISelection selection) {
	boolean enable = false;
	if (selection instanceof IStructuredSelection) {
		Object object = ((IStructuredSelection) selection).getFirstElement();
		if (object instanceof IProject) {
			IProject project = (IProject) object;
			try {
				if (project.isOpen() && 
				      project.hasNature(PDEPlugin.PLUGIN_NATURE) == false) {
					this.project = project;
					enable = true;
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	action.setEnabled(enable);
}
public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	this.targetPart = targetPart;
}
}
