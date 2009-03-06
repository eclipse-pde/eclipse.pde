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
	public LeakFieldProblemDetector(Set nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractTypeLeakDetector#isApplicable(org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
	 */
	protected boolean isApplicable(IApiAnnotations annotations) {
		return super.isApplicable(annotations) && 
			!RestrictionModifiers.isReferenceRestriction(annotations.getRestrictions());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return IReference.REF_FIELDDECL;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getElementType(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getElementType(IReference reference) {
		return IElementDescriptor.FIELD;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_FIELD_DECL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.LEAK_FIELD;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractTypeLeakDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		if (super.isProblem(reference)) {
			IApiField field = (IApiField) reference.getMember();
			if ((Flags.AccProtected & field.getModifiers()) > 0) {
				// TODO: could do this check before resolution - it's a check on the source location
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractTypeLeakDetector#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(type), 
				getSimpleTypeName(field), 
				field.getName()};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractTypeLeakDetector#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(type), 
				getQualifiedTypeName(field), 
				field.getName()};
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractTypeLeakDetector#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		IApiField field = (IApiField) reference.getMember();
		IField javaField = type.getField(field.getName());
		Position pos = null;
		if (javaField.exists()) {
			ISourceRange range = javaField.getNameRange();
			if(range != null) {
				pos = new Position(range.getOffset(), range.getLength()); 
			}
		}
		if(pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}
}
