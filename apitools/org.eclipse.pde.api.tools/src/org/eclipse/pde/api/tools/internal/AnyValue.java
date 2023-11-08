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
	 * Constructor
	 *
	 * @param arg
	 */
	public AnyValue(String arg) {
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