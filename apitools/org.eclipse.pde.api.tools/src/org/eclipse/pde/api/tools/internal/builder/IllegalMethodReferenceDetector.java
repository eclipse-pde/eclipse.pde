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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
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

	private Map<String, String> fIllegalTypes = new HashMap<>();

	/**
	 * Adds an {@link IReferenceTypeDescriptor} that is reference-restricted
	 *
	 * @param type the qualified name of the {@link IReferenceTypeDescriptor}
	 *            that is restricted
	 * @param componentid the id of the component that the type is from
	 * @since 1.0.400
	 */
	void addIllegalType(IReferenceTypeDescriptor type, String componentid) {
		fIllegalTypes.put(type.getQualifiedName(), componentid);
	}

	@Override
	public boolean considerReference(IReference reference) {
		if (super.considerReference(reference)) {
			return true;
		}
		if (isEnclosedBy(reference.getReferencedTypeName(), fIllegalTypes.keySet())) {
			retainReference(reference);
			return true;
		}
		return false;
	}

	@Override
	protected boolean isProblem(IReference reference) {
		if (super.isProblem(reference)) {
			return true;
		}
		// check the restricted types listing
		StringTokenizer tokenizer = new StringTokenizer(reference.getReferencedTypeName(), "$"); //$NON-NLS-1$
		String compid = null;
		while (tokenizer.hasMoreTokens()) {
			compid = fIllegalTypes.get(tokenizer.nextToken());
			if (compid != null) {
				break;
			}
		}
		return isReferenceFromComponent(reference, compid);
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_INTERFACEMETHOD | IReference.REF_SPECIALMETHOD | IReference.REF_STATICMETHOD | IReference.REF_VIRTUALMETHOD | IReference.REF_CONSTRUCTORMETHOD;
	}

	@Override
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_REFERENCE;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_REFERENCE;
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		if (method.isConstructor()) {
			return new String[] {
					Signatures.getMethodSignature(method),
					getSimpleTypeName(reference.getMember()) };
		}
		return new String[] {
				getSimpleTypeName(method),
				getSimpleTypeName(reference.getMember()),
				Signatures.getMethodSignature(method) };
	}

	@Override
	protected Position getSourceRange(IType type, IDocument document, IReference reference) throws CoreException, BadLocationException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		Position pos = getMethodNameRange(method.isConstructor(), Signatures.getMethodName(method), document, reference);
		if (pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(method),
				getQualifiedTypeName(reference.getMember()),
				Signatures.getMethodSignature(method) };
	}
}
