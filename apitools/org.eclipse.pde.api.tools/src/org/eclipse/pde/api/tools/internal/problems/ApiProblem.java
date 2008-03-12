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

import org.eclipse.core.runtime.Path;
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
	 * The resource path for this problem
	 */
	private String fResourcePath = null;
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
	 * The listing of extra argument ids for the problem
	 */
	private String[] fExtraArgumentIds = null;
	/**
	 * The listing of corresponding arguments for the problem
	 */
	private Object[] fExtraArguments = null;
	/**
	 * The listing of arguments used to fill localized messages
	 */
	private String[] fMessageArguments = null;
	/**
	 * The line number the problem occurred on
	 */
	private int fLineNumber = -1;
	/**
	 * The start of a character selection range
	 */
	private int fCharStart = -1;
	/**
	 * The end of a character selection range
	 */
	private int fCharEnd = -1;
	
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
	 * @param messageargs arguments to be passed into a localized message for the problem
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the line this problem occurred on
	 * @param charstart the char selection start position
	 * @param charend the char selection end position
	 * @param severity the severity level of the problem
	 * @param category the category of the problem
	 * @param element the {@link IElementDescriptor} kind
	 * @param kind the kind of problem
	 * @param flags any additional flags for the problem kind
	 */
	public ApiProblem(String path, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int category, int element, int kind, int flags) {
		this(path, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, 
				category | (element << IApiProblem.OFFSET_ELEMENT) | (kind << IApiProblem.OFFSET_KINDS) | (flags << IApiProblem.OFFSET_FLAGS));
	}
	
	/**
	 * Constructor
	 * @param resource the resource this problem occurs on / in
	 * @param messageargs arguments to be passed into a localized message for the problem
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the line this problem occurred on
	 * @param charstart the char selection start position
	 * @param charend the char selection end position
	 * @param severity the severity level of the problem
	 * @param id the id of the problem
	 */
	public ApiProblem(String path, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int id) {
		this.fResourcePath = path;
		this.fSeverity = severity;
		this.fId = id;
		this.fExtraArgumentIds = argumentids;
		this.fExtraArguments = arguments;
		this.fLineNumber = linenumber;
		this.fCharStart = charstart;
		this.fCharEnd = charend;
		this.fMessageArguments = messageargs;
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
		if(fMessage == null) {
			fMessage = ApiProblemFactory.getLocalizedMessage(this);
		}
		return fMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getResourcePath()
	 */
	public String getResourcePath() {
		return fResourcePath;
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
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getLineNumber()
	 */
	public int getLineNumber() {
		return fLineNumber;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getCharStart()
	 */
	public int getCharStart() {
		return fCharStart;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getCharEnd()
	 */
	public int getCharEnd() {
		return fCharEnd;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getExtraMarkerAttributeNames()
	 */
	public String[] getExtraMarkerAttributeIds() {
		if(fExtraArguments == null || fExtraArgumentIds == null) {
			return new String[0];
		}
		if(fExtraArgumentIds.length != fExtraArguments.length) {
			return new String[0];
		}
		return fExtraArgumentIds;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getExtraMarkerAttributeValues()
	 */
	public Object[] getExtraMarkerAttributeValues() {
		if(fExtraArguments == null || fExtraArgumentIds == null) {
			return new String[0];
		}
		if(fExtraArgumentIds.length != fExtraArguments.length) {
			return new String[0];
		}
		return fExtraArguments;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getMessageArguments()
	 */
	public String[] getMessageArguments() {
		if(fMessageArguments == null) {
			return new String[0];
		}
		return fMessageArguments;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiProblem) {
			IApiProblem problem = (IApiProblem) obj;
			return problem.getId() == fId &&
					new Path(problem.getResourcePath()).equals(new Path(fResourcePath));
		}
		return super.equals(obj);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Api problem: "); //$NON-NLS-1$
		buffer.append(fResourcePath);
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
		return getId() + fLineNumber + fCharStart + fCharEnd + fResourcePath.hashCode();
	}
}
