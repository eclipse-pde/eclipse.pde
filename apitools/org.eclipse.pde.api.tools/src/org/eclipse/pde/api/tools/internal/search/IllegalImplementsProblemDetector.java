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

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;

/**
 * Detects when a type illegally implements another type.
 * 
 * @since 1.1
 */
public class IllegalImplementsProblemDetector extends AbstractIllegalTypeReference {

	/**
	 * Map of directly implemented interfaces to implement restricted super-interfaces
	 */
	private HashMap fRestrictedInterfaces = new HashMap();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return ReferenceModifiers.REF_IMPLEMENTS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getProblemKind()
	 */
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_IMPLEMENT;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_IMPLEMENT;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#considerReference(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	public boolean considerReference(IReference reference) {
		try {
			if(super.considerReference(reference)) {
				return true;
			}
			IApiType type = (IApiType) reference.getMember();
			IApiType[] inters = type.getSuperInterfaces();
			IApiType inter = null;
			for (int j = 0; j < inters.length; j++) {
				if(inters[j].getName().equals(reference.getReferencedTypeName())) {
					inter = inters[j];
					break;
				}
			}
			if(inter != null && findRestrictedSuperinterfaces(type.getApiComponent(), reference.getReferencedTypeName(), inter)) {
				retainReference(reference);
				return true;
			}
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		try {
			if(isIllegalType(reference)) {
				return super.isProblem(reference);
			}
			if(fRestrictedInterfaces.size() > 0) {
				IApiMember member = reference.getMember();
				if(member.getType() == IApiElement.TYPE) {
					IApiType itype = (IApiType) fRestrictedInterfaces.get(reference.getReferencedTypeName());
					return itype != null && !isImplemented(((IApiType) member).getSuperclass(), itype.getName());
				}
			}
			return true;
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		return super.isProblem(reference);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		if(isIllegalType(reference)) {
			return super.getMessageArgs(reference);
		}
		if(fRestrictedInterfaces.size() > 0) {
			IApiType type = (IApiType) reference.getResolvedReference();
			IApiType inter = (IApiType) fRestrictedInterfaces.get(type.getName());
			if(inter != null) {
				return new String[] {getSimpleTypeName(type), inter.getSimpleName(), getSimpleTypeName(reference.getMember())};
			}
		}
		return super.getMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		if(isIllegalType(reference)) {
			return super.getProblemFlags(reference);
		}
		return IApiProblem.INDIRECT_REFERENCE;
	}

	/**
	 * Returns if the given type implements any of the given interfaces anywhere in its lineage
	 * @param type
	 * @param iname
	 * @return true if all of the interfaces are implemented, false otherwise
	 * @throws CoreException
	 */
	private boolean isImplemented(IApiType type, final String iname) throws CoreException {
		if(type == null) {
			return false;
		}
		IApiType[] sinters = type.getSuperInterfaces();
		for (int i = 0; i < sinters.length; i++) {
			if(sinters[i].getName().equals(iname)) {
				return true;
			}
		}
		return isImplemented(type.getSuperclass(), iname);
	}
	
	/**
	 * Finds all of the implements restricted interfaces in the hierarchy of this given type
	 * @param originalcomponent the original {@link IApiComponent}
	 * @param entryinterface the name of the interface we originally entered the recursion with
	 * @param type the {@link IApiType} to inspect the interfaces of
	 * @throws CoreException
	 */
	private boolean findRestrictedSuperinterfaces(final IApiComponent originalcomponent, final String entryinterface, IApiType type) throws CoreException {
		IApiType[] inters = type.getSuperInterfaces();
		if(inters.length == 0) {
			return false;
		}
		IApiAnnotations annot = null;
		IApiComponent comp = null;
		for (int i = 0; i < inters.length; i++) {
			comp = inters[i].getApiComponent();
			if(comp == null) {
				continue;
			}
			if(!comp.equals(originalcomponent)) {
				annot = comp.getApiDescription().resolveAnnotations(Factory.typeDescriptor(inters[i].getName()));
				if(annot != null && RestrictionModifiers.isImplementRestriction(annot.getRestrictions())) {
					return fRestrictedInterfaces.put(entryinterface, inters[i]) == null;
				}
			}
			return findRestrictedSuperinterfaces(originalcomponent, entryinterface, inters[i]);
		}
		return false;
	}
}
