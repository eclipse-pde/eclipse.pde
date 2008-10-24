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
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;

/**
 * Detects when a method illegally overrides another method.
 * 
 * @since 1.1
 */
public class IllegalOverrideProblemDetector extends AbstractIllegalMethodReference {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return ReferenceModifiers.REF_OVERRIDE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemKind()
	 */
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_OVERRIDE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_OVERRIDE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(method),
				getSimpleTypeName(reference.getMember()),
				Signature.toString(method.getSignature(), method.getName(), null, false, false)};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		return new String[] {
				getTypeName(method),
				getTypeName(reference.getMember()),
				Signature.toString(method.getSignature(), method.getName(), null, false, false)};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
		for (int i = 0; i < parameterTypes.length; i++) {
			parameterTypes[i] = parameterTypes[i].replace('/', '.');
		}
		IMethod Qmethod = type.getMethod(method.getName(), parameterTypes);
		IMethod[] methods = type.getMethods();
		IMethod match = null;
		for (int i = 0; i < methods.length; i++) {
			IMethod m = methods[i];
			if (m.isSimilar(Qmethod)) {
				match = m;
				break;
			}
		}
		Position pos = null;
		if (match != null) {
			ISourceRange range = match.getNameRange();
			if(range != null) {
				pos = new Position(range.getOffset(), range.getLength());
			}
		}
		if(pos == null) {
			noSourcePosition(type, reference);
		}
		return pos;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.NO_FLAGS;
	}	
}
