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
package org.eclipse.pde.api.tools.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Base implementation of {@link IApiProblemFilter}
 * 
 * @since 1.0.0
 */
public class ApiProblemFilter implements IApiProblemFilter, Comparable, Cloneable {

	
	
	private String fComponentId = null;
	private HashSet fKinds = null;
	private IElementDescriptor fElement = null;
	
	/**
	 * Constructor
	 * @param componentid
	 * @param element
	 * @param kind
	 */
	public ApiProblemFilter(String componentid, IElementDescriptor element, String[] kinds) {
		fComponentId = componentid;
		fElement = element;
		fKinds = new HashSet();
		if(kinds != null) {
			fKinds.addAll(Arrays.asList(kinds));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProblemFilter#getComponentId()
	 */
	public String getComponentId() {
		return fComponentId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProblemFilter#getElement()
	 */
	public IElementDescriptor getElement() {
		return fElement;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProblemFilter#getKind()
	 */
	public String[] getKinds() {
		String[] array = (String[]) fKinds.toArray(new String[fKinds.size()]);
		Arrays.sort(array);
		return array;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProblemFilter#addKind(java.lang.String)
	 */
	public void addKind(String kind) {
		if(kind != null) {
			fKinds.add(kind);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProblemFilter#removeKind(java.lang.String)
	 */
	public boolean removeKind(String kind) {
		return fKinds.remove(kind);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiProblemFilter) {
			IApiProblemFilter filter = (IApiProblemFilter) obj;
			boolean samekinds = false;
			if(fKinds.size() == filter.getKinds().length) {
				samekinds = true;
				String[] kinds = filter.getKinds();
				int idx = 0;
				for(Iterator iter = fKinds.iterator(); iter.hasNext();) {
					samekinds &= elementsEqual(iter.next(), kinds[idx++]);
				}
			}
			return samekinds && elementsEqual(this.fComponentId, filter.getComponentId()) &&
					elementsEqual(this.fElement, filter.getElement());
		}
		else if(obj instanceof IElementDescriptor) {
			return this.fElement.equals(obj);
		}
		return super.equals(obj);
	}
	
	/**
	 * Returns if the two specified objects are equal.
	 * Objects are considered equal if:
	 * <ol>
	 * <li>they are both null</li>
	 * <li>they are equal via the default .equals() method</li>
	 * </ol>
	 * @param s1
	 * @param s2
	 * @return true if the objects are equal, false otherwise
	 */
	private boolean elementsEqual(Object s1, Object s2) {
		return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Api Problem Filter: type ["+fElement.getElementType()+"] for ["+fElement.toString()+"]: kinds "+fKinds.toString()+""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if(o instanceof IApiProblemFilter) {
			IApiProblemFilter filter = (IApiProblemFilter) o;
			return ((ElementDescriptorImpl)filter.getElement()).compareTo(fElement);
		}
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return new ApiProblemFilter(this.fComponentId, this.fElement, this.getKinds());
	}
}
