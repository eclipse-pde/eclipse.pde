package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.correction.OrganizeManifestJob;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.progress.IProgressConstants;

public class OrganizeManifestAction implements IWorkbenchWindowActionDelegate {
	
	private ISelection fSelection;

	public OrganizeManifestAction() {
		super();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		IProject proj = null;
		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			Object element = ssel.getFirstElement();
			if (element instanceof IFile)
				proj = ((IFile)element).getProject();
			else if (element instanceof IProject)
				proj = (IProject) element;
			
			if (proj != null) {
				scheduleJobs(proj);
			}
		}
	}
	
	protected void scheduleJobs(IProject proj) {
		Job job = new OrganizeManifestJob(PDEUIMessages.OrganizeManifestAction_name, proj);
		job.setUser(true);
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PSEARCH_OBJ.createImage());
		job.schedule();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

}
