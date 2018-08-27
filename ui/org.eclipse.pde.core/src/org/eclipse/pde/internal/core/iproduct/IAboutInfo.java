/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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

public interface IAboutInfo extends IProductObject {

	public static final String P_IMAGE = "image"; //$NON-NLS-1$
	public static final String P_TEXT = "text"; //$NON-NLS-1$

	void setText(String text);

	String getText();

	void setImagePath(String path);

	String getImagePath();

}
