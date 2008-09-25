/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.IVersionRange;

/**
 * Implementation of a required component description based on
 * OSGi bundles.
 * 
 * @since 1.0.0
 */
public class RequiredComponentDescription implements IRequiredComponentDescription {
	
	private String fId;
	private boolean fIsOptional;
	private boolean fIsExprted;
	private IVersionRange fRange;
	
	
	/**
	 * Constructs a new required component description based on the given
	 * required component id and version range. The required component description is
	 * mandatory. 
	 * 
	 * @param id component's symbolic name
	 * @param range version range
	 */
	public RequiredComponentDescription(String id, IVersionRange range) {
		this(id, range, false, false);
	}
	
	/**
	 * Constructs a new required component description based on the given
	 * required component id and version range. 
	 * 
	 * @param id component's symbolic name
	 * @param range version range
	 * @param isOptional the optional flag of the required component
	 * @param isExported whether the required component is re-exported by the declaring component
	 */
	public RequiredComponentDescription(String id, IVersionRange range, boolean isOptional, boolean isExported) {
		fId = id;
		fRange = range;
		fIsOptional = isOptional;
		fIsExprted = isExported;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof RequiredComponentDescription) {
			RequiredComponentDescription desc = (RequiredComponentDescription) obj;
			return fId.equals(desc.fId) && fRange.equals(desc.fRange);
		}
		return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see IRequiredComponentDescription#getId()
	 */
	public String getId() {
		return fId;
	}

	/** (non-Javadoc)
	 * @see IRequiredComponentDescription#getVersionRange()
	 */
	public IVersionRange getVersionRange() {
		return fRange;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fId.hashCode() + fRange.hashCode();
	}

	/* (non-Javadoc)
	 * @see IRequiredComponentDescription#isOptional()
	 */
	public boolean isOptional() {
		return this.fIsOptional;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(fId);
		buf.append(' ');
		buf.append(fRange.toString());
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription#isExported()
	 */
	public boolean isExported() {
		return fIsExprted;
	}

}
