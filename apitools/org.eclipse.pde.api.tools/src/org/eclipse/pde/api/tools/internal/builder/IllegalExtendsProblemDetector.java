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
import org.eclipse.pde.api.tools.internal.util.Util;

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
		if(type.isLocal() && !type.isAnonymous()) {
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
	
	/**
	 * Processes the method name. In the event it is a constructor the simple name of
	 * the enclosing type is returned
	 * @param type
	 * @param methodname
	 * @return
	 * @throws CoreException
	 */
	protected String processMethodName(IApiType type, String methodname) throws CoreException {
		if("<init>".equals(methodname)) { //$NON-NLS-1$
			IApiType enclosingtype = type.getEnclosingType();
			return enclosingtype.getSimpleName();
		}
		return methodname;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		ApiType ltype = (ApiType) reference.getMember();
		if(ltype.isAnonymous()) {
			IApiType etype = ltype.getEnclosingType(); 
			StringBuffer buffer = new StringBuffer();
			buffer.append(etype.getName());
			IApiMethod method = ltype.getEnclosingMethod();
			if(method != null) {
				String methodname = processMethodName(ltype, method.getName());
				if(methodname != null) {
					buffer.append(".").append(Signature.toString(processMethodSignature(method), methodname, null, false, false)); //$NON-NLS-1$
				}
			}
			return new String[] {buffer.toString(), getSimpleTypeName(reference.getResolvedReference())};
		}
		if(ltype.isLocal()) {
			//local types are always defined in methods, include enclosing method infos in message
			IApiType etype = ltype.getEnclosingType(); 
			IApiMethod method = ltype.getEnclosingMethod();
			String methodname = processMethodName(ltype, method.getName());
			return new String[] {
					getAnonymousTypeName(reference.getMember().getName()),
					etype.getName(),
					Signature.toString(processMethodSignature(method), methodname, null, false, false),
					getSimpleTypeName(reference.getResolvedReference())
			};
		}
		return super.getMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		return this.getMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		ApiType ltype = (ApiType) reference.getMember();
		if(ltype.isAnonymous()) {
			if(reference.getLineNumber() < 0) {
				noSourcePosition(type, reference);
			}
			String name = getSimpleTypeName(reference.getResolvedReference());
			Position pos = getMethodNameRange(name, doc, reference);
			if(pos == null) {
				noSourcePosition(type, reference);
			}
			return pos;
		}
		if(ltype.isLocal()) {
			String name = ltype.getSimpleName();
			ltype.getName();
			ICompilationUnit cunit = type.getCompilationUnit();
			if(cunit.isWorkingCopy()) {
				cunit.reconcile(AST.JLS3, false, null, null);
			}
			IMethod method = getEnclosingMethod(ltype, type);
			IType localtype = null;
			if(method != null) {
				localtype = method.getType(name, 1);
			}
			if(localtype.exists()) {
				ISourceRange range = localtype.getNameRange();
				return new Position(range.getOffset(), range.getLength());
			}
			noSourcePosition(type, reference);
		}
		return super.getSourceRange(type, doc, reference);
	}
	
	/**
	 * Collects which signature to use and de-qualifies it. If there is a generic signature
	 * it is returned, otherwise the standard signature is used
	 * @param method
	 * @return the de-qualified signature for the method
	 */
	protected String processMethodSignature(IApiMethod method) {
		String signature = method.getGenericSignature();
		if(signature == null) {
			signature = method.getSignature();
		}
		return Util.dequalifySignature(signature);
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
			String signature = processMethodSignature(apimethod);
			String methodname = processMethodName(type, apimethod.getName());
			IMethod method = jtype.getMethod(methodname, Signature.getParameterTypes(signature));
			if(method.exists()) {
				return method;
			}
		}
		return null;
	}
}
