/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IWindowImages extends IProductObject {

	public static final String P_16 = "i16"; //$NON-NLS-1$
	public static final String P_32 = "i32"; //$NON-NLS-1$
	public static final String P_48 = "i48"; //$NON-NLS-1$
	public static final String P_64 = "i64"; //$NON-NLS-1$
	public static final String P_128 = "i128"; //$NON-NLS-1$
	public static final int TOTAL_IMAGES = 5;

	String getImagePath(int size);

	void setImagePath(String path, int size);

}
