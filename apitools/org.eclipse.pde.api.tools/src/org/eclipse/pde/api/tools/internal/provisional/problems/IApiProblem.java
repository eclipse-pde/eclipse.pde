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
package org.eclipse.pde.api.tools.internal.provisional.problems;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Describes a given api problem.
 * 
 * @since 1.0.0
 */
public interface IApiProblem {

	/**
	 * Constant representing the binary incompatibility problem category 
	 */
	public static final int CATEGORY_BINARY = 0x01000000;
	
	/**
	 * Constant representing the api usage problem category
	 */
	public static final int CATEGORY_USAGE = 0x01000000 << 1;
	
	/**
	 * Constant representing the version problem category
	 */
	public static final int CATEGORY_VERSION = 0x01000000 << 2;
	
	/**
	 * Constant representing the since tag problem category
	 */
	public static final int CATEGORY_SINCETAGS = 0x01000000 << 3;
	
	/**
	 * Constant representing the offset of the flags portion of a problem id bit mask.
	 */
	public static final int OFFSET_FLAGS = 0;
	
	/**
	 * Constant representing the offset of the kinds portion of a problem id bit mask.
	 */
	public static final int OFFSET_KINDS = 8;
	
	/**
	 * Constant representing the offset of the element kinds portion of a problem id bit mask.
	 */
	public static final int OFFSET_ELEMENT = 16;
	
	/**
	 * Constant representing the value of having no flags.
	 * Value is: <code>0</code>
	 */
	public static final int NO_FLAGS = 0x0;

	/**
	 * Constant representing the value of the invalid @since tag {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int SINCE_TAG_INVALID = 1;

	/**
	 * Constant representing the value of the malformed @since tag {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int SINCE_TAG_MALFORMED = 2;

	/**
	 * Constant representing the value of the missing @since tag {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int SINCE_TAG_MISSING = 3;

	/**
	 * Constant representing the value of the major version change {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int MAJOR_VERSION_CHANGE = 1;

	/**
	 * Constant representing the value of the minor version change {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int MINOR_VERSION_CHANGE = 2;
	
	/**
	 * Constant representing the value of an illegal extend {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int ILLEGAL_EXTEND = 1;
	
	/**
	 * Constant representing the value of an illegal instantiate {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int ILLEGAL_INSTANTIATE = 2;
	
	/**
	 * Constant representing the value of an illegal reference {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int ILLEGAL_REFERENCE = 3;
	
	/**
	 * Constant representing the value of an illegal implement {@link IApiProblem} kind.
	 * 
	 * @see #getKind()
	 */
	public static final int ILLEGAL_IMPLEMENT = 4;
	
	/**
	 * Returns the severity of the problem. 
	 * See {@link IMarker} for a listing of severities.
	 * 
	 * @return the severity of the problem
	 */
	public int getSeverity();
	
	/**
	 * Returns the kind of element descriptor this problem is related to.
	 * 
	 * @see IElementDescriptor#getElementType()
	 * 
	 * @return the {@link IElementDescriptor} kind this problem is related to.
	 */
	public int getElementKind();
	
	/**
	 * Returns the handle to the underlying resource of the marker this problem 
	 * was created from.
	 * 
	 * @return the handle to the underlying resource
	 */
	public IResource getResource();
	
	/**
	 * Returns the category for this problem. Guaranteed to be
	 * one of:
	 * <ul>
	 * <li>{@link #CATEGORY_BINARY}</li>
	 * <li>{@link #CATEGORY_SINCETAGS}</li>
	 * <li>{@link #CATEGORY_USAGE}</li>
	 * <li>{@link #CATEGORY_VERSION}</li>
	 * </ul> 
	 * @return
	 */
	public int getCategory();
	
	/**
	 * Returns the unique id of the problem. A problem Id is the composition of
	 * the category, kind, flags and severity.
	 * 
	 * @return the id of the problem
	 */
	public int getId();
	
	/**
	 * Returns a human readable description of the problem
	 * 
	 * @return the description of the problem
	 */
	public String getMessage();
	
	/**
	 * Returns the kind of this problem.
	 * 
	 * @return the kind of this problem
	 */
	public int getKind();
	
	/**
	 * Returns the flags for this problem. 
	 * 
	 * @return the flags for this problem
	 */
	public int getFlags();
}
