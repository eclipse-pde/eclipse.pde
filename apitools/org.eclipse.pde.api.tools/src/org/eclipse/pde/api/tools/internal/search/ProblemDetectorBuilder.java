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
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Builds problem detectors for reference analysis.
 * 
 * @since 1.1
 */
public class ProblemDetectorBuilder extends ApiDescriptionVisitor {

	/**
	 * Problem detectors
	 */
	private IllegalExtendsProblemDetector fIllegalExtends;
	private IllegalImplementsProblemDetector fIllegalImplements;
	private IllegalInstantiateProblemDetector fIllegalInstantiate;
	private IllegalOverrideProblemDetector fIllegalOverride;
	private IllegalMethodReferenceDetector fIllegalMethodRef;
	private IllegalFieldReferenceDetector fIllegalFieldRef;
	/**
	 * Identifier of component elements are being searched for in
	 */
	private String fOwningComponentId;
	
	/**
	 * Cache of non-API package names visisted
	 */
	private Set fNonApiPackageNames = new HashSet();
	
	/**
	 * Sets the owning component (i.e. component of description being visited).
	 * 
	 * @param id
	 */
	void setOwningComponentId(String id) {
		fOwningComponentId = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.descriptors.IElementDescriptor, java.lang.String, org.eclipse.pde.api.tools.IApiAnnotations)
	 */
	public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
		int mask = description.getRestrictions();
		switch (element.getElementType()) {
			case IElementDescriptor.PACKAGE:
				if (VisibilityModifiers.isPrivate(description.getVisibility())) {
					fNonApiPackageNames.add(((IPackageDescriptor)element).getName());
				}
				break;
			default:
				if (!RestrictionModifiers.isUnrestricted(mask)) {
					if(RestrictionModifiers.isOverrideRestriction(mask)) {
						getIllegalOverride().addIllegalMethod((IMethodDescriptor) element, fOwningComponentId);
						// IApiProblem.ILLEGAL_OVERRIDE, IElementDescriptor.T_METHOD
					}
					if (RestrictionModifiers.isExtendRestriction(mask)) {
						getIllegalExtends().addIllegalType((IReferenceTypeDescriptor) element, fOwningComponentId);
						// IApiProblem.ILLEGAL_EXTEND, IElementDescriptor.T_REFERENCE_TYPE 
					}
					if (RestrictionModifiers.isImplementRestriction(mask)) {
						getIllegalImplements().addIllegalType((IReferenceTypeDescriptor) element, fOwningComponentId);
						// IApiProblem.ILLEGAL_IMPLEMENT, IElementDescriptor.T_REFERENCE_TYPE
					}
					if (RestrictionModifiers.isInstantiateRestriction(mask)) {
						getIllegalInstantiate().addIllegalType((IReferenceTypeDescriptor) element, fOwningComponentId);
						// IApiProblem.ILLEGAL_INSTANTIATE, IElementDescriptor.T_REFERENCE_TYPE
					}
					if (RestrictionModifiers.isReferenceRestriction(mask)) {
						if (element.getElementType() == IElementDescriptor.METHOD) {
							getIllegalMethodReference().addIllegalMethod((IMethodDescriptor) element, fOwningComponentId);
							// IApiProblem.ILLEGAL_REFERENCE, IElementDescriptor.T_METHOD
						} else if (element.getElementType() == IElementDescriptor.FIELD) {
							getIllegalFieldReference().addIllegalField((IFieldDescriptor) element, fOwningComponentId);
							// IApiProblem.ILLEGAL_REFERENCE, IElementDescriptor.T_FIELD
						}
					}
				}
		}
		return true;
	}
	
	IllegalExtendsProblemDetector getIllegalExtends() {
		if (fIllegalExtends == null) {
			fIllegalExtends = new IllegalExtendsProblemDetector();
		}
		return fIllegalExtends;
	}
	
	IllegalImplementsProblemDetector getIllegalImplements() {
		if (fIllegalImplements == null) {
			fIllegalImplements = new IllegalImplementsProblemDetector();
		}
		return fIllegalImplements;
	}
	
	IllegalInstantiateProblemDetector getIllegalInstantiate() {
		if (fIllegalInstantiate == null) {
			fIllegalInstantiate = new IllegalInstantiateProblemDetector();
		}
		return fIllegalInstantiate;
	}
	
	IllegalOverrideProblemDetector getIllegalOverride() {
		if (fIllegalOverride == null) {
			fIllegalOverride = new IllegalOverrideProblemDetector();
		}
		return fIllegalOverride;
	}
	
	IllegalMethodReferenceDetector getIllegalMethodReference() {
		if (fIllegalMethodRef == null) {
			fIllegalMethodRef = new IllegalMethodReferenceDetector();
		}
		return fIllegalMethodRef;
	}
	
	IllegalFieldReferenceDetector getIllegalFieldReference() {
		if (fIllegalFieldRef == null) {
			fIllegalFieldRef = new IllegalFieldReferenceDetector();
		}
		return fIllegalFieldRef;
	}
	
	/**
	 * Returns a set of all non-API package names that are in prerequisite components.
	 * 
	 * @return
	 */
	Set getNonApiPackageNames() {
		return fNonApiPackageNames;
	}
	
	/**
	 * Returns a list of problem detectors to be used.
	 * 
	 * @return problem detectors
	 */
	List getProblemDetectors() {
		List detectors = new ArrayList();
		if (fIllegalExtends != null) {
			detectors.add(fIllegalExtends);
		}
		if (fIllegalImplements != null) {
			detectors.add(fIllegalImplements);
		}
		if (fIllegalInstantiate != null) {
			detectors.add(fIllegalInstantiate);
		}
		if (fIllegalOverride != null) {
			detectors.add(fIllegalOverride);
		}
		if (fIllegalMethodRef != null) {
			detectors.add(fIllegalMethodRef);
		}
		if (fIllegalFieldRef != null) {
			detectors.add(fIllegalFieldRef);
		}
		return detectors;
	}
	
}
