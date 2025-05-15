/*******************************************************************************
 * Copyright (c)  2005, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

public class CatchAllValue {
	public static CatchAllValue singleton = new CatchAllValue("*"); //$NON-NLS-1$

	public CatchAllValue(String s) {
		//do nothing
	}

	@Override
	public boolean equals(Object obj) {
		return true;
	}

	@Override
	public int hashCode() {
		return 31;
	}
}