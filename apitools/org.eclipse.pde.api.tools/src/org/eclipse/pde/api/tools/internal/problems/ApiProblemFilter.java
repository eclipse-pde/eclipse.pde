/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.problems;

import org.eclipse.core.runtime.Assert;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * Base implementation of {@link IApiProblemFilter}
 *
 * @since 1.0.0
 */
public class ApiProblemFilter implements IApiProblemFilter, Cloneable {

	public static final String HANDLE_ARGUMENTS_DELIMITER = ","; //$NON-NLS-1$
	public static final String HANDLE_DELIMITER = "%]"; //$NON-NLS-1$

	private String fComponentId = null;
	private IApiProblem fProblem = null;
	private String fComment = null;

	/**
	 * Constructor
	 *
	 * @param componentid
	 * @param problem
	 * @param comment
	 */
	public ApiProblemFilter(String componentid, IApiProblem problem, String comment) {
		fComponentId = componentid;
		Assert.isNotNull(problem);
		fProblem = problem;
		fComment = comment;
	}

	@Override
	public String getComment() {
		return fComment;
	}

	/**
	 * Sets the comment for this filter.
	 *
	 * @param comment the comment or <code>null</code> to remove the existing
	 *            comment
	 * @since 1.1
	 */
	public void setComment(String comment) {
		fComment = comment;
	}

	@Override
	public String getComponentId() {
		return fComponentId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiProblemFilter filter) {
			return elementsEqual(filter.getComponentId(), fComponentId) && filter.getUnderlyingProblem().equals(fProblem);
		} else if (obj instanceof IApiProblem) {
			return fProblem.equals(obj);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return fProblem.hashCode() + fComponentId.hashCode();
	}

	/**
	 * Returns if the two specified objects are equal. Objects are considered
	 * equal if:
	 * <ol>
	 * <li>they are both null</li>
	 * <li>they are equal via the default .equals() method</li>
	 * </ol>
	 *
	 * @param s1
	 * @param s2
	 * @return true if the objects are equal, false otherwise
	 */
	private boolean elementsEqual(Object s1, Object s2) {
		return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Filter for : "); //$NON-NLS-1$
		buffer.append(fProblem.toString());
		return buffer.toString();
	}

	@Override
	public Object clone() {
		return new ApiProblemFilter(this.fComponentId, fProblem, fComment);
	}

	@Override
	public IApiProblem getUnderlyingProblem() {
		return fProblem;
	}

	/**
	 * @return returns a handle that can be used to identify the filter
	 */
	public String getHandle() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(fProblem.getId());
		buffer.append(HANDLE_DELIMITER);
		buffer.append(fProblem.getResourcePath());
		buffer.append(HANDLE_DELIMITER);
		buffer.append(fProblem.getTypeName());
		buffer.append(HANDLE_DELIMITER);
		String[] margs = fProblem.getMessageArguments();
		for (int i = 0; i < margs.length; i++) {
			buffer.append(margs[i]);
			if (i < margs.length - 1) {
				buffer.append(HANDLE_ARGUMENTS_DELIMITER);
			}
		}
		return buffer.toString();
	}
}
