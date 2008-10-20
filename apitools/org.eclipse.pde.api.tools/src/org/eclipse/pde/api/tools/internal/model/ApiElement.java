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
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;

/**
 * Abstract description of an API element.
 * Each {@link IApiElement} has a specific type, name and parent.
 * <br>
 * API elements cannot be re-parented.
 * 
 * @since 1.0.0
 */
public abstract class ApiElement implements IApiElement {

	private int fType = 0;
	private String fName = null;
	private IApiElement fParent = null;
	
	/**
	 * Constructor
	 * @param parent the parent type for this element, may be <code>null</code>
	 * @param type the type of this element. See {@link IApiElement} for values.
	 * @param name the simple name of the element
	 */
	protected ApiElement(IApiElement parent, int type, String name) {
		fParent = parent;
		fType = type;
		fName = name;
	}
	
   /**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getAncestor(int)
	 */
	public IApiElement getAncestor(int ancestorType) {
		if(fType == ancestorType) {
			return this;
		}
		IApiElement parent = fParent;
		while(parent != null && parent.getType() != ancestorType) {
			parent = parent.getParent();
		}
		return parent;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getDescriptor()
	 */
	public String getDescriptor() {
		return fName;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getName()
	 */
	public String getName() {
		return fName;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getParent()
	 */
	public IApiElement getParent() {
		return fParent;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getType()
	 */
	public int getType() {
		return fType;
	}

	/**
	 * Sets the name of this {@link ApiElement} to the new name, iff the new name
	 * is not <code>null</code>, otherwise no change is made.
	 * @param newname
	 */
	protected void setName(String newname) {
		if(newname != null) {
			fName = newname;
		}
	}
}
