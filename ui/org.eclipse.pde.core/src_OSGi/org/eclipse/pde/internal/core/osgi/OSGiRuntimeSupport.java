/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi;
import org.eclipse.pde.core.*;

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
	

}
