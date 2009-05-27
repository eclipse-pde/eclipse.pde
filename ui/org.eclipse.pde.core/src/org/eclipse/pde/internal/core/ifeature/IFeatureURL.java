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
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;

/**
 * The container for all URL definitions of this feature.
 */
public interface IFeatureURL extends IFeatureObject {
	/**
	 * Add a URL element that should be used to
	 * discover new Eclipse features. This
	 * method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param discovery a new discovery URL element
	 */
	public void addDiscovery(IFeatureURLElement discovery) throws CoreException;

	/**
	 * Sets a URL element that should be used to
	 * update Eclipse features. This
	 * method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param update a new update URL element or null
	 */
	public void setUpdate(IFeatureURLElement update) throws CoreException;

	/**
	 * Return all URL elements that can be used
	 * to discover new Eclipse features.
	 *
	 * @return an array of URL features
	 */
	public IFeatureURLElement[] getDiscoveries();

	/**
	 * Return URL elements that can be used
	 * to update new Eclipse features.
	 *
	 * @return IFeatureURLElement or null if not set
	 */
	public IFeatureURLElement getUpdate();

	/**
	 * Remove a URL element that should be used to
	 * discover new Eclipse features. This
	 * method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param discovery a discovery URL element to remove
	 */
	public void removeDiscovery(IFeatureURLElement discovery) throws CoreException;
}
