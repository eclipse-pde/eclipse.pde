/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ibundle;

public interface IBundle {

	/**
	 * Sets the value of a header in this bundle.  If the value is <code>null</code> the header will be removed from the bundle.
	 * 
	 * @param key header name
	 * @param value value to set the header to, or <code>null</code> or an <b>EMPTY STRING</b> to remove the header.
	 *   A non-zero length empty string sets an empty header.
	 */
	void setHeader(String key, String value);

	void renameHeader(String key, String newKey);

	String getHeader(String key);

	IManifestHeader getManifestHeader(String key);

	IBundleModel getModel();

	String getLocalization();

	void setLocalization(String localization);

}
