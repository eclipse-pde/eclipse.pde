/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

public class ExecutionEnvironment extends PDEManifestElement {

	private static final long serialVersionUID = 1L;

	public ExecutionEnvironment(ManifestHeader header, String value) {
		super(header, value);
	}

	public String getName() {
		return getValue();
	}

	@Override
	public String toString() {
		return getName();
	}

}
