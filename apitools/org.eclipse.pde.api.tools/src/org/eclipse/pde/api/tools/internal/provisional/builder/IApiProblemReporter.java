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
package org.eclipse.pde.api.tools.internal.provisional.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Describes a problem reporter used by the framework of API builders.
 * Problem reporters are used to collect problems from an associated
 * API builder and optionally create workspace markers.
 * <p>
 * Clients may implement this interface
 * </p>
 * 
 * @since 1.0.0
 * 
 */
public interface IApiProblemReporter {

	/**
	 * Creates a new problem and adds it to this listing of problems this reporter is keeping track of.
	 * If the problem is already in the reporter no work is done.
	 * @param problem the problem to add
	 * @return true if a problem was added to the reporter, false otherwise
	 */
	public boolean addProblem(IApiProblem problem);
	
	/**
	 * Creates new markers for the listing of problems added to this reporter.
	 * 
	 * @param monitor to monitor progress
	 */
	public void createMarkers(IProgressMonitor monitor);
	
	/**
	 * Cleans up all the reporter and frees any used memory.
	 * This method is called at the end of the build cycle this reporter
	 * is taking part in.
	 */
	public void dispose();
	
}
