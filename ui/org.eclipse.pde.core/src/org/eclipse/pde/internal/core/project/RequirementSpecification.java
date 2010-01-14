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
package org.eclipse.pde.internal.core.project;

import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * Common implementation for a requirement specification - host, required bundle,
 * or package import.
 */
public abstract class RequirementSpecification {

	private String fName;
	private VersionRange fRange;
	private boolean fExport;
	private boolean fOptional;

	/**
	 * Constructs a new requirement specification.
	 * 
	 * @param name
	 * @param range
	 * @param export
	 */
	RequirementSpecification(String name, VersionRange range, boolean export, boolean optional) {
		fName = name;
		fRange = range;
		fExport = export;
		fOptional = optional;
	}

	public String getName() {
		return fName;
	}

	public VersionRange getVersionRange() {
		return fRange;
	}

	public boolean isExported() {
		return fExport;
	}

	public boolean equals(Object obj) {
		if (obj instanceof RequirementSpecification) {
			RequirementSpecification spec = (RequirementSpecification) obj;
			return getName().equals(spec.getName()) && isExported() == spec.isExported() && isOptional() == spec.isOptional() && equalOrNull(getVersionRange(), spec.getVersionRange());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int code = getClass().hashCode() + fName.hashCode();
		if (fRange != null) {
			code += fRange.hashCode();
		}
		if (fExport) {
			code++;
		}
		if (fOptional) {
			code = code + 2;
		}
		return code;
	}

	private boolean equalOrNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return o1 == null;
		}
		return o1.equals(o2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IRequirementSpecification#isOptional()
	 */
	public boolean isOptional() {
		return fOptional;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(fName);
		buf.append(' ');
		if (fRange != null) {
			buf.append(fRange);
		}
		if (fOptional) {
			buf.append(" optional"); //$NON-NLS-1$
		}
		if (fExport) {
			buf.append(" re-export"); //$NON-NLS-1$
		}
		return buf.toString();
	}

}
