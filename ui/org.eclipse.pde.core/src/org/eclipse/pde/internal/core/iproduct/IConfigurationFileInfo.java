/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.iproduct;

public interface IConfigurationFileInfo extends IProductObject {

	public static final String P_USE = "use"; //$NON-NLS-1$
	public static final String P_PATH = "path"; //$NON-NLS-1$
	public static final String P_OS = "os"; //$NON-NLS-1$

	void setUse(String os, String use);

	String getUse(String os);

	void setPath(String os, String path);

	String getPath(String os);

}
