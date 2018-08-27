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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects leaked field declarations (declared type).
 *
 * @since 1.1
 */
public class LeakFieldProblemDetector extends AbstractTypeLeakDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public LeakFieldProblemDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	protected boolean isApplicable(IApiAnnotations annotations) {
		return super.isApplicable(annotations) && !RestrictionModifiers.isReferenceRestriction(annotations.getRestrictions());
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_FIELDDECL;
	}

	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.FIELD;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_FIELD_DECL;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.LEAK_FIELD;
	}

	@Override
	protected boolean isProblem(IReference reference) {
		if (super.isProblem(reference)) {
			IApiField field = (IApiField) reference.getMember();
			if ((Flags.AccProtected & field.getModifiers()) > 0) {
				// TODO: could do this check before resolution - it's a check on
				// the source location
				// ignore protected members if contained in a @noextend type
				try {
					IApiDescription description = field.getApiComponent().getApiDescription();
					IApiAnnotations annotations = description.resolveAnnotations(field.getHandle().getEnclosingType());
					if (annotations == null || RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
						return false;
					}
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(type), getSimpleTypeName(field),
				field.getName() };
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(type), getQualifiedTypeName(field),
				field.getName() };
	}

	@Override
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		IApiField field = (IApiField) reference.getMember();
		IField javaField = type.getField(field.getName());
		Position pos = null;
		if (javaField.exists()) {
			ISourceRange range = javaField.getNameRange();
			if (range != null) {
				pos = new Position(range.getOffset(), range.getLength());
			}
		}
		if (pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}
}
