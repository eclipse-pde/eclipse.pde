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
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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
	 * The qualified type name for this problem
	 */
	private String fTypeName = null;
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
	public static final int CATEGORY_MASK = 0xF0000000;
	public static final int ELEMENT_KIND_MASK = 0x0F000000;
	public static final int KIND_MASK = 0x00F00000;
	public static final int FLAGS_MASK = 0x000FF000;
	public static final int MESSAGE_MASK = 0x00000FFF;
	
	/**
	 * Constant representing the name of the 'compatibilityStatus' attribute on API problem
	 * for the compatibility category.
	 * Value is: <code>compatibilityStatus</code>
	 */
	public static final String COMPATIBILITY_STATUS = "compatibilityStatus"; //$NON-NLS-1$

	/**
	 * Constructor
	 * @param resource the resource this problem occurs on / in
	 * @param typeName the qualified type name this problem occurs on / in
	 * @param messageargs arguments to be passed into a localized message for the problem
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the line this problem occurred on
	 * @param charstart the char selection start position
	 * @param charend the char selection end position
	 * @param severity the severity level of the problem
	 * @param id the id of the problem
	 */
	public ApiProblem(String path, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int id) {
		this.fResourcePath = path;
		this.fTypeName = typeName;
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
	 * @see org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem#getMessageid()
	 */
	public int getMessageid() {
		return (fId & MESSAGE_MASK) >> OFFSET_MESSAGE;
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
		if(ApiPlugin.isRunningInFramework()) {
			return ApiPlugin.getDefault().getSeverityLevel(ApiProblemFactory.getProblemSeverityId(this), null);
		}
		return ApiPlugin.SEVERITY_WARNING;
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
	
	public boolean equals(Object obj) {
		if(obj instanceof IApiProblem) {
			IApiProblem problem = (IApiProblem) obj;
			if (problem.getId() == fId && argumentsEqual(problem.getMessageArguments())) {
				String resourcePath = problem.getResourcePath();
				if (resourcePath == null) {
					if (this.fResourcePath != null) {
						return false;
					}
				} else if (this.fResourcePath == null) {
					return false;
				} else if (!new Path(resourcePath).equals(new Path(fResourcePath))) {
					return false;
				}
				String typeName = problem.getTypeName();
				if (typeName == null) {
					if (this.fTypeName != null) {
						return false;
					}
					return true;
				} else if (this.fTypeName == null) {
					return false;
				}
				return typeName.equals(this.fTypeName);
			}
			return false;
		}
		return super.equals(obj);
	}
	/**
	 * COmpares the complete list of message arguments
	 * @param arguments
	 * @return true if all of the arguments are equal, false otherwise
	 */
	private boolean argumentsEqual(String[] arguments) {
		String[] currentArguments = getMessageArguments();
		if((currentArguments == null && arguments != null) ||
			(currentArguments != null && arguments == null)) {
			return false;
		}
		boolean equal = true;
		if(currentArguments.length != arguments.length) {
			return false;
		}
		for(int i = 0; i < currentArguments.length; i++) {
			equal &= currentArguments[i].equals(arguments[i]);
		}
		return equal;
	}
	
	/**
	 * Returns the deep hash code of the complete listing of message arguments
	 * @param arguments
	 * @return the hash code of the message arguments
	 */
	private int argumentsHashcode(String[] arguments) {
		if(fMessageArguments == null) {
			return 0;
		}
		int hashcode = 0;
		for(int i = 0; i < fMessageArguments.length; i++) {
			hashcode += fMessageArguments[i].hashCode();
		}
		return hashcode;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Api problem: "); //$NON-NLS-1$
		buffer.append(fResourcePath).append("\n"); //$NON-NLS-1$
		buffer.append("\tseverity: "); //$NON-NLS-1$
		buffer.append(Util.getSeverity(getSeverity())).append("\n"); //$NON-NLS-1$
		buffer.append("\tcategory: "); //$NON-NLS-1$
		buffer.append(Util.getProblemCategory(getCategory())).append("\n"); //$NON-NLS-1$
		buffer.append("\telement kind: "); //$NON-NLS-1$
		buffer.append(Util.getProblemElementKind(getCategory(), getElementKind())).append("\n"); //$NON-NLS-1$
		buffer.append("\tkind: "); //$NON-NLS-1$
		buffer.append(Util.getProblemKind(getCategory(), getKind())).append("\n"); //$NON-NLS-1$
		buffer.append("\tflags: "); //$NON-NLS-1$
		buffer.append(Util.getProblemFlagsName(getCategory(), getFlags())).append("\n"); //$NON-NLS-1$
		buffer.append("\tmessage id: "); //$NON-NLS-1$
		buffer.append(getMessageid());
		return buffer.toString();
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getId()
			+ (fResourcePath != null ? fResourcePath.hashCode() : 0)
			+ argumentsHashcode(fMessageArguments)
			+ (fTypeName != null ? fTypeName.hashCode() : 0);
	}

	public String getTypeName() {
		return this.fTypeName;
	}
}
