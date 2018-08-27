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

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects when a type illegally extends another type.
 *
 * @since 1.1
 */
public class IllegalExtendsProblemDetector extends AbstractIllegalTypeReference {

	@Override
	public int getReferenceKinds() {
		return IReference.REF_EXTENDS;
	}

	@Override
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_EXTEND;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_EXTEND;
	}
}
