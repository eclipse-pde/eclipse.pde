package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.util.Util;

public class UpdateBundleVersionOperation {
	IMarker fMarker;
	String version;

	public UpdateBundleVersionOperation(IMarker marker, String version) {
		this.fMarker = marker;
		this.version = version;
	}
	public IStatus run(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) return Status.CANCEL_STATUS;
		if (monitor != null) {
			monitor.beginTask(MarkerMessages.UpdateVersionNumberingOperation_title, 2);
		}
		// retrieve the AST node compilation unit
		try {
			if (monitor != null) {
				monitor.worked(1);
			}
			IResource resource = this.fMarker.getResource();
			IProject project = resource.getProject();
			if (!project.exists()) {
				System.err.println("Project " + project.getName() + " doesn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
				return Status.OK_STATUS;
			}
//			IApiProfile workspaceProfile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
//			IApiComponent apiComponent = workspaceProfile.getApiComponent(project.getName());
//			if (apiComponent instanceof PluginProjectApiComponent) {
//				PluginProjectApiComponent component = (PluginProjectApiComponent) apiComponent;
//			}
			Util.getBuildJob(project).schedule();
			if (monitor != null) {
				monitor.worked(1);
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		return Status.OK_STATUS;
	}
}
