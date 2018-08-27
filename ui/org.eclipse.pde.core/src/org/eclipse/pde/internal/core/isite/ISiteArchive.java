/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

public interface ISiteArchive extends ISiteObject {
	String P_URL = "url"; //$NON-NLS-1$
	String P_PATH = "path"; //$NON-NLS-1$

	String getURL();

	void setURL(String url) throws CoreException;

	String getPath();

	void setPath(String path) throws CoreException;
}
