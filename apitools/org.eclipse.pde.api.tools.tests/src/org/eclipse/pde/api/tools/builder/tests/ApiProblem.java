/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.tests.builder.Problem;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;

/**
 * Customized {@link Problem} to handle using a problem id
 *
 * @since 1.0.0
 */
public class ApiProblem extends Problem {

	private int problemid = 0;
	private int linenumber = 0;

	/**
	 * Constructor
	 * @param marker
	 */
	public ApiProblem(IMarker marker) {
		super(marker);
		this.problemid = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0);
		this.linenumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
	}

	/**
	 * Constructor
	 * @param location
	 * @param message
	 * @param resourcePath
	 * @param start
	 * @param end
	 * @param categoryId
	 * @param severity
	 * @param problemid
	 */
	public ApiProblem(String location, String message, IPath resourcePath, int start, int end, int categoryId, int severity, int problemid) {
		super(location, message, resourcePath, start, end, categoryId, severity);
		this.problemid = problemid;
	}

	/**
	 * @return the line number from the problem
	 */
	public int getLineNumber() {
		return this.linenumber;
	}

	/**
	 * @return the problem id for this problem
	 */
	public int getProblemId() {
		return problemid;
	}

}
