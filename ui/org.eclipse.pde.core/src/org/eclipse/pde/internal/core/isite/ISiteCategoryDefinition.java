/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.*;

/**
 * @author dejan
 *
 */
public interface ISiteCategoryDefinition extends ISiteObject {
	String P_NAME = "name";
	String P_DESCRIPTION = "description";
	String getName();
	void setName(String name) throws CoreException;
	ISiteDescription getDescription();
	void setDescription(ISiteDescription description) throws CoreException;
}
