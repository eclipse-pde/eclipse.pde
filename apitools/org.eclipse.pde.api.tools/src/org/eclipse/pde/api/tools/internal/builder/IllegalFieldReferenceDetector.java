/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.model.MethodKey;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;


/**
 * Detects references to 'no reference' fields.
 * 
 * @since 1.1
 */
public class IllegalFieldReferenceDetector extends AbstractProblemDetector {
	
	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.model.MethodKey} to
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor} 
	 */
	private Map fIllegalFields = new HashMap();
	
	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor}
	 * to associated component IDs
	 */
	private Map fFieldComponents = new HashMap();
	
	/**
	 * Adds the given type as not to be extended.
	 * 
	 * @param field a field that is marked no reference
	 * @param componentId the component the type is located in
	 */
	void addIllegalField(IFieldDescriptor field, String componentId) {
		fIllegalFields.put(new MethodKey(field.getEnclosingType().getQualifiedName(), field.getName(), null, true), field);
		fFieldComponents.put(field, componentId);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#considerReference(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	public boolean considerReference(IReference reference) {
		if (super.considerReference(reference) && fIllegalFields.containsKey(new MethodKey(reference.getReferencedTypeName(), reference.getReferencedMemberName(), reference.getReferencedSignature(), true))) {
			retainReference(reference);
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return
			IReference.REF_GETFIELD |
			IReference.REF_GETSTATIC |
			IReference.REF_PUTFIELD |
			IReference.REF_PUTSTATIC;
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
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.FIELD;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getElementType(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getElementType(IReference reference) {
		return IElementDescriptor.FIELD;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(field), 
				getSimpleTypeName(reference.getMember()), 
				field.getName()};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(field), 
				getQualifiedTypeName(reference.getMember()), 
				field.getName()};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument document, IReference reference) throws CoreException, BadLocationException {
		return getFieldNameRange((IApiField) reference.getResolvedReference(), document, reference);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		if(!super.isProblem(reference)) {
			return false;
		}
		Object componentId = fFieldComponents.get(reference.getResolvedReference().getHandle());
		return isReferenceFromComponent(reference, componentId);
	}
}
