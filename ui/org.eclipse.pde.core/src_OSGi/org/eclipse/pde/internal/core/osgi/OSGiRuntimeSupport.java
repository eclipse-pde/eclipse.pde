/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.IBundlePluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.osgi.bundle.BundlePluginModelBase;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class OSGiRuntimeSupport implements IAlternativeRuntimeSupport {
	IWorkspaceModelManager workspaceModelManager;
	IExternalModelManager externalModelManager;
	
	public OSGiRuntimeSupport() {
	}

	public IWorkspaceModelManager getWorkspaceModelManager() {
		if (workspaceModelManager==null)
			workspaceModelManager = new OSGiWorkspaceModelManager();
		return workspaceModelManager;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IAlternativeRuntimeSupport#getExternalModelManager()
	 */
	public IExternalModelManager getExternalModelManager() {
		if (externalModelManager==null)
			externalModelManager = new OSGiExternalModelManager();
		return externalModelManager;
	}
	
	public IPath getPluginLocation(IPluginModelBase model) {
		String location = model.getInstallLocation();
		IResource resource = model.getUnderlyingResource();
		if (resource != null && resource.isLinked()) {
			// special case - linked resource
			if (model instanceof IBundlePluginModelBase) {
				// OSGi bundle - remove two segments.
				// We must get rid of META-INF
				location =
					resource
						.getLocation()
						.removeLastSegments(2)
						.addTrailingSeparator()
						.toString();
			}
			else {
				location =
					resource
						.getLocation()
						.removeLastSegments(1)
						.addTrailingSeparator()
						.toString();
			}
		}
		return new Path(location).addTrailingSeparator();
	}
	
	public void shutdown() {
		if (externalModelManager!=null) {
			externalModelManager.shutdown();
			externalModelManager=null;
		}
		if (workspaceModelManager!=null) {
			workspaceModelManager.shutdown();
			workspaceModelManager = null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IAlternativeRuntimeSupport#getTransientSitePath(org.eclipse.pde.core.plugin.IPluginModelBase)
	 */
	public IPath getTransientSitePath(IPluginModelBase model) {
		boolean bundle=false;
		if (model instanceof BundlePluginModelBase) {
			bundle=true;
		}
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IPath realPath = resource.getLocation();
			return realPath.removeLastSegments(bundle?4:3);
		} else {
			// external
			//TODO we may need to do something for external
			// bundle paths here
			IPath path = new Path(model.getInstallLocation());
			return path.removeLastSegments(2);
		}
	}
	public boolean isRelevantJavaProject(IProject project) {
		return OSGiWorkspaceModelManager.isJavaPluginProject(project) ||
			OSGiWorkspaceModelManager.isJavaBundleProject(project);
	}
}
