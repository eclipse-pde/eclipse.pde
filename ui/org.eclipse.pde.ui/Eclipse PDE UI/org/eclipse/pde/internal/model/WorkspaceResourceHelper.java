package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IResourceChangeEvent;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.PDEPlugin;
public class WorkspaceResourceHelper extends NLResourceHelper
implements IResourceChangeListener {
	private IFile file;

	/**
	 * Constructor for WorkspaceResourceHelper
	 */
	public WorkspaceResourceHelper(String name, URL url) {
		super(name, url);
		PDEPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	public void dispose() {
		PDEPlugin.getWorkspace().removeResourceChangeListener(this);
	}
	
	public void setFile(IFile file) {
		this.file = file;
	}
	
	public IFile getFile() {
		return file;
	}
	/**
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
		   IResourceDelta delta = event.getDelta();
		   if (delta!=null) {
		      event.getDelta().accept(new IResourceDeltaVisitor() {
		         public boolean visit(IResourceDelta delta) {
				    return WorkspaceResourceHelper.this.visit(delta);
			     }
		      });
		   }
		}
		catch (CoreException e) {
		}
	}
	
	private boolean visit(IResourceDelta delta) {
		IResource resource = delta.getResource();
		if (resource.equals(file)) {
			fBundle = null;
		}
		return true;
	}
}
