/*******************************************************************************
 *  Copyright (c) 2003, 2012 IBM Corporation and others.
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

import java.util.Map;

public interface IBundle {

	/**
	 * Sets the value of a header in this bundle.  If the value is <code>null</code> the header will be removed from the bundle.
	 *
	 * @param key header name
	 * @param value value to set the header to, or <code>null</code> to remove a header
	 */
	void setHeader(String key, String value);

	/**
	 * Convenience method for renaming a manifest header.  Creates newKey if it does not exist.  Removes oldKey if it exists.
	 * If oldKey exists, the value will be copied to newKey.
	 *
	 * @param oldKey header to rename
	 * @param newKey new name for the header
	 */
	void renameHeader(String oldKey, String newKey);

	/**
	 * Returns the current value for the specified header or <code>null</code> if the header does not exist.
	 *
	 * @param key name of the header
	 * @return value of the header or <code>null</code>, may also be an empty string.
	 */
	String getHeader(String key);

	/**
	 * Returns a {@link IManifestHeader} object for the given header key or <code>null</code> if the header
	 * does not exist.  Will return an existing object if this model has created it before.
	 *
	 * @param key name of the header
	 * @return the manifest header object this model has for the given key or <code>null</code>
	 */
	IManifestHeader getManifestHeader(String key);

	/**
	 * Returns the bundle model that this bundle object belongs to.
	 *
	 * @return the bundle model that this bundle object belongs to
	 */
	IBundleModel getModel();

	/**
	 * Returns the value of the bundle localization header or the default location if no header is found
	 *
	 * @return localization header value or the default value
	 */
	String getLocalization();

	Map<String, IManifestHeader> getManifestHeaders();

}
