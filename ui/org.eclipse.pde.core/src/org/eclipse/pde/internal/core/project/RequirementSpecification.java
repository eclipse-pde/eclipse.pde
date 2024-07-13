/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.project;

import java.util.Objects;

import org.osgi.framework.VersionRange;

/**
 * Common implementation for a requirement specification - host, required bundle,
 * or package import.
 */
public abstract class RequirementSpecification {

	private final String fName;
	private final VersionRange fRange;
	private final boolean fExport;
	private final boolean fOptional;

	/**
	 * Constructs a new requirement specification.
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

	public VersionRange getVersion() {
		return fRange;
	}

	public boolean isExported() {
		return fExport;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof RequirementSpecification spec //
				&& getName().equals(spec.getName()) && isExported() == spec.isExported()
				&& isOptional() == spec.isOptional() && Objects.equals(getVersion(), spec.getVersion());
	}

	@Override
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

	public boolean isOptional() {
		return fOptional;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
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
