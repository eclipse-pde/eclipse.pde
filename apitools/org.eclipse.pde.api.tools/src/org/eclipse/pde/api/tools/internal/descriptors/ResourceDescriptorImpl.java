/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.descriptors;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IResourceDescriptor;

/**
 * An element descriptor that can describe an {@link IResource}
 * 
 * @since 0.1.0
 */
public class ResourceDescriptorImpl extends NamedElementDescriptorImpl implements IResourceDescriptor {

	/**
	 * The backing path for this {@link IElementDescriptor}
	 */
	/*private IPath fPath = null;*/
	
	private IResource fResourceHandle = null;
	
	/**
	 * Constructor
	 * @param name
	 */
	public ResourceDescriptorImpl(IResource resource) {
		super(resource.getName());
		fResourceHandle = resource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl#getPath()
	 */
	public IElementDescriptor[] getPath() {
		ArrayList paths = new ArrayList();
		IProject project = fResourceHandle.getProject();
		if(project != null) {
			IPath path = fResourceHandle.getProjectRelativePath();
			IResource res = null;
			for(int i = 1; i < path.segmentCount()+1; i++) {
				res = project.findMember(path.uptoSegment(i));
				if(res == null) {
					//if any part of the path no longer exists, clear partial path collection and stop processing
					paths.clear();
					break;
				}
				paths.add(new ResourceDescriptorImpl(res));
			}
		}
		return (IElementDescriptor[]) paths.toArray(new IElementDescriptor[paths.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl#getParent()
	 */
	public IElementDescriptor getParent() {
		return new ResourceDescriptorImpl(fResourceHandle.getParent());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor#getElementType()
	 */
	public int getElementType() {
		return T_RESOURCE;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IResourceDescriptor#getResourceType()
	 */
	public int getResourceType() {
		return fResourceHandle.getType();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getName().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IResourceDescriptor) {
			return ((IResourceDescriptor)obj).getName().equals(getName());
		}
		return super.equals(obj);
	}
}
