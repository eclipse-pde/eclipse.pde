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
 * This factory should be used to create
 * instances of a manifest.mf header.
 */
public interface IBundleModelFactory {

	/**
	 * Creates a new manifest header
	 * @return a new manifest header instance
	 */
	IManifestHeader createHeader();

	/**
	 * Creates a new manifest header
	 *
	 * @param key the manifest header key
	 * @param value the manifest header value
	 * 
	 * @return a new manifest header instance
	 */
	IManifestHeader createHeader(String key, String value);

}
