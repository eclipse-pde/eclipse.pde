/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.Arrays;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;

/**
 * This class is used by <code>{@link UseScanParser}</code> to visit the API Use Scan reports
 *
 */
public class UseScanReferenceVisitor extends UseScanVisitor {
	private IApiComponent fLookupAPIComponent;
	private List fLookupMemberTypes;
	private String fCurrentReferencedMemberRootType;
	private IComponentDescriptor fCurrentComponent;
	private IComponentDescriptor fReferencingComponent;
	private IReferenceCollection fReferences;
	private IMemberDescriptor fCurrentReferencedMember;

	public UseScanReferenceVisitor(IApiComponent component, String[] lookupTypes, IReferenceCollection references) {
		fLookupAPIComponent = component;
		if (lookupTypes == null || lookupTypes.length == 0) {
			fLookupMemberTypes = null;
		} else {
			fLookupMemberTypes = Arrays.asList(lookupTypes);
		}
		fReferences = references;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public boolean visitComponent(IComponentDescriptor target) {

		if (fLookupAPIComponent == null || fLookupAPIComponent.getSymbolicName().equals(target.getId())) {
			fCurrentComponent = target;
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
	 */
	// Visit only for the specific types, if supplied.
	public boolean visitMember(IMemberDescriptor referencedMember) {
		boolean found = false;

		String referencedMemberRootType;
		if (referencedMember instanceof IReferenceTypeDescriptor) {
			referencedMemberRootType = ((IReferenceTypeDescriptor)referencedMember).getQualifiedName();
		} else {
			referencedMemberRootType = referencedMember.getEnclosingType().getQualifiedName();
		}
		if (referencedMemberRootType.indexOf('$') > -1) {
			referencedMemberRootType = referencedMemberRootType.substring(0, referencedMemberRootType.indexOf('$'));
		}
		found = fLookupMemberTypes == null || fLookupMemberTypes.contains(referencedMemberRootType);
		fCurrentReferencedMemberRootType = referencedMemberRootType;
		fCurrentReferencedMember = referencedMember;
		
		return found;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor)
	 */
	public void visitReference(IReferenceDescriptor reference) {
		ReferenceDescriptor refDesc = new ReferenceDescriptor(fReferencingComponent, reference.getMember(), reference.getLineNumber(), fCurrentComponent, fCurrentReferencedMember, reference.getReferenceKind(), reference.getReferenceFlags(), reference.getVisibility(), null);
		fReferences.add(fCurrentReferencedMemberRootType, refDesc);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public boolean visitReferencingComponent(IComponentDescriptor component) {
		fReferencingComponent = component;
		return true;
	}
}