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
