/*******************************************************************************
 *  Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 424113
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IWindowImages extends IProductObject {

	public static final String P_16 = "i16"; //$NON-NLS-1$
	public static final String P_32 = "i32"; //$NON-NLS-1$
	public static final String P_48 = "i48"; //$NON-NLS-1$
	public static final String P_64 = "i64"; //$NON-NLS-1$
	public static final String P_128 = "i128"; //$NON-NLS-1$
	public static final String P_256 = "i256"; //$NON-NLS-1$

	public static final int TOTAL_IMAGES = 6;

	String getImagePath(int size);

	void setImagePath(String path, int size);

}
