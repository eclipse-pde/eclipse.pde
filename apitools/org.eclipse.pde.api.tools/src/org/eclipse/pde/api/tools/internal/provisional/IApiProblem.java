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
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

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
	 * Constant representing the value of having no flags.
	 * Value is: <code>0</code>
	 */
	public static final int NO_FLAGS = 0x0;

	/**
	 * Constant representing the value of the invalid @since tag {@link IApiProblem} kind.
	 * <br>
	 * Value is: <code>4</code>
	 * 
	 * @see #getKind()
	 */
	public static final int SINCE_TAG_INVALID = 0x1 << 2;

	/**
	 * Constant representing the value of the malformed @since tag {@link IApiProblem} kind.
	 * <br>
	 * Value is: <code>2</code>
	 * 
	 * @see #getKind()
	 */
	public static final int SINCE_TAG_MALFORMED = 0x1 << 1;

	/**
	 * Constant representing the value of the missing @since tag {@link IApiProblem} kind.
	 * <br>
	 * Value is: <code>1</code>
	 * 
	 * @see #getKind()
	 */
	public static final int SINCE_TAG_MISSING = 0x1;

	/**
	 * Constant representing the value of the major version change {@link IApiProblem} kind.
	 * <br>
	 * Value is: <code>1</code>
	 * 
	 * @see #getKind()
	 */
	public static final int MAJOR_VERSION_CHANGE = 0x1;

	/**
	 * Constant representing the value of the minor version change {@link IApiProblem} kind.
	 * <br>
	 * Value is: <code>2</code>
	 * 
	 * @see #getKind()
	 */
	public static final int MINOR_VERSION_CHANGE = 0x1 << 1;
	
	/**
	 * Returns the severity of the problem. 
	 * See {@link IMarker} for a listing of severities.
	 * 
	 * @return the severity of the problem
	 */
	public int getSeverity();
	
	/**
	 * Returns the handle to the underlying resource of the marker this problem 
	 * was created from.
	 * 
	 * @return the handle to the underlying resource
	 */
	public IResource getResource();
	
	/**
	 * Returns the unique id of the problem. A problem Id is the composition of
	 * the category, kind, flags and severity.
	 * 
	 * @return the id of the problem
	 */
	public int getId();
	
	/**
	 * Returns the category of this problem
	 * 
	 * @see IApiProblem for categories
	 * @return the category of this problem.
	 */
	public int getCategory();
	
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
