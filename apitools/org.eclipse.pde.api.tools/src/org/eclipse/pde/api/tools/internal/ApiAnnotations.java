/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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

	public static final int VISIBILITY_MASK = 0x000F;
	public static final int RESTRICTIONS_MASK = 0x01F0;
	public static final int OFFSET_VISIBILITY = 0;
	public static final int OFFSET_RESTRICTIONS = 4;
	private int bits;
	
	public ApiAnnotations(int visibility, int restrictions) {
		this.bits = (visibility << OFFSET_VISIBILITY)
			| (restrictions << OFFSET_RESTRICTIONS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getRestrictions()
	 */
	public int getRestrictions() {
		return (this.bits & RESTRICTIONS_MASK) >> OFFSET_RESTRICTIONS;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getVisibility()
	 */
	public int getVisibility() {
		return (this.bits & VISIBILITY_MASK) >> OFFSET_VISIBILITY;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(VisibilityModifiers.getVisibilityName(getVisibility()));
		buffer.append(" / "); //$NON-NLS-1$
		int restrictions = getRestrictions();
		buffer.append(RestrictionModifiers.getRestrictionText(restrictions));
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ApiAnnotations) {
			ApiAnnotations desc = (ApiAnnotations) obj;
			return
				this.bits == desc.bits;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.bits;
	}
	
}
