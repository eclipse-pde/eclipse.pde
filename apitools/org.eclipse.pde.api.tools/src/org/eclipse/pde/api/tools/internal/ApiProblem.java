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

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * The description of an api problem
 * 
 * @since 1.0.0
 */
public class ApiProblem implements IApiProblem {

	private int fCategory = 0;
	private int fKind = 0;
	private int fFlags = 0;
	private String fMessage = null;
	private IResource fResource = null;
	private int fSeverity = 0;
	
	/**
	 * Constructor
	 * @param resource the resource this problem occurs on / in
	 * @param message the human readable message for the problem
	 * @param severity the severity level of the problem
	 * @param category the category of the problem
	 * @param kind the kind of problem
	 * @param flags any additional flags for the problem kind
	 */
	public ApiProblem(IResource resource, String message, int severity, int category, int kind, int flags) {
		this.fResource = resource;
		this.fMessage = message;
		this.fSeverity = severity;
		this.fCategory = category;
		this.fKind = kind;
		this.fFlags = flags;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getId()
	 */
	public int getId() {
		//TODO needs to include element
		return fCategory | fKind | fFlags | fSeverity;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getCategory()
	 */
	public int getCategory() {
		return fCategory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getFlags()
	 */
	public int getFlags() {
		return fFlags;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProblem#getKind()
	 */
	public int getKind() {
		return fKind;
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
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Api problem: "); //$NON-NLS-1$
		buffer.append(fResource.getFullPath());
		buffer.append("[severity: "); //$NON-NLS-1$
		buffer.append(Util.getSeverity(fSeverity));
		buffer.append(" category: "); //$NON-NLS-1$
		buffer.append(Util.getProblemCategory(fCategory));
		buffer.append(" kind: "); //$NON-NLS-1$
		buffer.append(fKind);
		buffer.append(" flags: "); //$NON-NLS-1$
		buffer.append(Util.getDeltaFlagsName(fFlags));
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiProblem) {
			IApiProblem problem = (IApiProblem) obj;
			return problem.getId() == getId() && 
					problem.getSeverity() == fSeverity &&
					problem.getResource().equals(fResource);
		}
		return super.equals(obj);
	}
}
