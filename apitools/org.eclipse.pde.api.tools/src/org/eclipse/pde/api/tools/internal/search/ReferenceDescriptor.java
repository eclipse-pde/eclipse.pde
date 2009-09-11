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

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;

/**
 * Implementation of a reference descriptor
 */
public class ReferenceDescriptor implements IReferenceDescriptor {
	
	private IMemberDescriptor origin;
	private IMemberDescriptor target;
	private IComponentDescriptor from;
	private IComponentDescriptor to;
	
	private int line;
	private int kind;
	private int visibility;
	
	public ReferenceDescriptor(IComponentDescriptor from, IMemberDescriptor origin, int line, IComponentDescriptor to, IMemberDescriptor target, int kind, int vis) {
		this.origin = origin;
		this.target = target;
		this.from = from;
		this.to = to;
		this.line = line;
		this.kind = kind;
		this.visibility = vis;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ReferenceDescriptor) {
			ReferenceDescriptor rd = (ReferenceDescriptor) obj;
			return origin.equals(rd.origin) &&
			target.equals(rd.target) &&
			from.equals(rd.from) &&
			to.equals(rd.to) &&
			line == rd.line && kind == rd.kind && visibility == rd.visibility;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return origin.hashCode() + target.hashCode() + from.hashCode() + to.hashCode() + line + kind + visibility;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getComponent()
	 */
	public IComponentDescriptor getComponent() {
		return from;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getLineNumber()
	 */
	public int getLineNumber() {
		return line;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getMember()
	 */
	public IMemberDescriptor getMember() {
		return origin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getReferenceKind()
	 */
	public int getReferenceKind() {
		return kind;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getReferenceType()
	 */
	public int getReferenceType() {
		switch (getReferencedMember().getElementType()) {
		case IElementDescriptor.TYPE:
			return IReference.T_TYPE_REFERENCE;
		case IElementDescriptor.METHOD:
			return IReference.T_METHOD_REFERENCE;
		case IElementDescriptor.FIELD:
			return IReference.T_FIELD_REFERENCE;
		default:
			return -1;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getReferencedComponent()
	 */
	public IComponentDescriptor getReferencedComponent() {
		return to;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getReferencedMember()
	 */
	public IMemberDescriptor getReferencedMember() {
		return target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor#getVisibility()
	 */
	public int getVisibility() {
		return visibility;
	}

}
