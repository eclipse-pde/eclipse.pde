/*******************************************************************************
 * Copyright (c) Aug 21, 2013 IBM Corporation and others.
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
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Detector for illegally referenced annotations
 *
 * @since 1.0.600
 */
public class IllegalAnnotationReferenceDetector extends AbstractIllegalTypeReference {

	@Override
	public int getReferenceKinds() {
		return IReference.REF_ANNOTATION_USE;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.ANNOTATION;
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
	public boolean considerReference(IReference reference) {
		return super.considerReference(reference);
	}

	@Override
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		IApiMember member = reference.getMember();
		if (member != null) {
			IAnnotatable annotable = findAnnotableElement(type, member, reference, doc);
			if (annotable != null) {
				IAnnotation[] annotations = annotable.getAnnotations();
				String typename = Signatures.getSimpleTypeName(reference.getResolvedReference().getName());
				for (IAnnotation annotation : annotations) {
					if (annotation.getElementName().equals(typename)) {
						ISourceRange range = annotation.getNameRange();
						if (range != null) {
							// select the '@' as well - augment offset and
							// length accordingly
							return new Position(range.getOffset() - 1, range.getLength() + 1);
						} else {
							break;
						}
					}
				}
			}
		}
		return defaultSourcePosition(type, reference);
	}

	/**
	 * Tries to find the {@link IAnnotatable} parent for the given
	 * {@link IApiMember}. If the direct parent cannot be computed return
	 * <code>null</code> and not the enclosing {@link IAnnotatable} (if there is
	 * one). That way we can fetch the default illegal type reference message
	 * location
	 *
	 * @param type
	 * @param member
	 * @param reference
	 * @param doc
	 * @return the {@link IAnnotatable} parent or <code>null</code> if it could
	 *         not be computed.
	 * @throws CoreException
	 */
	IAnnotatable findAnnotableElement(IType type, IApiMember member, IReference reference, IDocument doc) throws CoreException {
		switch (member.getType()) {
			case IApiElement.TYPE: {
				return findTypeInType(type, (IApiType) member, reference, doc);
			}
			case IApiElement.FIELD: {
				return findFieldInType(type, (IApiField) member);
			}
			case IApiElement.METHOD: {
				return findMethodInType(type, (IApiMethod) member);
			}
			default:
				break;
		}
		return null;
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		return new String[] { getSimpleTypeName(reference.getResolvedReference()) };
	}
}
