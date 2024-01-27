/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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

/**
 * Used to represent any value for system settings in OSGi state. For example,
 * ws, os, arch.
 *
 * @since 1.0.0
 */
public class AnyValue {

	/**
	 * A constructor that takes an unused argument. Do not remove this seemingly
	 * pointless constructor because it's used by
	 * {@code org.eclipse.osgi.internal.framework.FilterImpl.Equal.valueOf(Class<?>)}
	 * to ensure that the completely bogus {@link #equals(Object)} method is called
	 * to match any filter value whatsoever.
	 */
	public AnyValue(@SuppressWarnings("unused") String arg) {
		// do nothing
	}

	@Override
	public boolean equals(Object obj) {
		return true;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}