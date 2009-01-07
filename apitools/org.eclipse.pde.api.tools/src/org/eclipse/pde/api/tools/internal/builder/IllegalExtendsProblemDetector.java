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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.builder.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Detects when a type illegally extends another type.
 * 
 * @since 1.1
 */
public class IllegalExtendsProblemDetector extends AbstractIllegalTypeReference {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return ReferenceModifiers.REF_EXTENDS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getProblemKind()
	 */
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_EXTEND;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		IApiType type = (IApiType) reference.getMember();
		if(type.isLocal()) {
			return IApiProblem.LOCAL_TYPE;
		}
		if(type.isAnonymous()) {
			return IApiProblem.ANONYMOUS_TYPE;
		}
		return super.getProblemFlags(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_EXTEND;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		ApiType ltype = (ApiType) reference.getMember();
		if(ltype.isAnonymous()) {
			IApiType etype = ltype.getEnclosingType();
			String signature = Signatures.getQualifiedTypeSignature(etype);
			IApiMethod method = ltype.getEnclosingMethod();
			if(method != null) {
				signature = Signatures.getQualifiedMethodSignature(method);
			}
			return new String[] {signature, getSimpleTypeName(reference.getResolvedReference())};
		}
		if(ltype.isLocal()) {
			//local types are always defined in methods, include enclosing method infos in message
			IApiType etype = ltype.getEnclosingType(); 
			IApiMethod method = ltype.getEnclosingMethod();
			if(method != null) {
				String methodsig = Signatures.getQualifiedMethodSignature(method);
				return new String[] {
						Signatures.getAnonymousTypeName(reference.getMember().getName()),
						methodsig,
						getSimpleTypeName(reference.getResolvedReference())
				};
			}
			else {
				return new String[] {
						Signatures.getAnonymousTypeName(reference.getMember().getName()), 
						getSimpleTypeName(etype), 
						getSimpleTypeName(reference.getResolvedReference())};
			}
		}
		return super.getMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		ApiType ltype = (ApiType) reference.getMember();
		if(ltype.isLocal() || ltype.isAnonymous()) {
			return this.getMessageArgs(reference);
		}
		return super.getQualifiedMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		ApiType ltype = (ApiType) reference.getMember();
		if(ltype.isAnonymous()) {
			if(reference.getLineNumber() < 0) {
				return defaultSourcePosition(type, reference);
			}
			String name = getSimpleTypeName(reference.getResolvedReference());
			Position pos = getMethodNameRange(name, doc, reference);
			if(pos == null) {
				return defaultSourcePosition(type, reference);
			}
			return pos;
		}
		if(ltype.isLocal()) {
			String name = ltype.getSimpleName();
			ICompilationUnit cunit = type.getCompilationUnit();
			if(cunit.isWorkingCopy()) {
				cunit.reconcile(AST.JLS3, false, null, null);
			}
			IMethod method = getEnclosingMethod(ltype, type);
			IType localtype = type;
			if(method != null) {
				localtype = method.getType(name, 1);
			}
			if(localtype.exists()) {
				ISourceRange range = localtype.getNameRange();
				return new Position(range.getOffset(), range.getLength());
			}
			return defaultSourcePosition(type, reference);
		}
		return super.getSourceRange(type, doc, reference);
	}
	
	/**
	 * Returns the enclosing {@link IMethod} for the given type or <code>null</code>
	 * if it cannot be computed
	 * @param type
	 * @param jtype
	 * @return the {@link IMethod} enclosing the given type or <code>null</code>
	 * @throws CoreException
	 */
	private IMethod getEnclosingMethod(ApiType type, IType jtype) throws CoreException { 
		IApiMethod apimethod = type.getEnclosingMethod();
		if(apimethod != null) {
			String signature = Signatures.processMethodSignature(apimethod);
			String methodname = Signatures.getMethodName(apimethod);
			IMethod method = jtype.getMethod(methodname, Signature.getParameterTypes(signature));
			if(method.exists()) {
				return method;
			}
		}
		return null;
	}
}
