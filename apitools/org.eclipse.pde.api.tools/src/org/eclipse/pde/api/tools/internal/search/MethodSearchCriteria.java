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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Search criteria for method descriptors.
 * This criteria always filters @noreference and @nooverride restrictions
 * 
 * @since 1.0.0
 */
public class MethodSearchCriteria extends SearchCriteria {
	
	/**
	 * Constructor
	 * @param referencekind
	 */
	public MethodSearchCriteria(int referencekind, Object userdata) {
		setReferencedRestrictions(VisibilityModifiers.PRIVATE, RestrictionModifiers.ALL_RESTRICTIONS);
		setReferenceKinds(referencekind);
		setUserData(userdata);
		fSourceVisibility = VisibilityModifiers.API;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.SearchCriteria#matchesSourceModifiers(org.eclipse.pde.api.tools.internal.provisional.model.IApiMember)
	 */
	protected boolean matchesSourceModifiers(IApiMember member) {
		while (member != null) {
			int modifiers = member.getModifiers();
			if (Util.isPublic(modifiers) || Util.isProtected(modifiers)) {
				try {
					member = member.getEnclosingType();
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.SearchCriteria#matchesSourceApiRestrictions(org.eclipse.pde.api.tools.internal.provisional.model.IApiMember)
	 */
	protected boolean matchesSourceApiRestrictions(IApiMember member) {
		if(member.getType() != IApiElement.METHOD) {
			return false;
		}
		IApiComponent apiComponent = member.getApiComponent();
		try {
			IApiMethod method = (IApiMethod) member;
			IApiAnnotations annotations = apiComponent.getApiDescription().resolveAnnotations(method.getHandle());
			if (annotations != null) {
				if ((annotations.getVisibility() & fSourceVisibility) > 0) {
					int ares = annotations.getRestrictions();
					if(ares != 0) {
						if(method.isConstructor()) {
							return (ares & RestrictionModifiers.NO_REFERENCE) == 0;
						}
						if((ares & RestrictionModifiers.NO_OVERRIDE) == 0) {
							IApiAnnotations annot = apiComponent.getApiDescription().resolveAnnotations(method.getEnclosingType().getHandle());
							int pres = 0;
							if(annot != null) {
								pres = annot.getRestrictions();
							}
							return (ares & RestrictionModifiers.NO_REFERENCE) != 0 && (!Util.isFinal(method.getModifiers())
									&& !Util.isStatic(method.getModifiers())
									&& !Util.isFinal(method.getEnclosingType().getModifiers())
									&& ((pres & RestrictionModifiers.NO_EXTEND) == 0));
						}
						return  (ares & RestrictionModifiers.NO_REFERENCE) == 0; 
					}
					else {
						return fSourceRestriction != 0;
					}
				}
			} else {
				return true;
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}
}
