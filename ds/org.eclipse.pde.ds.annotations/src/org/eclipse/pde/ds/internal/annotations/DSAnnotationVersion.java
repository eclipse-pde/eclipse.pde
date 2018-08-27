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

	V1_1(IDSConstants.NAMESPACE),

	V1_2("http://www.osgi.org/xmlns/scr/v1.2.0"), //$NON-NLS-1$

	V1_3("http://www.osgi.org/xmlns/scr/v1.3.0"); //$NON-NLS-1$

	private final String namespace;

	private DSAnnotationVersion(String namespace) {
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

	public static DSAnnotationVersion fromNamespace(String namespace) {
		for (DSAnnotationVersion value : values()) {
			if (value.namespace.equals(namespace)) {
				return value;
			}
		}

		return null;
	}
}
