/*******************************************************************************
 * Copyright (c) 2016 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
