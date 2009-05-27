/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

public interface ISiteCategoryDefinition extends ISiteObject {
	String P_NAME = "name"; //$NON-NLS-1$
	String P_DESCRIPTION = "description"; //$NON-NLS-1$

	String getName();

	void setName(String name) throws CoreException;

	ISiteDescription getDescription();

	void setDescription(ISiteDescription description) throws CoreException;
}
