/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

public interface ISiteDescription extends ISiteObject {
	String P_NAME = "name"; //$NON-NLS-1$
	String P_URL = "url"; //$NON-NLS-1$
	String P_TEXT = "text"; //$NON-NLS-1$

	String getName();

	String getURL();

	String getText();

	void setName(String name) throws CoreException;

	void setURL(String url) throws CoreException;

	void setText(String text) throws CoreException;

}
