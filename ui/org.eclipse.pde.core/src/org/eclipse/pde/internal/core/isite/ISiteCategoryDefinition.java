/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Red Hat Inc. - Support for nested categories
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392
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

	void addCategories(ISiteCategory[] categories) throws CoreException;

	void removeCategories(ISiteCategory[] categories) throws CoreException;

	ISiteCategory[] getCategories();
}
