/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

public interface IRepositoryReference extends ISiteObject {

	boolean getEnabled();

	void setEnabled(boolean enabled) throws CoreException;

	String getURL();

	void setURL(String url) throws CoreException;

}
