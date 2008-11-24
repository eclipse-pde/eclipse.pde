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

import java.util.List;


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
	 * Returns a list of any problems detected after analyzing potential reference
	 * problems that are now resolved.
	 * 
	 * @return list of {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem}
	 */
	public List createProblems();
}
