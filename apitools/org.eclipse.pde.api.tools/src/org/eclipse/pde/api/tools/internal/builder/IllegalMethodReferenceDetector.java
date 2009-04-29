/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Detects when a 'no reference' method is called.
 * 
 * @since 1.1
 */
public class IllegalMethodReferenceDetector extends AbstractIllegalMethodReference {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return
			IReference.REF_INTERFACEMETHOD |
			IReference.REF_SPECIALMETHOD |
			IReference.REF_STATICMETHOD |
			IReference.REF_VIRTUALMETHOD |
			IReference.REF_CONSTRUCTORMETHOD;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemKind()
	 */
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_REFERENCE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_REFERENCE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		if (method.isConstructor()) {
			return new String[] {
				Signatures.getMethodSignature(method), 
				getSimpleTypeName(reference.getMember())};
		}
		return new String[] {
			getSimpleTypeName(method), 
			getSimpleTypeName(reference.getMember()), 
			Signatures.getMethodSignature(method)};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument document, IReference reference) throws CoreException, BadLocationException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		Position pos = getMethodNameRange(method.isConstructor(), Signatures.getMethodName(method), document, reference);
		if(pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(method), 
				getQualifiedTypeName(reference.getMember()),
				Signatures.getMethodSignature(method)};
	}
}
