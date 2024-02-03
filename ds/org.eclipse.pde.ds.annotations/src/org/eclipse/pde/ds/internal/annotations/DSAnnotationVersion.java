/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.pde.internal.ds.core.IDSConstants;

@SuppressWarnings("restriction")
public enum DSAnnotationVersion {

	V1_0("1.0", "http://www.osgi.org/xmlns/scr/v1.0.0"),

	V1_1("1.1", IDSConstants.NAMESPACE),

	V1_2("1.2", "http://www.osgi.org/xmlns/scr/v1.2.0"), //$NON-NLS-1$

	V1_3("1.3", "http://www.osgi.org/xmlns/scr/v1.3.0"), //$NON-NLS-1$
	
	V1_4("1.4", "http://www.osgi.org/xmlns/scr/v1.4.0"), //$NON-NLS-1$

	V1_5("1.5", "http://www.osgi.org/xmlns/scr/v1.5.0"); //$NON-NLS-1$

	public static final DSAnnotationVersion DEFAULT_VERSION = DSAnnotationVersion.V1_4;

	private final String namespace;
	private String version;

	private DSAnnotationVersion(String version, String namespace) {
		this.version = version;
		this.namespace = namespace;
	}

	public String getNamespace() {
		return namespace;
	}

	public DSAnnotationVersion max(DSAnnotationVersion other) {
		if (compareTo(other) < 0) {
			return other;
		}

		return this;
	}

	/**
	 * Compares this version with another one
	 * 
	 * @param other
	 * @return true if this version is higher or equal to this version
	 */
	public boolean isEqualOrHigherThan(DSAnnotationVersion other) {
		return other.compareTo(this) >= 0;
	}

	public static DSAnnotationVersion fromNamespace(String namespace) {
		for (DSAnnotationVersion value : values()) {
			if (value.namespace.equals(namespace)) {
				return value;
			}
		}

		return null;
	}

	public String getSpecificationVersion() {
		return version;
	}

	@Override
	public String toString() {
		return version + " - " + namespace;
	}
}
