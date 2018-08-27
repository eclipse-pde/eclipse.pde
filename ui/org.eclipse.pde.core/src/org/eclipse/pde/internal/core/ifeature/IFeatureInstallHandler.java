/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;

public interface IFeatureInstallHandler extends IFeatureObject {
	String P_LIBRARY = "library"; //$NON-NLS-1$

	String P_HANDLER_NAME = "handlerName"; //$NON-NLS-1$

	public String getLibrary();

	public String getHandlerName();

	public void setLibrary(String library) throws CoreException;

	public void setHandlerName(String handlerName) throws CoreException;
}
