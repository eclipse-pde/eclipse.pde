/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Copied from IFeatureBundle
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IVersionable;

public interface ISiteBundle extends IVersionable, ISiteObject {
	void addCategories(ISiteCategory[] categories) throws CoreException;

	void removeCategories(ISiteCategory[] categories) throws CoreException;

	ISiteCategory[] getCategories();
}
