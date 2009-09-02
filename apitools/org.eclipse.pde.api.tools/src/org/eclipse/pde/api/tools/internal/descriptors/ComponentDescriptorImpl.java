/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Base implementation of {@link IComponentDescriptor}
 * 
 * @since 1.0.1
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ComponentDescriptorImpl extends NamedElementDescriptorImpl implements IComponentDescriptor {

	private String componentid = null;
	private String version = null;
	
	/**
	 * Constructor
	 * @param componentid
	 */
	public ComponentDescriptorImpl(String componentid, String version) {
		super(componentid);
		this.componentid = componentid;
		this.version = version;
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor#getElementType()
	 */
	public int getElementType() {
		return COMPONENT;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int hc = 0;
		if (version != null) {
			hc = version.hashCode();
		}
		return this.componentid.hashCode() + hc;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IComponentDescriptor) {
			if (this.componentid.equals(((IComponentDescriptor)obj).getId())) {
				if (this.version == null) {
					return ((IComponentDescriptor)obj).getVersion() == null;
				} else {
					return this.version.equals(((IComponentDescriptor)obj).getVersion());
				}
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor#getId()
	 */
	public String getId() {
		return this.componentid;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor#getPath()
	 */
	public IElementDescriptor[] getPath() {
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.componentid;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor#getVersion()
	 */
	public String getVersion() {
		return version;
	}
	
}
