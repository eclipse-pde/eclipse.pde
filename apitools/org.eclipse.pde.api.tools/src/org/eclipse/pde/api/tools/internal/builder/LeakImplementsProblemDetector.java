/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects leaked implemented interfaces.
 *
 * @since 1.1
 */
public class LeakImplementsProblemDetector extends AbstractTypeLeakDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public LeakImplementsProblemDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_IMPLEMENTS;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_IMPLEMENT;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.LEAK_IMPLEMENTS;
	}

}
