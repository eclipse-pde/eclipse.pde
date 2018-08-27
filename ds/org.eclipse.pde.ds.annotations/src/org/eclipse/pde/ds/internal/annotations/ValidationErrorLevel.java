/*******************************************************************************
 * Copyright (c) 2016 Ecliptical Software Inc. and others.
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

public enum ValidationErrorLevel {

	error,

	warning,

	ignore;

	public boolean isError() {
		return this == error;
	}

	public boolean isWarning() {
		return this == warning;
	}

	public boolean isIgnore() {
		return this == ignore;
	}
}
