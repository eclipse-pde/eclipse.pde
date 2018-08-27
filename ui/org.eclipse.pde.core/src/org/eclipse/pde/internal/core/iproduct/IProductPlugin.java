/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IProductPlugin extends IProductObject {

	String getId();

	void setId(String id);

	String getVersion();

	void setVersion(String version);

	/**
	 * @return whether this product plug-in is a fragment. <code>false</code> by default.
	 * @see #setFragment(boolean)
	 */
	boolean isFragment();

	/**
	 * Sets whether this product plug-in is a fragment. <code>false</code> by default.
	 * @param isFragment whether this product is a fragment
	 * @see #isFragment()
	 */
	void setFragment(boolean isFragment);
}
