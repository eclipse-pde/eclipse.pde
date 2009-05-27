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
package org.eclipse.pde.internal.core.ibundle;

/**
 * Models that implement this interface can provide a reference
 * of the bundle plug-in model that owns the model in question.
 * This interface allows objects of these models to reach up
 * to the parent adapter without making too many assumptions
 * about the nature of the parent. Models that don't have
 * bundle plug-in model parent are required to return <code>null</code>.
 * 
 * @since 3.0
 */
public interface IBundlePluginModelProvider {
	/**
	 * Returns the parent bundle plug-in model if the provider
	 * belongs to it.
	 * @return the parent bundle plug-in model or <code>null</code>
	 * if the provider does not have a bundle plug-in model parent.
	 */
	IBundlePluginModelBase getBundlePluginModel();
}
