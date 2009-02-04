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
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;

/**
 * Default implementation of {@link IApiSearchScope}
 * 
 * @since 1.0.0
 */
public class ApiUseSearchScope implements IApiSearchScope {

	/**
	 * The raw list of elements in this scope
	 */
	private List fElements = null;
	
	/**
	 * Constructor
	 * @param elements
	 */
	public ApiUseSearchScope(IApiElement[] elements) {
		fElements = new ArrayList(elements.length);
		for(int i = 0; i < elements.length; i++) {
			fElements.add(elements[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope#getScope()
	 */
	public IApiElement[] getScope() {
		return (IApiElement[]) fElements.toArray(new IApiComponent[fElements.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope#encloses(org.eclipse.pde.api.tools.internal.provisional.model.IApiElement)
	 */
	public boolean encloses(IApiElement element) {
		if(element != null) {
			IApiComponent component = element.getApiComponent();
			IApiComponent enclosing = null;
			for(Iterator iter = fElements.iterator(); iter.hasNext();) {
				enclosing = ((IApiElement)iter.next()).getApiComponent();
				if(component.equals(enclosing)) {
					return true;
				}
			}
		}
		return false;
	}

}
