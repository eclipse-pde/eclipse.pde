/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Base implementation of {@link ILocation}
 * 
 * @since 1.0.0
 */
public class Location implements ILocation {

	private IApiComponent fComponent = null;
	/**
	 * Associated element
	 */
	private IMemberDescriptor fElement;
	
	private int linenumber = -1;
	
	/**
	 * Constructor
	 * @param component component the location is contained in, or <code>null</code>
	 * @param element element associated with location
	 */
	public Location(IApiComponent component, IMemberDescriptor element) {
		fComponent = component;
		fElement = element;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof Location) {
			Location loc = (Location) obj;
			if (loc.getMember().equals(getMember()) && loc.getLineNumber() == getLineNumber()) {
				return Util.equalsOrNull(getApiComponent(), loc.getApiComponent());
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.ILocation#getApiComponent()
	 */
	public IApiComponent getApiComponent() {
		return fComponent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.ILocation#getLineNumber()
	 */
	public int getLineNumber() {
		return linenumber;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.ILocation#getElement()
	 */
	public IMemberDescriptor getMember() {
		return fElement;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.ILocation#getType()
	 */
	public IReferenceTypeDescriptor getType() {
		IMemberDescriptor member = getMember();
		if (member instanceof IReferenceTypeDescriptor) {
			return (IReferenceTypeDescriptor) member;
		} else {
			return member.getEnclosingType();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fElement.hashCode() + linenumber;
	}

	/**
	 * Allows the component id to be set. Used via {@link Reference} when resolving a target location
	 * @param component the new component id
	 */
	public void setApiComponent(IApiComponent component) {
		fComponent = component;
	}

	/* (non-Javadoc)
	 * @see ILocation#setLineNumber(int) 
	 */
	public void setLineNumber(int value) {
		linenumber = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("location ["); //$NON-NLS-1$
		buffer.append(getMember().toString());
		buffer.append(" from: "); //$NON-NLS-1$
		if(fComponent == null) {
			buffer.append(" <unresolved component>"); //$NON-NLS-1$
		}
		else {
			buffer.append(fComponent);
		}
		buffer.append(" line: "+linenumber); //$NON-NLS-1$
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}
	
}
