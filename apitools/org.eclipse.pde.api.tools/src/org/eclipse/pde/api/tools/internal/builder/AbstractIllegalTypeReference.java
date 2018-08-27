/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Base implementation of a problem detector for type references
 *
 * @since 1.1
 * @noextend This class is not intended to be sub-classed by clients.
 */
public abstract class AbstractIllegalTypeReference extends AbstractProblemDetector {

	/**
	 * Map of fully qualified type names to associated component IDs that
	 * represent illegal references
	 */
	private Map<String, String> fIllegalTypes = new HashMap<>();

	/**
	 * Adds the given type as not to be extended.
	 *
	 * @param type a type that is marked no extend
	 * @param componentId the component the type is located in
	 */
	void addIllegalType(IReferenceTypeDescriptor type, String componentId) {
		fIllegalTypes.put(type.getQualifiedName(), componentId);
	}

	@Override
	public boolean considerReference(IReference reference) {
		if (super.considerReference(reference) && fIllegalTypes.containsKey(reference.getReferencedTypeName())) {
			retainReference(reference);
			return true;
		}
		return false;
	}

	/**
	 * Returns if the mapping contains the referenced type name
	 *
	 * @param reference
	 * @return true of the mapping contains the key false otherwise
	 */
	protected boolean isIllegalType(IReference reference) {
		return fIllegalTypes.containsKey(reference.getReferencedTypeName());
	}

	@Override
	protected boolean isProblem(IReference reference) {
		if (!super.isProblem(reference)) {
			return false;
		}
		IApiMember type = reference.getResolvedReference();
		boolean isConstructorVirtualMethod = false;
		if (type.getName().equals("<init>") && reference.getReferenceKind() == IReference.REF_VIRTUALMETHOD) {//$NON-NLS-1$
			if (type.getParent() != null) {
				isConstructorVirtualMethod = true;
			}
		}
		String componentId = fIllegalTypes.get(isConstructorVirtualMethod ? type.getParent().getName() : type.getName());
		return isReferenceFromComponent(reference, componentId);
	}

	@Override
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		IApiMember member = reference.getMember();
		if (member.getType() == IApiElement.TYPE) {
			ApiType ltype = (ApiType) member;
			IMethod method = null;
			if (ltype.isAnonymous()) {
				// has a side-effect on
				// reference.getMember().setEnclosingMethodInfo(..)
				getEnclosingMethod(type, reference, doc);
				if (reference.getLineNumber() < 0) {
					return defaultSourcePosition(type, reference);
				}
				String name = getSimpleTypeName(reference.getResolvedReference());
				Position pos = getMethodNameRange(true, name, doc, reference);
				if (pos == null) {
					return defaultSourcePosition(type, reference);
				}
				return pos;
			}
			if (ltype.isLocal()) {
				String name = ltype.getSimpleName();
				ICompilationUnit cunit = type.getCompilationUnit();
				if (cunit.isWorkingCopy()) {
					cunit.reconcile(AST.JLS10, false, null, null);
				}
				IType localtype = type;
				method = getEnclosingMethod(type, reference, doc);
				if (method != null) {
					localtype = method.getType(name, 1);
				}
				if (localtype.exists()) {
					ISourceRange range = localtype.getNameRange();
					return new Position(range.getOffset(), range.getLength());
				}
				return defaultSourcePosition(type, reference);
			}
		}
		ISourceRange range = type.getNameRange();
		Position pos = null;
		if (range != null) {
			pos = new Position(range.getOffset(), range.getLength());
		}
		if (pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}

	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.TYPE;
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		if (member.getType() == IApiElement.TYPE) {
			ApiType ltype = (ApiType) member;
			String simpleTypeName = getSimpleTypeName(reference.getResolvedReference());
			if (ltype.isAnonymous()) {
				IApiType etype = ltype.getEnclosingType();
				String signature = Signatures.getQualifiedTypeSignature(etype);
				IApiMethod method = ltype.getEnclosingMethod();
				if (method != null) {
					signature = Signatures.getQualifiedMethodSignature(method);
				}
				return new String[] { signature, simpleTypeName };
			}
			if (ltype.isLocal()) {
				// local types are always defined in methods, include enclosing
				// method infos in message
				IApiType etype = ltype.getEnclosingType();
				IApiMethod method = ltype.getEnclosingMethod();
				if (method != null) {
					String methodsig = Signatures.getQualifiedMethodSignature(method);
					return new String[] {
							Signatures.getAnonymousTypeName(ltype.getName()),
							methodsig, simpleTypeName };
				} else {
					return new String[] {
							Signatures.getAnonymousTypeName(ltype.getName()),
							getSimpleTypeName(etype), simpleTypeName };
				}
			}
		}
		return new String[] {
				getSimpleTypeName(reference.getResolvedReference()),
				getSimpleTypeName(reference.getMember()) };
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		if (member.getType() == IApiElement.TYPE) {
			ApiType ltype = (ApiType) member;
			if (ltype.isLocal() || ltype.isAnonymous()) {
				return getMessageArgs(reference);
			}
		}
		return new String[] {
				getQualifiedTypeName(reference.getResolvedReference()),
				getQualifiedTypeName(reference.getMember()) };
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		IApiMember member = reference.getMember();
		if (member.getType() == IApiElement.TYPE) {
			IApiType type = (IApiType) reference.getMember();
			if (type.isLocal()) {
				return IApiProblem.LOCAL_TYPE;
			}
			if (type.isAnonymous()) {
				return IApiProblem.ANONYMOUS_TYPE;
			}
		}
		return IApiProblem.NO_FLAGS;
	}
}
