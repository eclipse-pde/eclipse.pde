package org.eclipse.pde.internal.ui.build;

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

public class BuildSiteJob extends FeatureExportJob {
	
	private IProject fSiteProject;

	public BuildSiteJob(IFeatureModel[] models, IProject project, ISiteBuildModel buildModel) {
		super(EXPORT_AS_UPDATE_JARS, 
				false, 
				project.getLocation().toOSString(),
				null,  
				models);
		fSiteProject = project;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		touchSite(monitor);
		IStatus status = super.run(monitor);
		refresh(monitor);
		return status;
	}
	
	private void touchSite(IProgressMonitor monitor) {
		File file = new File(fSiteProject.getLocation().toOSString(), "site.xml");
		file.setLastModified(System.currentTimeMillis());
	}
	
	private void refresh(IProgressMonitor monitor) {
		try {
			fSiteProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (CoreException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getLogFoundMessage()
	 */
	protected String getLogFoundMessage() {
		return "Compilation errors occurred during the build.  Logs can be found in 'logs.zip' at the root of the site project.";
	}
}
