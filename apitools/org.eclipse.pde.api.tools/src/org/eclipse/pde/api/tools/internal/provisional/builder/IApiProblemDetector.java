/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional.builder;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Detects API use problems and leaks.
 *
 * @since 1.1
 */
public interface IApiProblemDetector {

	/**
	 * Returns a bit mask of reference kinds this analyzer is interested in.
	 *
	 * @return bit mask of {@link ReferenceModifiers} constants
	 */
	public int getReferenceKinds();

	/**
	 * Returns whether the given unresolved reference is a potential problem.
	 * This analyzer should retain the reference if it is a potential problem
	 * for further analysis once references have been resolved.
	 *
	 * @param reference potential problem
	 * @return whether the unresolved reference is a potential problem
	 */
	public boolean considerReference(IReference reference);

	/**
	 * Returns a list of any problems detected after analyzing potential
	 * reference problems that are now resolved.
	 *
	 * @param monitor the monitor to report progress.
	 * @return list of
	 *         {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem}
	 */
	public List<IApiProblem> createProblems(IProgressMonitor monitor);
}
