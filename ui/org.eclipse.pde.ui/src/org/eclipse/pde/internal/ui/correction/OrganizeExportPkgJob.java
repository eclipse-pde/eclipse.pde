package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.text.edits.MalformedTreeException;

public class OrganizeExportPkgJob extends WorkspaceJob {
	
	private IProject fProject;

	public OrganizeExportPkgJob(String name, IProject proj) {
		super(name);
		fProject = proj;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		try {
			ExportPackageUtil.organizeExportPackages(fProject);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.OK, PDEUIMessages.OrganizeExportPkgJob_ok, null);
		} catch (CoreException e) {
			PDEPlugin.log(e);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), null);
		} catch (MalformedTreeException e) {
			PDEPlugin.log(e);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), null);
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), null);
		}
	}

}
