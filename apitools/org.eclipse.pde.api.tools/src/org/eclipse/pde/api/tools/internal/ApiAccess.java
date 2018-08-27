/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;

/**
 * Default implementation of {@link IApiAccess}
 *
 * @since 1.0.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiAccess implements IApiAccess {

	public static final IApiAccess NORMAL_ACCESS = new NormalAccess();

	static class NormalAccess implements IApiAccess {
		@Override
		public int getAccessLevel() {
			return IApiAccess.NORMAL;
		}
	}

	private int access = IApiAccess.NORMAL;

	/**
	 * Constructor
	 *
	 * @param access
	 */
	public ApiAccess(int access) {
		this.access = access;
	}

	@Override
	public int getAccessLevel() {
		return this.access;
	}

	@Override
	public int hashCode() {
		return this.access;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiAccess) {
			return this.access == ((IApiAccess) obj).getAccessLevel();
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Access Level: "); //$NON-NLS-1$
		buffer.append(getAccessText(getAccessLevel()));
		return buffer.toString();
	}

	/**
	 * Returns a textual representation of an {@link IApiAccess}
	 *
	 * @param access
	 * @return the textual representation of an {@link IApiAccess}
	 */
	public static String getAccessText(int access) {
		switch (access) {
			case IApiAccess.NORMAL:
				return "NORMAL"; //$NON-NLS-1$
			case IApiAccess.FRIEND:
				return "FRIEND"; //$NON-NLS-1$
			default:
				break;
		}
		return "<UNKNOWN ACCESS LEVEL>"; //$NON-NLS-1$
	}
}
