/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;

/**
 * Base implementation of the {@linkplain IApiAnnotations} interface
 * 
 * @since 1.0.0
 */
public class ApiAnnotations implements IApiAnnotations {

	private int fVisibility, fRestrictions;
	
	/**
	 * Constructs API annotations.
	 * 
	 * @param visibility the visibility of an element. See {@linkplain VisibilityModifiers} for visibility constants
	 * @param restrictions the restrictions for an element. See {@linkplain RestrictionModifiers} for restriction kind constants
	 */
	public ApiAnnotations(int visibility, int restrictions) {
		fVisibility = visibility;
		fRestrictions = restrictions;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getRestrictions()
	 */
	public int getRestrictions() {
		return fRestrictions;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getVisibility()
	 */
	public int getVisibility() {
		return fVisibility;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String visibility = null;
		switch (fVisibility) {
		case VisibilityModifiers.API:
			visibility = "API"; //$NON-NLS-1$
			break;
		case VisibilityModifiers.SPI:
			visibility = "SPI"; //$NON-NLS-1$
			break;
		case VisibilityModifiers.PRIVATE_PERMISSIBLE:
			visibility = "PRIVATE PERMISSIBLE"; //$NON-NLS-1$
			break;
		case VisibilityModifiers.PRIVATE:
			visibility = "PRIVATE"; //$NON-NLS-1$
			break;
		case 0:
			visibility = "INHERITED"; //$NON-NLS-1$
			break;
		default:
			visibility = "<unknown visibility>"; //$NON-NLS-1$
			break;
		}
		buffer.append(visibility);
		buffer.append(" / "); //$NON-NLS-1$
		int restrictions = getRestrictions();
		if (restrictions == RestrictionModifiers.NO_RESTRICTIONS) {
			buffer.append("<no restrictions>"); //$NON-NLS-1$
		} else {
			if (RestrictionModifiers.isExtendRestriction(restrictions)) {
				buffer.append("@noextend "); //$NON-NLS-1$
			}
			if (RestrictionModifiers.isImplementRestriction(restrictions)) {
				buffer.append("@noimplement "); //$NON-NLS-1$
			}
			if (RestrictionModifiers.isInstantiateRestriction(restrictions)) {
				buffer.append("@noinstantiate "); //$NON-NLS-1$
			}
			if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
				buffer.append("@noreference"); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ApiAnnotations) {
			ApiAnnotations desc = (ApiAnnotations) obj;
			return
				fRestrictions == desc.fRestrictions &&
				fVisibility == desc.fVisibility;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fRestrictions + fVisibility;
	}
	
}
