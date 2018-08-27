/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IPreferencesInfo extends IProductObject {

	public static final String P_SOURCEFILEPATH = "sourcefilepath"; //$NON-NLS-1$
	public static final String P_OVERWRITE = "overwrite"; //$NON-NLS-1$
	public static final String P_TARGETFILEPATH = "targetfilepath"; //$NON-NLS-1$

	void setSourceFilePath(String text);

	String getSourceFilePath();

	void setOverwrite(String text);

	String getOverwrite();

	void setPreferenceCustomizationPath(String text);

	String getPreferenceCustomizationPath();

}
