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
package org.eclipse.pde.api.tools.internal.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Detects when a method illegally overrides another method.
 *
 * @since 1.1
 */
public class IllegalOverrideProblemDetector extends AbstractIllegalMethodReference {

	@Override
	public int getReferenceKinds() {
		return IReference.REF_OVERRIDE;
	}

	@Override
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_OVERRIDE;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_OVERRIDE;
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(method),
				getSimpleTypeName(reference.getMember()),
				Signature.toString(method.getSignature(), method.getName(), null, false, false) };
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		return new String[] {
				getTypeName(method), getTypeName(reference.getMember()),
				Signatures.getMethodSignature(method) };
	}

	@Override
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		return getSourceRangeForMethod(type, reference, (IApiMethod) reference.getResolvedReference());
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.NO_FLAGS;
	}
}
