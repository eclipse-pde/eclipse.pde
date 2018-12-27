/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.iproduct;

public interface IProductFeature extends IProductObject {

	String getId();

	void setId(String id);

	String getVersion();

	void setVersion(String version);

	/**
	 * @return true if the feature should be installed as a root feature.
	 */
	boolean isRootInstallMode();

	/**
	 * @param root
	 *            true if the feature must be installed as a root feature
	 * @return this
	 */
	IProductFeature setRootInstallMode(boolean root);
}
