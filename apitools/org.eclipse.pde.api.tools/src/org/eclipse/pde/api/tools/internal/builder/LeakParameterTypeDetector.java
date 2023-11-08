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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects leaks in method parameter types
 *
 * @since 1.1
 */
public class LeakParameterTypeDetector extends MethodLeakDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public LeakParameterTypeDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_METHOD_PARAM;
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_PARAMETER;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		IApiMethod method = (IApiMethod) reference.getMember();
		if (method.isConstructor()) {
			return IApiProblem.LEAK_CONSTRUCTOR_PARAMETER;
		}
		return IApiProblem.LEAK_METHOD_PARAMETER;
	}

}
