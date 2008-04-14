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
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
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
	 * @see org.eclipse.pde.api.tools.internal.search.SearchCriteria#matchesSourceModifiers(org.eclipse.pde.api.tools.internal.provisional.search.ILocation)
	 */
	protected boolean matchesSourceModifiers(ILocation location) {
		IMemberDescriptor member = location.getMember();
		while (member != null) {
			int modifiers = member.getModifiers();
			if (Util.isPublic(modifiers) || Util.isProtected(modifiers)) {
				member = member.getEnclosingType();
			} else {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.SearchCriteria#matchesSourceApiRestrictions(org.eclipse.pde.api.tools.internal.provisional.search.ILocation)
	 */
	protected boolean matchesSourceApiRestrictions(ILocation location) {
		IApiComponent apiComponent = location.getApiComponent();
		try {
			IApiAnnotations annotations = apiComponent.getApiDescription().resolveAnnotations(location.getMember());
			if (annotations != null) {
				if ((annotations.getVisibility() & fSourceVisibility) > 0) {
					int ares = annotations.getRestrictions();
					if(ares != 0) {
						return (ares & RestrictionModifiers.NO_OVERRIDE) == 0 ||
								(ares & RestrictionModifiers.NO_REFERENCE) == 0; 
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
