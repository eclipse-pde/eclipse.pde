/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
	private final int bits;
	boolean isExact;

	public ApiAnnotations(int visibility, int restrictions) {
		this.bits = (visibility << OFFSET_VISIBILITY) | (restrictions << OFFSET_RESTRICTIONS);
	}

	@Override
	public int getRestrictions() {
		return (this.bits & RESTRICTIONS_MASK) >> OFFSET_RESTRICTIONS;
	}

	@Override
	public int getVisibility() {
		return (this.bits & VISIBILITY_MASK) >> OFFSET_VISIBILITY;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(VisibilityModifiers.getVisibilityName(getVisibility()));
		buffer.append(" / "); //$NON-NLS-1$
		int restrictions = getRestrictions();
		buffer.append(RestrictionModifiers.getRestrictionText(restrictions));
		return buffer.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ApiAnnotations desc) {
			return this.bits == desc.bits;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.bits;
	}

	public void setExact(boolean equals) {
		isExact = equals;
	}

	public boolean isExact() {
		return isExact;
	}

}
