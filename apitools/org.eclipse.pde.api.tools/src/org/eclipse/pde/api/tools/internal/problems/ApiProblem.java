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
package org.eclipse.pde.api.tools.internal.problems;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * A description of an api problem
 * 
 * @since 1.0.0
 */
public class ApiProblem implements IApiProblem {

	/**
	 * Human readable message for the problem
	 * TODO should not be passed in  by user, should be derived (lazily loaded)
	 */
	private String fMessage = null;
	/**
	 * The backing resource handle for this problem
	 */
	private IResource fResource = null;
	/**
	 * the severity of the problem
	 * TODO needs to be derived from the associated preference (lazily loaded)
	 */
	private int fSeverity = 0;
	/**
	 * The composite id of the problem. Contains the category, 
	 * element kind, kind, and flags for a specific problem
	 */
	private int fId = 0;
	
	/**
	 * Masks to get the original bits out of the id
	 */
	private static final int CATEGORY_MASK = 0xFF000000;
	private static final int ELEMENT_KIND_MASK = 0x00FF0000;
	private static final int KIND_MASK = 0x0000FF00;
	private static final int FLAGS_MASK = 0x000000FF;
	
	/**
	 * Constructor
	 * @param resource the resource this problem occurs on / in
	 * @param message the human readable message for the problem
	 * @param severity the severity level of the problem
	 * @param category the category of the problem
	 * @param element the {@link IElementDescriptor} kind
	 * @param kind the kind of problem
	 * @param flags any additional flags for the problem kind
	 */
	public ApiProblem(IResource resource, String message, int severity, int category, int element, int kind, int flags) {
		this.fResource = resource;
		this.fMessage = message;
		this.fSeverity = severity;
		this.fId = category | (element << IApiProblem.OFFSET_ELEMENT) | (kind << IApiProblem.OFFSET_KINDS) | (flags << IApiProblem.OFFSET_FLAGS);
	}
	
	/**
	 * Constructor
	 * @param resource the resource this problem occurs on / in
	 * @param message the human readable message for the problem
	 * @param severity the severity level of the problem
	 * @param id the id of the problem
	 */
	public ApiProblem(IResource resource, String message, int severity, int id) {
		this.fResource = resource;
		this.fMessage = message;
		this.fSeverity = severity;
		this.fId = id;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getId()
	 */
	public int getId() {
		return fId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getCategory()
	 */
	public int getCategory() {
		return (fId & CATEGORY_MASK);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getFlags()
	 */
	public int getFlags() {
		return (fId & FLAGS_MASK) >> OFFSET_FLAGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getKind()
	 */
	public int getKind() {
		return (fId & KIND_MASK) >> OFFSET_KINDS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getMessage()
	 */
	public String getMessage() {
		return fMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getResource()
	 */
	public IResource getResource() {
		return fResource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getSeverity()
	 */
	public int getSeverity() {
		return fSeverity;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getElementKind()
	 */
	public int getElementKind() {
		return (fId & ELEMENT_KIND_MASK) >> OFFSET_ELEMENT;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiProblem) {
			IApiProblem problem = (IApiProblem) obj;
			return problem.getId() == getId() && 
					problem.getResource().equals(fResource);
		}
		return super.equals(obj);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Api problem: "); //$NON-NLS-1$
		buffer.append(fResource.getFullPath());
		buffer.append("[severity: "); //$NON-NLS-1$
		buffer.append(Util.getSeverity(fSeverity));
		buffer.append(" category: "); //$NON-NLS-1$
		buffer.append(Util.getProblemCategory(getCategory()));
		buffer.append(" element kind: "); //$NON-NLS-1$
		buffer.append(Util.getProblemElementKind(getCategory(), getElementKind()));
		buffer.append(" kind: "); //$NON-NLS-1$
		buffer.append(Util.getProblemKind(getCategory(), getKind()));
		buffer.append(" flags: "); //$NON-NLS-1$
		buffer.append(Util.getDeltaFlagsName(getFlags()));
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getId();
	}
}
